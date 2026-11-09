package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.governance.LgpdTextRedactor
import com.example.speech.SpeechRecognitionState
import com.example.speech.SpeechToTextManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShiftBottomSheet(
    voiceAiState: VoiceAiUiState,
    onExtractWithFirebaseAi: (String) -> Unit,
    onResetVoiceAiState: () -> Unit,
    onDismiss: () -> Unit,
    onSaveShift: (
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
        voiceTranscript: String
    ) -> Unit
) {
    BackHandler {
        onResetVoiceAiState()
        onDismiss()
    }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var voiceTranscript by remember { mutableStateOf("") }
    var staffName by remember { mutableStateOf("") }
    var roleTitle by remember { mutableStateOf("") }
    var crmOrRegistry by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("UTI Adulto") }
    var category by remember { mutableStateOf("MEDICAL") }
    var shiftStatus by remember { mutableStateOf("CURRENT") }
    var shiftTimeLabel by remember { mutableStateOf("07:00 – 19:00 (12h)") }
    var dateLabel by remember { mutableStateOf("Hoje • Em Plantão") }
    var locationRoom by remember { mutableStateOf("Bloco A • Leitos 01 a 12") }
    var permissionLevel by remember { mutableStateOf("Acesso Clínico Plantonista") }
    var notes by remember { mutableStateOf("") }
    var isOnCall by remember { mutableStateOf(false) }
    var createdViaVoice by remember { mutableStateOf(false) }

    val applyParsedShift: (ParsedVoiceShift, String) -> Unit = { parsed, rawTranscript ->
        staffName = parsed.staffName
        roleTitle = parsed.roleTitle
        crmOrRegistry = parsed.crmOrRegistry
        department = parsed.department
        category = parsed.category
        shiftStatus = parsed.shiftStatus
        shiftTimeLabel = parsed.shiftTimeLabel
        dateLabel = parsed.dateLabel
        locationRoom = parsed.locationRoom
        permissionLevel = parsed.permissionLevel
        notes = parsed.notes
        isOnCall = parsed.isOnCall
        createdViaVoice = true
        if (rawTranscript.isNotBlank()) {
            voiceTranscript = rawTranscript
        }
    }

    // Apply structured output when Firebase AI Logic extraction succeeds
    LaunchedEffect(voiceAiState) {
        if (voiceAiState is VoiceAiUiState.Success) {
            applyParsedShift(voiceAiState.parsed, voiceTranscript)
        }
    }

    val speechToTextManager = remember(context) { SpeechToTextManager(context) }
    val recognitionState by speechToTextManager.recognitionState.collectAsStateWithLifecycle()

    DisposableEffect(speechToTextManager) {
        onDispose {
            speechToTextManager.stopListening()
        }
    }

    // Automatically populate transcript & local preview when SpeechRecognizer emits results
    LaunchedEffect(recognitionState) {
        when (val state = recognitionState) {
            is SpeechRecognitionState.PartialResult -> {
                voiceTranscript = state.partialText
            }
            is SpeechRecognitionState.FinalResult -> {
                voiceTranscript = state.transcript
                val localParsed = ShiftViewModel.parseVoiceTranscript(state.transcript)
                applyParsedShift(localParsed, state.transcript)
                onExtractWithFirebaseAi(state.transcript)
            }
            else -> Unit
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechToTextManager.startListening()
        }
    }

    val isListening = recognitionState is SpeechRecognitionState.Listening ||
        recognitionState is SpeechRecognitionState.PartialResult

    val statusSubtitle = when (val s = recognitionState) {
        is SpeechRecognitionState.Idle ->
            "Toque no microfone para Speech-to-Text ou escolha um comando de voz abaixo."
        is SpeechRecognitionState.Listening ->
            "Ouvindo microfone… Fale nome, especialidade, setor e horário."
        is SpeechRecognitionState.PartialResult ->
            "Captando: \"${s.partialText}\""
        is SpeechRecognitionState.FinalResult ->
            "Reconhecido: \"${s.transcript}\""
        is SpeechRecognitionState.Error ->
            s.message
    }

    ModalBottomSheet(
        onDismissRequest = {
            onResetVoiceAiState()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.modal_add_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Speech-to-Text + Firebase AI Logic & Sincronização Firestore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        onResetVoiceAiState()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("close_add_shift_modal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar modal"
                    )
                }
            }

            // Speech-to-Text & Firebase AI Logic Assistant Card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.secondary
                                )
                                .clickable {
                                    if (isListening) {
                                        speechToTextManager.stopListening()
                                    } else {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            speechToTextManager.startListening()
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("voice_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Ativar Speech-to-Text",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isListening) "Speech-to-Text Ativo…" else "Comando de Voz (Speech-to-Text)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = statusSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = voiceTranscript,
                        onValueChange = {
                            voiceTranscript = it
                            if (it.isNotBlank()) {
                                createdViaVoice = true
                            }
                        },
                        label = { Text("Transcrição de Voz (Speech-to-Text)") },
                        placeholder = { Text("Ex: Dra. Mariana Costa intensivista na UTI Adulto plantão hoje") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_voice_transcript")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val localParsed = ShiftViewModel.parseVoiceTranscript(voiceTranscript)
                                applyParsedShift(localParsed, voiceTranscript)
                                onExtractWithFirebaseAi(voiceTranscript)
                            },
                            enabled = voiceTranscript.isNotBlank() && voiceAiState !is VoiceAiUiState.Extracting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_parse_voice_button")
                        ) {
                            if (voiceAiState is VoiceAiUiState.Extracting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extraindo com Gemini…")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extrair com Firebase AI")
                            }
                        }
                    }

                    // Show Firebase AI Logic status or error transparently
                    when (voiceAiState) {
                        is VoiceAiUiState.Success -> {
                            Text(
                                text = "✓ ${voiceAiState.sourceLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        is VoiceAiUiState.Error -> {
                            Text(
                                text = "Aviso IA: ${voiceAiState.message} (Dados preenchidos pelo parser local)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        else -> Unit
                    }

                    // Instant Voice Preset Chips (including LGPD PII auto-redaction test phrase)
                    val voicePresets = listOf(
                        "Dra. Mariana Costa intensivista na UTI Adulto plantão hoje",
                        "Dr. Bruno Silveira na UTI paciente CPF 123.456.789-00 contato paciente@hospital.org",
                        "Coordenador Roberto Dias na Administração plantão diurno"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        voicePresets.forEachIndexed { index, phrase ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        val sanitizedPhrase = LgpdTextRedactor.redact(phrase)
                                        voiceTranscript = sanitizedPhrase
                                        val localParsed = ShiftViewModel.parseVoiceTranscript(sanitizedPhrase)
                                        applyParsedShift(localParsed, sanitizedPhrase)
                                        onExtractWithFirebaseAi(sanitizedPhrase)
                                    }
                                    .testTag("voice_preset_$index")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "\"${phrase.take(36)}…\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live LGPD PII Redaction Inspection Banner
            val liveLgpdReport = remember(
                voiceTranscript,
                staffName,
                roleTitle,
                crmOrRegistry,
                department,
                locationRoom,
                notes
            ) {
                LgpdTextRedactor.inspectFields(
                    voiceTranscript,
                    staffName,
                    roleTitle,
                    crmOrRegistry,
                    department,
                    locationRoom,
                    notes
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (liveLgpdReport.wasRedacted) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lgpd_redaction_status_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (liveLgpdReport.wasRedacted) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (liveLgpdReport.wasRedacted) {
                                    "PII Detectado (${liveLgpdReport.detectedTypes.joinToString { it.label }})"
                                } else {
                                    "Filtro Automático LGPD Ativo"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (liveLgpdReport.wasRedacted) {
                                    "${liveLgpdReport.redactedCount} dado(s) sensível(is) serão redigidos automaticamente antes de salvar no Room."
                                } else {
                                    "CPFs, e-mails, telefones, RG e CNS em voz ou texto são redigidos antes de salvar no Room."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (liveLgpdReport.wasRedacted) {
                        TextButton(
                            onClick = {
                                voiceTranscript = LgpdTextRedactor.redact(voiceTranscript)
                                staffName = LgpdTextRedactor.redact(staffName)
                                roleTitle = LgpdTextRedactor.redact(roleTitle)
                                crmOrRegistry = LgpdTextRedactor.redact(crmOrRegistry)
                                department = LgpdTextRedactor.redact(department)
                                locationRoom = LgpdTextRedactor.redact(locationRoom)
                                notes = LgpdTextRedactor.redact(notes)
                            },
                            modifier = Modifier.testTag("btn_redact_pii_now")
                        ) {
                            Text(
                                text = "Redigir Agora",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Category & Shift Status Toggles
            Text(
                text = "Área e Status do Plantão",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = category == "MEDICAL",
                    onClick = {
                        category = "MEDICAL"
                        permissionLevel = "Acesso Clínico Plantonista"
                    },
                    label = { Text("Corpo Clínico") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("chip_category_medical")
                )
                FilterChip(
                    selected = category == "ADMIN",
                    onClick = {
                        category = "ADMIN"
                        permissionLevel = "Gestão de Escalas & Permissões"
                    },
                    label = { Text("Área Administrativa") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("chip_category_admin")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = shiftStatus == "CURRENT",
                    onClick = {
                        shiftStatus = "CURRENT"
                        dateLabel = "Hoje • Em Plantão"
                    },
                    label = { Text("Em Plantão Agora") },
                    leadingIcon = if (shiftStatus == "CURRENT") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("chip_status_current")
                )
                FilterChip(
                    selected = shiftStatus == "UPCOMING",
                    onClick = {
                        shiftStatus = "UPCOMING"
                        dateLabel = "Próximo Turno • Escalonado"
                    },
                    label = { Text("Próximo Plantão") },
                    leadingIcon = if (shiftStatus == "UPCOMING") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("chip_status_upcoming")
                )
            }

            // Form Inputs
            OutlinedTextField(
                value = staffName,
                onValueChange = { staffName = it },
                label = { Text("Nome do Profissional (Médico / Gestor)") },
                placeholder = { Text("Ex: Dra. Fernanda Lima") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_staff_name")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it },
                    label = { Text("Cargo / Especialidade") },
                    placeholder = { Text("Intensivista") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_role_title")
                )
                OutlinedTextField(
                    value = crmOrRegistry,
                    onValueChange = { crmOrRegistry = it },
                    label = { Text("CRM / Registro") },
                    placeholder = { Text("CRM-SP 192.410") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("input_crm_registry")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Setor / Unidade") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_department")
                )
                OutlinedTextField(
                    value = shiftTimeLabel,
                    onValueChange = { shiftTimeLabel = it },
                    label = { Text("Horário do Plantão") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_shift_time")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = locationRoom,
                    onValueChange = { locationRoom = it },
                    label = { Text("Ala / Sala / Leitos") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_location_room")
                )
                OutlinedTextField(
                    value = permissionLevel,
                    onValueChange = { permissionLevel = it },
                    label = { Text("Nível de Permissão") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_permission_level")
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações de Passagem de Plantão") },
                placeholder = { Text("Resumo clínico ou demandas administrativas do turno…") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_shift_notes")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Regime de Sobreaviso / Retaguarda",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Destaca o profissional para acionamento imediato",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isOnCall,
                    onCheckedChange = { isOnCall = it },
                    modifier = Modifier.testTag("switch_on_call")
                )
            }

            AnimatedVisibility(visible = createdViaVoice) {
                Text(
                    text = "✓ Plantão configurado via Speech-to-Text (será salvo no Room + Firebase Firestore)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onResetVoiceAiState()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_shift_button")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
                Button(
                    onClick = {
                        onSaveShift(
                            LgpdTextRedactor.redact(staffName),
                            LgpdTextRedactor.redact(roleTitle),
                            LgpdTextRedactor.redact(crmOrRegistry),
                            LgpdTextRedactor.redact(department),
                            category,
                            shiftStatus,
                            shiftTimeLabel,
                            dateLabel,
                            LgpdTextRedactor.redact(locationRoom),
                            permissionLevel,
                            LgpdTextRedactor.redact(notes),
                            isOnCall,
                            createdViaVoice,
                            LgpdTextRedactor.redact(voiceTranscript)
                        )
                        onResetVoiceAiState()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_shift_button")
                ) {
                    Text("Salvar (Room & Firebase)")
                }
            }
        }
    }
}

