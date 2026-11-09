package com.example.governance

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GovernanceStatus(val label: String) {
    VALID("VÁLIDO • Auditável"),
    WEAK("FRÁGIL • Sem Evidência"),
    BLOCKED("BLOQUEADO • Governança TCRIA"),
    INVALID("INVÁLIDO • Sem Âncora/Contexto")
}

data class GateCheckResult(
    val gateName: String,
    val ruleCode: String,
    val passed: Boolean,
    val status: GovernanceStatus,
    val reason: String
)

data class TcriaAuditEvaluation(
    val overallStatus: GovernanceStatus,
    val gates: List<GateCheckResult>,
    val auditSummary: String
)

@Entity(tableName = "clinical_cases")
data class ClinicalCaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseDocId: String = "",
    val projectClinicAnchor: String, // RULE 1.1: Project / Clinic anchor
    val clinicUf: String = "RJ",
    val clinicSpecialty: String = "Clínica Geral",
    val caseReference: String, // Patient alias or de-identified code (e.g. CASO-001 or Maria S.)
    val responsibleHuman: String, // RULE 4.1: Accountable professional + CRM
    val declaredPurpose: String, // RULE 4.1: Declared purpose of the record
    val contextScope: String, // RULE 1.2: Context, scope, and clinical conditions
    val evidencias: String, // RULE 2.1: Evidence & clinical findings
    val raciocinio: String, // RULE 2.2: Bidirectional clinical reasoning
    val decisao: String, // RULE 3.1 & 3.2: Clinical decision or conduct
    val deidentificationConfirmed: Boolean = true,
    val governanceStatus: String = GovernanceStatus.VALID.name,
    val governanceReport: String = "",
    val createdViaVoice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class CloudClinicalCase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectClinicAnchor: String = "",
    val clinicUf: String = "RJ",
    val clinicSpecialty: String = "Clínica Geral",
    val caseReference: String = "",
    val responsibleHuman: String = "",
    val declaredPurpose: String = "",
    val contextScope: String = "",
    val evidencias: String = "",
    val raciocinio: String = "",
    val decisao: String = "",
    val deidentificationConfirmed: Boolean = true,
    val governanceStatus: String = "VALID",
    val governanceReport: String = "",
    val createdViaVoice: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Implements the TCRIA Core Governance Ruleset & Two-Layer Blocked Artifact Review
 * combined with LGPD De-identification validation from Médicos a Postos.
 */
object TcriaGovernanceEngine {

    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
    private val CPF_REGEX = Regex("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b")
    private val PRESCRIPTIVE_CONDEMNATORY_TERMS = listOf(
        "culpado", "negligência comprovada", "condenação automática", "fraude absoluta"
    )

