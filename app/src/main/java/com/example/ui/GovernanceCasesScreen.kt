package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.governance.ClinicalCaseEntity
import com.example.governance.GateCheckResult
import com.example.governance.GovernanceStatus
import com.example.governance.LgpdTextRedactor
import com.example.governance.TcriaGovernanceEngine
import com.example.ui.theme.JetBrainsMonoFamily

private val EmeraldPrimary = Color(0xFF087F67)
private val EmeraldDeep = Color(0xFF066350)
private val ForestInk = Color(0xFF14302A)
private val AmberWarn = Color(0xFFD97706)
private val CrimsonBlock = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GovernanceCasesScreen(
    viewModel: ShiftViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.governanceUiState.collectAsStateWithLifecycle()
    var showAddCaseModal by rememberSaveable { mutableStateOf(false) }
    var showAnchorConfigDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("governance_cases_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCaseModal = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_clinical_case"),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Novo Caso Clínico"
                    )
                },
                text = {
                    Text(
                        text = "Registrar Decisão TCRIA",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("governance_cases_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GovernanceHeaderBanner(
                    clinicConfig = state.clinicConfig,
                    validCount = state.validCount,
                    weakCount = state.weakCount,
                    blockedCount = state.blockedCount + state.invalidCount,
                    onEditAnchor = { showAnchorConfigDialog = true }
                )
            }

            item {
                TcriaRulesetPillarsCard()
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FILTRO DE AUDITORIA EM 2 CAMADAS (PIPELINE TCRIA)",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GovernanceLayerFilter.entries.forEach { filter ->
                            val selected = state.selectedLayerFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setGovernanceLayerFilter(filter) },
                                label = {
                                    Text(
                                        text = filter.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (filter) {
                                            GovernanceLayerFilter.ALL -> Icons.AutoMirrored.Filled.Rule
                                            GovernanceLayerFilter.OFFICIAL_AUDIT -> Icons.Default.GppGood
                                            GovernanceLayerFilter.BLOCKED_REVIEW -> Icons.Default.GppMaybe
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.16f),
                                    selectedLabelColor = EmeraldDeep,
                                    selectedLeadingIconColor = EmeraldDeep
                                ),
                                modifier = Modifier.testTag("layer_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            items(
                items = state.visibleCases,
                key = { it.id }
            ) { clinicalCase ->
                ClinicalCaseGovernanceCard(
                    clinicalCase = clinicalCase,
                    onDelete = { viewModel.deleteClinicalCase(clinicalCase) }
                )
            }
        }
    }

    if (showAnchorConfigDialog) {
        ClinicAnchorDialog(
            initialConfig = state.clinicConfig,
            onDismiss = { showAnchorConfigDialog = false },
            onSave = { name, uf, specialty, crm ->
                viewModel.updateClinicAnchorConfig(name, uf, specialty, crm)
                showAnchorConfigDialog = false
            }
        )
    }

    if (showAddCaseModal) {
        AddClinicalCaseBottomSheet(
            clinicConfig = state.clinicConfig,
            onDismiss = { showAddCaseModal = false },
            onSaveCase = { anchor, uf, spec, ref, human, purpose, ctx, evid, rac, dec, deid ->
                viewModel.addClinicalCase(
                    projectClinicAnchor = anchor,
                    clinicUf = uf,
                    clinicSpecialty = spec,
                    caseReference = ref,
                    responsibleHuman = human,
                    declaredPurpose = purpose,
                    contextScope = ctx,
                    evidencias = evid,
                    raciocinio = rac,
                    decisao = dec,
                    deidentificationConfirmed = deid
                )
                showAddCaseModal = false
            }
        )
    }
}

