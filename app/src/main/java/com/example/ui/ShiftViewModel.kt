package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CloudShift
import com.example.data.CloudShiftRepository
import com.example.data.ShiftEntity
import com.example.data.ShiftRepository
import com.example.governance.ClinicalCaseEntity
import com.example.governance.CloudClinicalCase
import com.example.governance.GovernanceStatus
import com.example.governance.LgpdTextRedactor
import com.example.governance.TcriaGovernanceEngine
import com.example.speech.FirebaseVoiceAiExtractor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ShiftViewModel"

enum class ShiftFilter(val label: String) {
    ALL("Todos"),
    CURRENT("Em Plantão"),
    UPCOMING("Próximos"),
    MEDICAL("Corpo Clínico"),
    ADMIN("Administrativo")
}

enum class GovernanceLayerFilter(val label: String) {
    ALL("Pipeline Completo"),
    OFFICIAL_AUDIT("1ª Camada • Válidos"),
    BLOCKED_REVIEW("2ª Camada • Bloqueados/Frágeis")
}

data class ClinicAnchorConfig(
    val clinicName: String = "Clínica Médicos a Postos • Unidade Central",
    val uf: String = "RJ",
    val specialty: String = "Clínica Geral & Emergência",
    val defaultCrm: String = "CRM-RJ 52.10482-9"
)

data class GovernanceCasesUiState(
    val clinicConfig: ClinicAnchorConfig = ClinicAnchorConfig(),
    val allCases: List<ClinicalCaseEntity> = emptyList(),
    val visibleCases: List<ClinicalCaseEntity> = emptyList(),
    val selectedLayerFilter: GovernanceLayerFilter = GovernanceLayerFilter.ALL,
    val validCount: Int = 0,
    val weakCount: Int = 0,
    val blockedCount: Int = 0,
    val invalidCount: Int = 0
)

data class ParsedVoiceShift(
    val staffName: String,
    val roleTitle: String,
    val crmOrRegistry: String,
    val department: String,
    val category: String,
    val shiftStatus: String,
    val shiftTimeLabel: String,
    val dateLabel: String,
    val locationRoom: String,
    val permissionLevel: String,
    val notes: String,
    val isOnCall: Boolean
)

sealed interface VoiceAiUiState {
    data object Idle : VoiceAiUiState
    data object Extracting : VoiceAiUiState
    data class Success(val parsed: ParsedVoiceShift, val sourceLabel: String) : VoiceAiUiState
    data class Error(val message: String) : VoiceAiUiState
}

data class ShiftSummaryUiState(
    val currentShifts: List<ShiftEntity> = emptyList(),
    val upcomingShifts: List<ShiftEntity> = emptyList(),
    val completedShifts: List<ShiftEntity> = emptyList(),
    val totalCurrentCount: Int = 0,
    val totalUpcomingCount: Int = 0,
    val medicalStaffCount: Int = 0,
    val adminStaffCount: Int = 0,
    val onCallCount: Int = 0,
    val selectedFilter: ShiftFilter = ShiftFilter.ALL,
    val searchQuery: String = "",
    val selectedShiftIds: Set<Int> = emptySet(),
    val cloudSyncError: String? = null,
    val isLoading: Boolean = true
) {
    val isMultiSelectMode: Boolean
        get() = selectedShiftIds.isNotEmpty()
}