    fun evaluateCase(
        projectClinicAnchor: String,
        caseReference: String,
        responsibleHuman: String,
        declaredPurpose: String,
        contextScope: String,
        evidencias: String,
        raciocinio: String,
        decisao: String,
        deidentificationConfirmed: Boolean
    ): TcriaAuditEvaluation {
        val gates = mutableListOf<GateCheckResult>()

        // RULE 1.1 — PROJECT ANCHOR
        val hasProjectAnchor = projectClinicAnchor.trim().length >= 3
        gates.add(
            GateCheckResult(
                gateName = "Project Anchor Gate",
                ruleCode = "RULE_1.1",
                passed = hasProjectAnchor,
                status = if (hasProjectAnchor) GovernanceStatus.VALID else GovernanceStatus.INVALID,
                reason = if (hasProjectAnchor) {
                    "Vinculado à clínica/projeto: ${projectClinicAnchor.trim()}"
                } else {
                    "Entidade órfã: sem clínica ou projeto âncora vinculado."
                }
            )
        )

        // RULE 1.2 — CONTEXT REQUIREMENT
        val hasContext = contextScope.trim().length >= 5 && caseReference.trim().isNotEmpty()
        gates.add(
            GateCheckResult(
                gateName = "Context Integrity Gate",
                ruleCode = "RULE_1.2",
                passed = hasContext,
                status = if (hasContext) GovernanceStatus.VALID else GovernanceStatus.INVALID,
                reason = if (hasContext) {
                    "Contexto, escopo e referência do caso preenchidos."
                } else {
                    "Contexto ausente: informe condições clínicas e referência do caso."
                }
            )
        )

        // RULE 4.1 — HUMAN ACCOUNTABILITY (complianceGate)
        val hasAccountability = responsibleHuman.trim().length >= 4 && declaredPurpose.trim().length >= 4
        gates.add(
            GateCheckResult(
                gateName = "Compliance & Accountability Gate",
                ruleCode = "RULE_4.1",
                passed = hasAccountability,
                status = if (hasAccountability) GovernanceStatus.VALID else GovernanceStatus.BLOCKED,
                reason = if (hasAccountability) {
                    "Responsável humano (${responsibleHuman.trim()}) e finalidade declarada."
                } else {
                    "Bloqueado: ausência de responsável humano (CRM/Nome) ou finalidade declarada."
                }
            )
        )

        // RULE 2.1 & 2.2 — TRACEABILITY & EVIDENCE LINK (traceabilityCheck)
        val hasEvidence = evidencias.trim().length >= 5
        val hasReasoningAndDecision = raciocinio.trim().length >= 4 && decisao.trim().length >= 4
        val tracePassed = hasEvidence && hasReasoningAndDecision
        val traceStatus = when {
            !hasReasoningAndDecision -> GovernanceStatus.INVALID
            !hasEvidence -> GovernanceStatus.WEAK
            else -> GovernanceStatus.VALID
        }
        gates.add(
            GateCheckResult(
                gateName = "Traceability & Evidence Check",
                ruleCode = "RULE_2.1 / 5.1",
                passed = tracePassed,
                status = traceStatus,
                reason = when {
                    !hasReasoningAndDecision -> "Raciocínio clínico ou decisão/conduta ausentes."
                    !hasEvidence -> "Afirmação frágil (WEAK): decisão sem vínculo explícito a evidências/achados."
                    else -> "Cadeia bidirecional completa: Evidência → Raciocínio → Conduta."
                }
            )
        )

        // PRESCRIPTIVE & LGPD DE-IDENTIFICATION GATE (prescriptiveGate)
        val combinedText = "$evidencias $raciocinio $decisao"
        val containsPii = EMAIL_REGEX.containsMatchIn(combinedText) || CPF_REGEX.containsMatchIn(combinedText)
        val containsCondemnatory = PRESCRIPTIVE_CONDEMNATORY_TERMS.any {
            combinedText.lowercase().contains(it)
        }
        val prescriptivePassed = !containsCondemnatory && (!deidentificationConfirmed || !containsPii)
        gates.add(
            GateCheckResult(
                gateName = "Prescriptive & LGPD Gate",
                ruleCode = "RULE_3.1 / LGPD",
                passed = prescriptivePassed,
                status = if (prescriptivePassed) GovernanceStatus.VALID else GovernanceStatus.BLOCKED,
                reason = when {
                    containsCondemnatory -> "Bloqueado pelo prescriptiveGate: linguagem condenatória sem revisão humana."
                    deidentificationConfirmed && containsPii ->
                        "Bloqueado LGPD: detectado e-mail ou CPF em registro marcado como desidentificado."
                    else -> "Registro compatível com guarda documental e desidentificação."
                }
            )
        )

        val overallStatus = when {
            gates.any { it.status == GovernanceStatus.BLOCKED } -> GovernanceStatus.BLOCKED
            gates.any { it.status == GovernanceStatus.INVALID } -> GovernanceStatus.INVALID
            gates.any { it.status == GovernanceStatus.WEAK } -> GovernanceStatus.WEAK
            else -> GovernanceStatus.VALID
        }

        val failedReasons = gates.filter { !it.passed }.joinToString(" | ") { "${it.ruleCode}: ${it.reason}" }
        val summary = if (failedReasons.isEmpty()) {
            "Aprovado em todos os 5 grupos de regras TCRIA (Anchor, Context, Evidence, Accountability, LGPD)."
        } else {
            failedReasons
        }

        return TcriaAuditEvaluation(
            overallStatus = overallStatus,
            gates = gates,
            auditSummary = summary
        )
    }
}
