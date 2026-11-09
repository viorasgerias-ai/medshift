package com.example.governance

import com.example.data.ShiftEntity
import com.example.ui.ParsedVoiceShift

enum class SensitivePatternType(
    val label: String,
    val replacementToken: String
) {
    CPF("CPF", "[CPF REDIGIDO - LGPD]"),
    EMAIL("E-mail Pessoal", "[EMAIL REDIGIDO - LGPD]"),
    PHONE("Telefone/Celular", "[TELEFONE REDIGIDO - LGPD]"),
    CNS("Cartão SUS (CNS)", "[CNS REDIGIDO - LGPD]"),
    RG("Documento RG", "[RG REDIGIDO - LGPD]")
}

data class LgpdRedactionResult(
    val originalText: String,
    val sanitizedText: String,
    val detectedTypes: Set<SensitivePatternType>,
    val redactedCount: Int
) {
    val wasRedacted: Boolean
        get() = redactedCount > 0

    val summaryLabel: String
        get() = if (!wasRedacted) {
            "Conformidade LGPD: nenhum CPF, e-mail ou identificador sensível detectado."
        } else {
            val labels = detectedTypes.joinToString(", ") { it.label }
            "Proteção LGPD Ativa: $redactedCount ocorrência(s) redigida(s) automaticamente ($labels)."
        }
}

/**
 * LGPD Text Processing & Automatic Redaction Service.
 * Scans voice transcripts and manual form inputs for sensitive Brazilian PII patterns
 * (CPF, personal emails, mobile phones, RG, and Cartão SUS/CNS) and redacts them
 * before persisting records to Room or syncing with cloud storage.
 */
object LgpdTextRedactor {

    // Matches formatted CPFs (000.000.000-00 or 000 000 000 00) and standalone 11-digit CPFs
    private val FORMATTED_CPF_REGEX = Regex(
        pattern = "(?<!\\d)\\d{3}[.\\s]\\d{3}[.\\s]\\d{3}[-\\s]\\d{2}(?!\\d)"
    )
    private val LABELED_OR_RAW_CPF_REGEX = Regex(
        pattern = "(?i)(?:\\bcpf\\s*[:#\\-]?\\s*)?\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b"
    )