@Composable
private fun GovernanceHeaderBanner(
    clinicConfig: ClinicAnchorConfig,
    validCount: Int,
    weakCount: Int,
    blockedCount: Int,
    onEditAnchor: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ForestInk),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(ForestInk, EmeraldDeep)
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "GOVERNANÇA TCRIA • MÉDICOS A POSTOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = Color(0xFFD1FAE5),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEditAnchor,
                    modifier = Modifier.testTag("btn_edit_clinic_anchor")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Configurar Âncora da Clínica",
                        tint = Color.White
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = clinicConfig.clinicName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "RULE_1.1 Âncora Ativa • UF ${clinicConfig.uf} • ${clinicConfig.specialty} • ${clinicConfig.defaultCrm}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA7F3D0)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuditMetricTile(
                    label = "1ª Camada (Válidos)",
                    value = validCount.toString(),
                    accentColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                AuditMetricTile(
                    label = "Frágeis (WEAK)",
                    value = weakCount.toString(),
                    accentColor = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                AuditMetricTile(
                    label = "2ª Camada (Retidos)",
                    value = blockedCount.toString(),
                    accentColor = Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AuditMetricTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = JetBrainsMonoFamily,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TcriaRulesetPillarsCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Regras Estruturais TCRIA & Guarda Documental LGPD",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Todo registro passa por 5 gates: (1) RULE_1.1 Project/Clinic Anchor, (2) RULE_1.2 Contexto & Escopo, (3) RULE_2.1/5.1 Cadeia Bidirecional Evidência → Raciocínio → Conduta, (4) RULE_4.1 Responsável Humano (CRM) e (5) Gate Prescritivo & Desidentificação LGPD.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClinicalCaseGovernanceCard(
    clinicalCase: ClinicalCaseEntity,
    onDelete: () -> Unit
) {
    var expandedGates by rememberSaveable { mutableStateOf(false) }

    val evaluation = remember(clinicalCase) {
        TcriaGovernanceEngine.evaluateCase(
            projectClinicAnchor = clinicalCase.projectClinicAnchor,
            caseReference = clinicalCase.caseReference,
            responsibleHuman = clinicalCase.responsibleHuman,
            declaredPurpose = clinicalCase.declaredPurpose,
            contextScope = clinicalCase.contextScope,
            evidencias = clinicalCase.evidencias,
            raciocinio = clinicalCase.raciocinio,
            decisao = clinicalCase.decisao,
            deidentificationConfirmed = clinicalCase.deidentificationConfirmed
        )
    }

    val statusColor = when (evaluation.overallStatus) {
        GovernanceStatus.VALID -> EmeraldPrimary
        GovernanceStatus.WEAK -> AmberWarn
        GovernanceStatus.BLOCKED, GovernanceStatus.INVALID -> CrimsonBlock
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("clinical_case_card_${clinicalCase.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (evaluation.overallStatus) {
                                GovernanceStatus.VALID -> Icons.Default.CheckCircle
                                GovernanceStatus.WEAK -> Icons.Default.WarningAmber
                                else -> Icons.Default.ErrorOutline
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = evaluation.overallStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir Caso Clínico",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = clinicalCase.caseReference,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "${clinicalCase.responsibleHuman} • ${clinicalCase.clinicSpecialty} (${clinicalCase.clinicUf})",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Contexto (RULE_1.2): ${clinicalCase.contextScope}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TraceFieldRow(
                        tag = "EVIDÊNCIA (2.1)",
                        value = clinicalCase.evidencias.ifBlank { "[Ausente — Afirmação sem evidência vinculada]" }
                    )
                    TraceFieldRow(
                        tag = "RACIOCÍNIO (2.2)",
                        value = clinicalCase.raciocinio
                    )
                    TraceFieldRow(
                        tag = "DECISÃO / CONDUTA (3.2)",
                        value = clinicalCase.decisao
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expandedGates = !expandedGates }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Ver Relatório dos 5 Gates TCRIA (${evaluation.gates.count { it.passed }}/5 aprovados)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expandedGates) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
            }

            AnimatedVisibility(visible = expandedGates) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    evaluation.gates.forEach { gate ->
                        GateCheckItemRow(gate)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceFieldRow(tag: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            color = EmeraldDeep,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GateCheckItemRow(gate: GateCheckResult) {
    val tint = when (gate.status) {
        GovernanceStatus.VALID -> EmeraldPrimary
        GovernanceStatus.WEAK -> AmberWarn
        else -> CrimsonBlock
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (gate.passed) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${gate.ruleCode} • ${gate.gateName}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = gate.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ClinicAnchorDialog(
    initialConfig: ClinicAnchorConfig,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var clinicName by rememberSaveable { mutableStateOf(initialConfig.clinicName) }
    var uf by rememberSaveable { mutableStateOf(initialConfig.uf) }
    var specialty by rememberSaveable { mutableStateOf(initialConfig.specialty) }
    var crm by rememberSaveable { mutableStateOf(initialConfig.defaultCrm) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configurar Âncora da Clínica (RULE_1.1)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = { Text("Nome da Clínica / Projeto Âncora") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uf,
                        onValueChange = { uf = it },
                        label = { Text("UF") },
                        singleLine = true,
                        modifier = Modifier.weight(0.35f)
                    )
                    OutlinedTextField(
                        value = crm,
                        onValueChange = { crm = it },
                        label = { Text("CRM Padrão") },
                        singleLine = true,
                        modifier = Modifier.weight(0.65f)
                    )
                }
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Especialidade / Setor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(clinicName, uf, specialty, crm) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Salvar Âncora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddClinicalCaseBottomSheet(
    clinicConfig: ClinicAnchorConfig,
    onDismiss: () -> Unit,
    onSaveCase: (
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
        deidentificationConfirmed: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var caseReference by rememberSaveable { mutableStateOf("CASO-005 • Paciente M.A. (Desidentificado)") }
    var responsibleHuman by rememberSaveable {
        mutableStateOf("Dra. Helena Vasconcelos (${clinicConfig.defaultCrm})")
    }
    var declaredPurpose by rememberSaveable {
        mutableStateOf("Registro estruturado para continuidade assistencial e passagem de plantão")
    }
    var contextScope by rememberSaveable {
        mutableStateOf("Atendimento em Sala de Observação • Reavaliação de 6h")
    }
    var evidencias by rememberSaveable {
        mutableStateOf("PA 124/78 mmHg, afebril, exames laboratoriais sem leucocitose, ausculta pulmonar limpa.")
    }
    var raciocinio by rememberSaveable {
        mutableStateOf("Estabilidade hemodinâmica e ausência de marcadores inflamatórios agudos.")
    }
    var decisao by rememberSaveable {
        mutableStateOf("Manter hidratação oral, alta hospitalar assistida e retorno ambulatorial em 48h.")
    }
    var deidentificationConfirmed by rememberSaveable { mutableStateOf(true) }

    val liveEval = remember(
        clinicConfig.clinicName,
        caseReference,
        responsibleHuman,
        declaredPurpose,
        contextScope,
        evidencias,
        raciocinio,
        decisao,
        deidentificationConfirmed
    ) {
        TcriaGovernanceEngine.evaluateCase(
            projectClinicAnchor = clinicConfig.clinicName,
            caseReference = caseReference,
            responsibleHuman = responsibleHuman,
            declaredPurpose = declaredPurpose,
            contextScope = contextScope,
            evidencias = evidencias,
            raciocinio = raciocinio,
            decisao = decisao,
            deidentificationConfirmed = deidentificationConfirmed
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Novo Registro Clínico Rastreável (TCRIA)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Âncora ativa: ${clinicConfig.clinicName} (${clinicConfig.uf})",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = caseReference,
                onValueChange = { caseReference = it },
                label = { Text("Referência do Caso / Apelido Desidentificado") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_case_reference")
            )

            OutlinedTextField(
                value = responsibleHuman,
                onValueChange = { responsibleHuman = it },
                label = { Text("Profissional Responsável + CRM (RULE_4.1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contextScope,
                onValueChange = { contextScope = it },
                label = { Text("Contexto Clínico & Escopo (RULE_1.2)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = evidencias,
                onValueChange = { evidencias = it },
                label = { Text("Evidências & Achados Objetivos (RULE_2.1)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = raciocinio,
                onValueChange = { raciocinio = it },
                label = { Text("Raciocínio Clínico Bidirecional (RULE_2.2)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = decisao,
                onValueChange = { decisao = it },
                label = { Text("Decisão / Conduta Registrada (RULE_3.2)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = deidentificationConfirmed,
                    onCheckedChange = { deidentificationConfirmed = it }
                )
                Text(
                    text = "Confirmo desidentificação LGPD (sem CPF, e-mail ou dados sensíveis diretos)",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            Text(
                text = "Pré-avaliação em Tempo Real: ${liveEval.overallStatus.label}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMonoFamily,
                color = when (liveEval.overallStatus) {
                    GovernanceStatus.VALID -> EmeraldPrimary
                    GovernanceStatus.WEAK -> AmberWarn
                    else -> CrimsonBlock
                },
                fontWeight = FontWeight.Bold
            )

            liveEval.gates.forEach { gate ->
                GateCheckItemRow(gate)
            }

            val liveLgpdReport = remember(
                caseReference,
                responsibleHuman,
                declaredPurpose,
                contextScope,
                evidencias,
                raciocinio,
                decisao
            ) {
                LgpdTextRedactor.inspectFields(
                    caseReference,
                    responsibleHuman,
                    declaredPurpose,
                    contextScope,
                    evidencias,
                    raciocinio,
                    decisao
                )
            }

            if (liveLgpdReport.wasRedacted) {
                Surface(
                    color = AmberWarn.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = liveLgpdReport.summaryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestInk,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                caseReference = LgpdTextRedactor.redact(caseReference)
                                responsibleHuman = LgpdTextRedactor.redact(responsibleHuman)
                                declaredPurpose = LgpdTextRedactor.redact(declaredPurpose)
                                contextScope = LgpdTextRedactor.redact(contextScope)
                                evidencias = LgpdTextRedactor.redact(evidencias)
                                raciocinio = LgpdTextRedactor.redact(raciocinio)
                                decisao = LgpdTextRedactor.redact(decisao)
                            }
                        ) {
                            Text("Redigir Agora", fontWeight = FontWeight.Bold, color = EmeraldDeep)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onSaveCase(
                            clinicConfig.clinicName,
                            clinicConfig.uf,
                            clinicConfig.specialty,
                            LgpdTextRedactor.redact(caseReference),
                            LgpdTextRedactor.redact(responsibleHuman),
                            LgpdTextRedactor.redact(declaredPurpose),
                            LgpdTextRedactor.redact(contextScope),
                            LgpdTextRedactor.redact(evidencias),
                            LgpdTextRedactor.redact(raciocinio),
                            LgpdTextRedactor.redact(decisao),
                            deidentificationConfirmed
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_confirm_save_case")
                ) {
                    Text("Salvar & Auditar")
                }
            }
        }
    }
}