class ShiftViewModel(
    private val repository: ShiftRepository,
    private val cloudRepository: CloudShiftRepository? = null,
    private val voiceAiExtractor: FirebaseVoiceAiExtractor = FirebaseVoiceAiExtractor()
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ShiftFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedShiftIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _cloudSyncError = MutableStateFlow<String?>(null)

    private val _clinicConfig = MutableStateFlow(ClinicAnchorConfig())
    private val _governanceLayerFilter = MutableStateFlow(GovernanceLayerFilter.ALL)

    private val _voiceAiState = MutableStateFlow<VoiceAiUiState>(VoiceAiUiState.Idle)
    val voiceAiState: StateFlow<VoiceAiUiState> = _voiceAiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        observeCloudShiftsIfConfigured()
        observeCloudCasesIfConfigured()
    }

    private fun observeCloudShiftsIfConfigured() {
        val cloudRepo = cloudRepository ?: return
        viewModelScope.launch {
            cloudRepo.observeShifts()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing cloud shifts", rootError)
                    _cloudSyncError.value = rootError.message ?: "Erro ao sincronizar plantões da nuvem."
                }
                .collect { cloudShifts ->
                    _cloudSyncError.value = null
                    repository.syncCloudShiftsToRoom(cloudShifts)
                }
        }
    }

    private fun observeCloudCasesIfConfigured() {
        val cloudRepo = cloudRepository ?: return
        viewModelScope.launch {
            cloudRepo.observeClinicalCases()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing cloud clinical cases", firestoreError ?: error)
                }
                .collect { cloudCases ->
                    repository.syncCloudCasesToRoom(cloudCases)
                }
        }
    }

    val governanceUiState: StateFlow<GovernanceCasesUiState> = combine(
        repository.allClinicalCases,
        _clinicConfig,
        _governanceLayerFilter
    ) { allCases, clinicCfg, layerFilter ->
        val visible = when (layerFilter) {
            GovernanceLayerFilter.ALL -> allCases
            GovernanceLayerFilter.OFFICIAL_AUDIT -> allCases.filter {
                it.governanceStatus == GovernanceStatus.VALID.name
            }
            GovernanceLayerFilter.BLOCKED_REVIEW -> allCases.filter {
                it.governanceStatus != GovernanceStatus.VALID.name
            }
        }
        GovernanceCasesUiState(
            clinicConfig = clinicCfg,
            allCases = allCases,
            visibleCases = visible,
            selectedLayerFilter = layerFilter,
            validCount = allCases.count { it.governanceStatus == GovernanceStatus.VALID.name },
            weakCount = allCases.count { it.governanceStatus == GovernanceStatus.WEAK.name },
            blockedCount = allCases.count { it.governanceStatus == GovernanceStatus.BLOCKED.name },
            invalidCount = allCases.count { it.governanceStatus == GovernanceStatus.INVALID.name }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = GovernanceCasesUiState()
    )


    val uiState: StateFlow<ShiftSummaryUiState> = combine(
        repository.allShifts,
        _selectedFilter,
        _searchQuery,
        _selectedShiftIds,
        _cloudSyncError
    ) { allShifts, filter, query, selectedIds, syncErr ->
        val normalizedQuery = query.trim().lowercase()
        val existingIds = allShifts.map { it.id }.toSet()
        val activeSelectedIds = selectedIds.intersect(existingIds)

        val filtered = allShifts.filter { shift ->
            val matchesFilter = when (filter) {
                ShiftFilter.ALL -> true
                ShiftFilter.CURRENT -> shift.shiftStatus == "CURRENT"
                ShiftFilter.UPCOMING -> shift.shiftStatus == "UPCOMING"
                ShiftFilter.MEDICAL -> shift.category == "MEDICAL"
                ShiftFilter.ADMIN -> shift.category == "ADMIN"
            }

            val matchesSearch = if (normalizedQuery.isEmpty()) {
                true
            } else {
                shift.staffName.lowercase().contains(normalizedQuery) ||
                    shift.roleTitle.lowercase().contains(normalizedQuery) ||
                    shift.department.lowercase().contains(normalizedQuery) ||
                    shift.crmOrRegistry.lowercase().contains(normalizedQuery) ||
                    shift.locationRoom.lowercase().contains(normalizedQuery) ||
                    shift.permissionLevel.lowercase().contains(normalizedQuery)
            }

            matchesFilter && matchesSearch
        }

        ShiftSummaryUiState(
            currentShifts = filtered.filter { it.shiftStatus == "CURRENT" },
            upcomingShifts = filtered.filter { it.shiftStatus == "UPCOMING" },
            completedShifts = filtered.filter { it.shiftStatus == "COMPLETED" },
            totalCurrentCount = allShifts.count { it.shiftStatus == "CURRENT" },
            totalUpcomingCount = allShifts.count { it.shiftStatus == "UPCOMING" },
            medicalStaffCount = allShifts.count { it.category == "MEDICAL" && it.shiftStatus != "COMPLETED" },
            adminStaffCount = allShifts.count { it.category == "ADMIN" && it.shiftStatus != "COMPLETED" },
            onCallCount = allShifts.count { it.isOnCall && it.shiftStatus != "COMPLETED" },
            selectedFilter = filter,
            searchQuery = query,
            selectedShiftIds = activeSelectedIds,
            cloudSyncError = syncErr,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ShiftSummaryUiState()
    )

    fun setFilter(filter: ShiftFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShiftSelection(shiftId: Int) {
        _selectedShiftIds.update { current ->
            if (shiftId in current) current - shiftId else current + shiftId
        }
    }

    fun selectAllVisibleShifts() {
        val state = uiState.value
        val visibleIds = (state.currentShifts + state.upcomingShifts + state.completedShifts)
            .map { it.id }
            .toSet()
        _selectedShiftIds.value = visibleIds
    }

    fun clearSelection() {
        _selectedShiftIds.value = emptySet()
    }

    fun deleteSelectedShifts(onDeletedCount: (Int) -> Unit = {}) {
        val state = uiState.value
        val selectedIds = state.selectedShiftIds
        if (selectedIds.isEmpty()) return

        val allVisibleAndKnown = state.currentShifts + state.upcomingShifts + state.completedShifts
        val selectedShifts = allVisibleAndKnown.filter { it.id in selectedIds }
        val roomIdsToDelete = selectedIds.toList()
        val firebaseIdsToDelete = selectedShifts.mapNotNull {
            it.firebaseDocId.takeIf { docId -> docId.isNotBlank() }
        }

        _selectedShiftIds.value = emptySet()
        onDeletedCount(roomIdsToDelete.size)

        viewModelScope.launch {
            repository.deleteByIds(roomIdsToDelete)
            if (firebaseIdsToDelete.isNotEmpty() && cloudRepository != null) {
                try {
                    cloudRepository.deleteShifts(firebaseIdsToDelete)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bulk-delete shifts from Firestore", e)
                }
            }
        }
    }

    fun resetVoiceAiState() {
        _voiceAiState.value = VoiceAiUiState.Idle
    }

    fun extractVoiceCommandWithFirebaseAi(transcript: String) {
        if (transcript.isBlank()) {
            _voiceAiState.value = VoiceAiUiState.Error("Diga ou selecione um comando de voz antes de extrair.")
            return
        }
        viewModelScope.launch {
            _voiceAiState.value = VoiceAiUiState.Extracting
            val sanitizedTranscript = LgpdTextRedactor.redact(transcript)
            val result = voiceAiExtractor.extractShiftFromSpeech(sanitizedTranscript)
            result.fold(
                onSuccess = { parsed ->
                    _voiceAiState.value = VoiceAiUiState.Success(
                        parsed = LgpdTextRedactor.sanitizeParsedVoiceShift(parsed),
                        sourceLabel = "Extraído via Firebase AI Logic (Gemini) • Saneado LGPD"
                    )
                },
                onFailure = { error ->
                    _voiceAiState.value = VoiceAiUiState.Error(
                        error.localizedMessage ?: "Falha ao processar comando de voz com Firebase AI."
                    )
                }
            )
        }
    }

    fun addShift(
        staffName: String,
        roleTitle: String,
        crmOrRegistry: String,
        department: String,
        category: String,
        shiftStatus: String,
        shiftTimeLabel: String,
        dateLabel: String,
        locationRoom: String,
        permissionLevel: String,
        notes: String,
        isOnCall: Boolean,
        createdViaVoice: Boolean,
        voiceTranscript: String = ""
    ) {
        viewModelScope.launch {
            val priority = when (shiftStatus) {
                "CURRENT" -> 1
                "UPCOMING" -> 5
                else -> 10
            }

            val resolvedName = LgpdTextRedactor.redact(staffName.ifBlank { "Profissional Plantonista" })
            val resolvedRole = LgpdTextRedactor.redact(roleTitle.ifBlank { "Médico Plantonista" })
            val resolvedCrm = LgpdTextRedactor.redact(crmOrRegistry.ifBlank { "CRM-SP 204.510" })
            val resolvedDept = LgpdTextRedactor.redact(department.ifBlank { "Clínica Geral" })
            val resolvedTime = shiftTimeLabel.ifBlank { "07:00 – 19:00 (12h)" }
            val resolvedDate = dateLabel.ifBlank {
                if (shiftStatus == "CURRENT") "Hoje • Em Plantão" else "Próximo Turno"
            }
            val resolvedRoom = LgpdTextRedactor.redact(locationRoom.ifBlank { "Ala Central • Consultório 02" })
            val resolvedPermission = permissionLevel.ifBlank {
                if (category == "ADMIN") "Gestão de Escalas & Permissões" else "Acesso Clínico Plantonista"
            }
            val resolvedNotes = LgpdTextRedactor.redact(notes.ifBlank { "Plantão registrado no controle clínico." })
            val resolvedTranscript = LgpdTextRedactor.redact(voiceTranscript)

            var firebaseDocId = ""
            if (cloudRepository != null) {
                try {
                    firebaseDocId = cloudRepository.createShift(
                        CloudShift(
                            staffName = resolvedName,
                            roleTitle = resolvedRole,
                            crmOrRegistry = resolvedCrm,
                            department = resolvedDept,
                            category = category,
                            shiftStatus = shiftStatus,
                            shiftTimeLabel = resolvedTime,
                            dateLabel = resolvedDate,
                            locationRoom = resolvedRoom,
                            permissionLevel = resolvedPermission,
                            notes = resolvedNotes,
                            voiceTranscript = resolvedTranscript,
                            isOnCall = isOnCall,
                            createdViaVoice = createdViaVoice
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync new shift to Firebase Firestore", e)
                    _cloudSyncError.value = e.localizedMessage ?: "Erro ao salvar plantão no Firebase."
                }
            }

            val newShift = ShiftEntity(
                firebaseDocId = firebaseDocId,
                staffName = resolvedName,
                roleTitle = resolvedRole,
                crmOrRegistry = resolvedCrm,
                department = resolvedDept,
                category = category,
                shiftStatus = shiftStatus,
                shiftTimeLabel = resolvedTime,
                dateLabel = resolvedDate,
                locationRoom = resolvedRoom,
                permissionLevel = resolvedPermission,
                notes = resolvedNotes,
                voiceTranscript = resolvedTranscript,
                progressFraction = if (shiftStatus == "CURRENT") 0.25f else 0.0f,
                isOnCall = isOnCall,
                createdViaVoice = createdViaVoice,
                sortPriority = priority
            )
            repository.insert(newShift)
        }
    }

    fun startShift(shift: ShiftEntity) {
        viewModelScope.launch {
            val updatedDateLabel = "Hoje • Em Plantão"
            repository.update(
                shift.copy(
                    shiftStatus = "CURRENT",
                    dateLabel = updatedDateLabel,
                    progressFraction = 0.15f,
                    sortPriority = 1
                )
            )
            if (shift.firebaseDocId.isNotBlank() && cloudRepository != null) {
                try {
                    cloudRepository.updateShiftStatus(shift.firebaseDocId, "CURRENT", updatedDateLabel)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update shift status in Firestore", e)
                }
            }
        }
    }

    fun completeShift(shift: ShiftEntity) {
        viewModelScope.launch {
            val updatedDateLabel = "Concluído • Hoje"
            repository.update(
                shift.copy(
                    shiftStatus = "COMPLETED",
                    dateLabel = updatedDateLabel,
                    progressFraction = 1.0f,
                    sortPriority = 10
                )
            )
            if (shift.firebaseDocId.isNotBlank() && cloudRepository != null) {
                try {
                    cloudRepository.updateShiftStatus(shift.firebaseDocId, "COMPLETED", updatedDateLabel)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to complete shift in Firestore", e)
                }
            }
        }
    }

    fun deleteShift(shift: ShiftEntity) {
        _selectedShiftIds.update { it - shift.id }
        viewModelScope.launch {
            repository.deleteById(shift.id)
            if (shift.firebaseDocId.isNotBlank() && cloudRepository != null) {
                try {
                    cloudRepository.deleteShift(shift.firebaseDocId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete shift from Firestore", e)
                }
            }
        }
    }

    fun setGovernanceLayerFilter(filter: GovernanceLayerFilter) {
        _governanceLayerFilter.value = filter
    }

    fun updateClinicAnchorConfig(
        clinicName: String,
        uf: String,
        specialty: String,
        defaultCrm: String
    ) {
        _clinicConfig.value = ClinicAnchorConfig(
            clinicName = clinicName.ifBlank { "Clínica Médicos a Postos • Unidade Central" },
            uf = uf.ifBlank { "RJ" }.uppercase(),
            specialty = specialty.ifBlank { "Clínica Geral" },
            defaultCrm = defaultCrm.ifBlank { "CRM-RJ 52.10482-9" }
        )
    }

    fun addClinicalCase(
        projectClinicAnchor: String,
        clinicUf: String,
        clinicSpecialty: String,
        caseReference: String,
        responsibleHuman: String,
        declaredPurpose: String,
        contextScope: String,
        evidencias: String,
        raciocinio: String,
        decisao: String,
        deidentificationConfirmed: Boolean,
        createdViaVoice: Boolean = false
    ) {
        viewModelScope.launch {
            val cleanReference = LgpdTextRedactor.redact(caseReference)
            val cleanResponsible = LgpdTextRedactor.redact(responsibleHuman)
            val cleanPurpose = LgpdTextRedactor.redact(declaredPurpose)
            val cleanContext = LgpdTextRedactor.redact(contextScope)
            val cleanEvidencias = LgpdTextRedactor.redact(evidencias)
            val cleanRaciocinio = LgpdTextRedactor.redact(raciocinio)
            val cleanDecisao = LgpdTextRedactor.redact(decisao)

            val eval = TcriaGovernanceEngine.evaluateCase(
                projectClinicAnchor = projectClinicAnchor,
                caseReference = cleanReference,
                responsibleHuman = cleanResponsible,
                declaredPurpose = cleanPurpose,
                contextScope = cleanContext,
                evidencias = cleanEvidencias,
                raciocinio = cleanRaciocinio,
                decisao = cleanDecisao,
                deidentificationConfirmed = deidentificationConfirmed
            )

            var firebaseDocId = ""
            if (cloudRepository != null) {
                try {
                    firebaseDocId = cloudRepository.createClinicalCase(
                        CloudClinicalCase(
                            projectClinicAnchor = projectClinicAnchor,
                            clinicUf = clinicUf,
                            clinicSpecialty = clinicSpecialty,
                            caseReference = cleanReference,
                            responsibleHuman = cleanResponsible,
                            declaredPurpose = cleanPurpose,
                            contextScope = cleanContext,
                            evidencias = cleanEvidencias,
                            raciocinio = cleanRaciocinio,
                            decisao = cleanDecisao,
                            deidentificationConfirmed = deidentificationConfirmed,
                            governanceStatus = eval.overallStatus.name,
                            governanceReport = eval.auditSummary,
                            createdViaVoice = createdViaVoice
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync clinical case to Firestore", e)
                }
            }

            repository.insertClinicalCase(
                ClinicalCaseEntity(
                    firebaseDocId = firebaseDocId,
                    projectClinicAnchor = projectClinicAnchor,
                    clinicUf = clinicUf,
                    clinicSpecialty = clinicSpecialty,
                    caseReference = cleanReference,
                    responsibleHuman = cleanResponsible,
                    declaredPurpose = cleanPurpose,
                    contextScope = cleanContext,
                    evidencias = cleanEvidencias,
                    raciocinio = cleanRaciocinio,
                    decisao = cleanDecisao,
                    deidentificationConfirmed = deidentificationConfirmed,
                    governanceStatus = eval.overallStatus.name,
                    governanceReport = eval.auditSummary,
                    createdViaVoice = createdViaVoice
                )
            )
        }
    }

    fun deleteClinicalCase(clinicalCase: ClinicalCaseEntity) {
        viewModelScope.launch {
            repository.deleteClinicalCaseById(clinicalCase.id)
            if (clinicalCase.firebaseDocId.isNotBlank() && cloudRepository != null) {
                try {
                    cloudRepository.deleteClinicalCase(clinicalCase.firebaseDocId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete clinical case from Firestore", e)
                }
            }
        }
    }


    companion object {
        fun parseVoiceTranscript(transcript: String): ParsedVoiceShift {
            val clean = LgpdTextRedactor.redact(transcript.trim())
            val lower = clean.lowercase()

            val isAdmin = lower.contains("admin") ||
                lower.contains("recepção") ||
                lower.contains("faturamento") ||
                lower.contains("coordena") ||
                lower.contains("gestor")

            val department = when {
                lower.contains("uti") -> "UTI Adulto"
                lower.contains("emergência") || lower.contains("pronto") || lower.contains("trauma") -> "Pronto-Socorro / Trauma"
                lower.contains("cirúrgic") || lower.contains("cirurgia") -> "Centro Cirúrgico"
                lower.contains("pediatria") || lower.contains("neonat") -> "Pediatria & UTI Neonatal"
                lower.contains("cardio") -> "Unidade Coronariana"
                isAdmin -> "Administração & Regulação"
                else -> "Clínica Médica Geral"
            }

            val isUpcoming = lower.contains("amanhã") ||
                lower.contains("próximo") ||
                lower.contains("noturno") ||
                lower.contains("19h") ||
                lower.contains("noite")

            val isOnCall = lower.contains("sobreaviso") || lower.contains("retaguarda")

            val roleTitle = when {
                lower.contains("intensivista") -> "Médico(a) Intensivista"
                lower.contains("cirurgi") -> "Cirurgião(ã) Plantonista"
                lower.contains("pediatra") -> "Médico(a) Pediatra"
                lower.contains("cardio") -> "Cardiologista de Plantão"
                lower.contains("anestesi") -> "Anestesiologista"
                isAdmin -> "Coordenador(a) Administrativo(a)"
                else -> "Médico(a) Plantonista"
            }

            val nameRegex = Regex(
                "(?:doutora|doutor|dra\\.?|dr\\.?|enfermeira|enfermeiro|coordenadora|coordenador|gestora|gestor)\\s+([a-zA-ZÀ-ÿ]+(?:\\s+[a-zA-ZÀ-ÿ]+)?)",
                RegexOption.IGNORE_CASE
            )
            val matchedName = nameRegex.find(clean)?.let { match ->
                val rawPrefix = match.value.substringBefore(" ").lowercase()
                val formattedPrefix = when {
                    rawPrefix.startsWith("doutora") || rawPrefix.startsWith("dra") -> "Dra."
                    rawPrefix.startsWith("doutor") || rawPrefix.startsWith("dr") -> "Dr."
                    else -> ""
                }
                val personName = match.groupValues[1].split(" ")
                    .filter { word ->
                        val w = word.lowercase()
                        w !in listOf("na", "no", "em", "para", "plantão", "turno", "uti", "hoje", "amanhã", "cpf")
                    }
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { it.uppercase() }
                    }
                if (formattedPrefix.isNotEmpty()) "$formattedPrefix $personName" else personName
            } ?: "Dr. Eduardo Santoro"

            val timeLabel = if (lower.contains("noite") || lower.contains("noturno") || lower.contains("19")) {
                "19:00 – 07:00 (12h)"
            } else {
                "07:00 – 19:00 (12h)"
            }

            return LgpdTextRedactor.sanitizeParsedVoiceShift(
                ParsedVoiceShift(
                    staffName = matchedName,
                    roleTitle = roleTitle,
                    crmOrRegistry = if (isAdmin) "ADM-CL 0941" else "CRM-SP 186.420",
                    department = department,
                    category = if (isAdmin) "ADMIN" else "MEDICAL",
                    shiftStatus = if (isUpcoming) "UPCOMING" else "CURRENT",
                    shiftTimeLabel = timeLabel,
                    dateLabel = if (isUpcoming) "Hoje à Noite • Agendado por Voz" else "Hoje • Em Plantão",
                    locationRoom = "$department • Setor Principal",
                    permissionLevel = if (isAdmin) "Gestão de Escalas & Permissões" else "Acesso Clínico Plantonista",
                    notes = "Comando de voz: \"$clean\"",
                    isOnCall = isOnCall
                )
            )
        }
    }
}

class ShiftViewModelFactory(
    private val repository: ShiftRepository,
    private val cloudRepository: CloudShiftRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShiftViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShiftViewModel(repository, cloudRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