    // Personal or patient email addresses
    private val EMAIL_REGEX = Regex(
        pattern = "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    )

    // Brazilian CNS (Cartão Nacional de Saúde - 15 digits starting with 1, 2, 7, 8, or 9)
    private val CNS_REGEX = Regex(
        pattern = "(?i)(?:\\bcns\\s*[:#\\-]?\\s*|\\bsus\\s*[:#\\-]?\\s*)?(?<!\\d)[12789]\\d{2}[.\\s]?\\d{4}[.\\s]?\\d{4}[.\\s]?\\d{4}(?!\\d)"
    )

    // Labeled RG numbers (e.g., RG 12.345.678-9 or RG: 12345678-X)
    private val RG_REGEX = Regex(
        pattern = "(?i)\\brg\\s*[:#\\-]?\\s*\\d{1,2}\\.?\\d{3}\\.?\\d{3}[\\-\\s]?[0-9xX]\\b"
    )

    // Brazilian phone numbers with DDD, e.g., (21) 99876-5432, +55 11 98765-4321, tel 21 98888-7777
    private val PHONE_REGEX = Regex(
        pattern = "(?i)(?:\\b(?:tel|telefone|celular|whatsapp|contato)\\s*[:#\\-]?\\s*)?(?:\\+55\\s*)?(?:\\(?\\d{2}\\)?\\s*)9?\\d{4}[\\-\\s]\\d{4}\\b"
    )

    /**
     * Scans [input] for sensitive LGPD patterns and replaces all matches with standardized
     * redaction tokens.
     */
    fun redactWithReport(input: String): LgpdRedactionResult {
        if (input.isBlank()) {
            return LgpdRedactionResult(
                originalText = input,
                sanitizedText = input,
                detectedTypes = emptySet(),
                redactedCount = 0
            )
        }

        var working = input
        val detected = linkedSetOf<SensitivePatternType>()
        var count = 0

        // 1. Redact Emails
        val emailMatches = EMAIL_REGEX.findAll(working).count()
        if (emailMatches > 0) {
            detected.add(SensitivePatternType.EMAIL)
            count += emailMatches
            working = EMAIL_REGEX.replace(working, SensitivePatternType.EMAIL.replacementToken)
        }

        // 2. Redact CNS (15 digits) before 11-digit CPF to avoid partial digit collisions
        val cnsMatches = CNS_REGEX.findAll(working).count()
        if (cnsMatches > 0) {
            detected.add(SensitivePatternType.CNS)
            count += cnsMatches
            working = CNS_REGEX.replace(working, SensitivePatternType.CNS.replacementToken)
        }

        // 3. Redact formatted and raw CPFs
        val formattedCpfMatches = FORMATTED_CPF_REGEX.findAll(working).count()
        if (formattedCpfMatches > 0) {
            detected.add(SensitivePatternType.CPF)
            count += formattedCpfMatches
            working = FORMATTED_CPF_REGEX.replace(working, SensitivePatternType.CPF.replacementToken)
        }

        val rawCpfMatches = LABELED_OR_RAW_CPF_REGEX.findAll(working).count()
        if (rawCpfMatches > 0) {
            detected.add(SensitivePatternType.CPF)
            count += rawCpfMatches
            working = LABELED_OR_RAW_CPF_REGEX.replace(working, SensitivePatternType.CPF.replacementToken)
        }

        // 4. Redact Labeled RGs
        val rgMatches = RG_REGEX.findAll(working).count()
        if (rgMatches > 0) {
            detected.add(SensitivePatternType.RG)
            count += rgMatches
            working = RG_REGEX.replace(working, SensitivePatternType.RG.replacementToken)
        }

        // 5. Redact Phone / WhatsApp numbers
        val phoneMatches = PHONE_REGEX.findAll(working).count()
        if (phoneMatches > 0) {
            detected.add(SensitivePatternType.PHONE)
            count += phoneMatches
            working = PHONE_REGEX.replace(working, SensitivePatternType.PHONE.replacementToken)
        }

        return LgpdRedactionResult(
            originalText = input,
            sanitizedText = working,
            detectedTypes = detected,
            redactedCount = count
        )
    }

    /**
     * Convenience method returning only the sanitized string.
     */
    fun redact(input: String): String = redactWithReport(input).sanitizedText

    /**
     * Scans multiple fields at once and returns an aggregated [LgpdRedactionResult].
     */
    fun inspectFields(vararg fields: String): LgpdRedactionResult {
        val combined = fields.joinToString(" ")
        return redactWithReport(combined)
    }

    /**
     * Ensures a [ShiftEntity] has all free-text and voice transcript fields redacted
     * prior to saving into Room.
     */
    fun sanitizeShiftEntity(shift: ShiftEntity): ShiftEntity {
        return shift.copy(
            staffName = redact(shift.staffName),
            roleTitle = redact(shift.roleTitle),
            crmOrRegistry = redact(shift.crmOrRegistry),
            department = redact(shift.department),
            locationRoom = redact(shift.locationRoom),
            notes = redact(shift.notes),
            voiceTranscript = redact(shift.voiceTranscript)
        )
    }

    /**
     * Ensures a [ParsedVoiceShift] extracted from voice commands has sensitive patterns redacted.
     */
    fun sanitizeParsedVoiceShift(parsed: ParsedVoiceShift): ParsedVoiceShift {
        return parsed.copy(
            staffName = redact(parsed.staffName),
            roleTitle = redact(parsed.roleTitle),
            crmOrRegistry = redact(parsed.crmOrRegistry),
            department = redact(parsed.department),
            locationRoom = redact(parsed.locationRoom),
            notes = redact(parsed.notes)
        )
    }

    /**
     * Ensures a [ClinicalCaseEntity] has all clinical narrative and case reference fields
     * redacted prior to saving into Room.
     */
    fun sanitizeClinicalCaseEntity(clinicalCase: ClinicalCaseEntity): ClinicalCaseEntity {
        return clinicalCase.copy(
            caseReference = redact(clinicalCase.caseReference),
            responsibleHuman = redact(clinicalCase.responsibleHuman),
            declaredPurpose = redact(clinicalCase.declaredPurpose),
            contextScope = redact(clinicalCase.contextScope),
            evidencias = redact(clinicalCase.evidencias),
            raciocinio = redact(clinicalCase.raciocinio),
            decisao = redact(clinicalCase.decisao)
        )
    }
}
