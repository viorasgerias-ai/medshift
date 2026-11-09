package com.example

import com.example.data.ShiftRepository
import com.example.ui.ShiftViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun defaultSampleShifts_containsCurrentAndUpcomingShifts() {
        val shifts = ShiftRepository.defaultSampleShifts()
        assertTrue(shifts.isNotEmpty())
        assertTrue(shifts.any { it.shiftStatus == "CURRENT" })
        assertTrue(shifts.any { it.shiftStatus == "UPCOMING" })
        assertTrue(shifts.any { it.category == "MEDICAL" })
        assertTrue(shifts.any { it.category == "ADMIN" })
    }

    @Test
    fun parseVoiceTranscript_extractsMedicalShiftDetails() {
        val parsed = ShiftViewModel.parseVoiceTranscript(
            "Dra. Mariana Costa intensivista na UTI Adulto plantão noturno sobreaviso"
        )
        assertEquals("Dra. Mariana Costa", parsed.staffName)
        assertEquals("UTI Adulto", parsed.department)
        assertEquals("UPCOMING", parsed.shiftStatus)
        assertEquals("MEDICAL", parsed.category)
        assertTrue(parsed.isOnCall)
    }

    @Test
    fun parseVoiceTranscript_extractsAdminShiftDetails() {
        val parsed = ShiftViewModel.parseVoiceTranscript(
            "Coordenador Roberto Dias na Administração plantão diurno"
        )
        assertEquals("ADMIN", parsed.category)
        assertEquals("CURRENT", parsed.shiftStatus)
    }

    @Test
    fun tcriaGovernanceEngine_validatesCompleteAndBlockedCases() {
        val validEval = com.example.governance.TcriaGovernanceEngine.evaluateCase(
            projectClinicAnchor = "Clínica Médicos a Postos",
            caseReference = "CASO-001 • Maria S.",
            responsibleHuman = "Dra. Helena Vasconcelos (CRM-RJ 52.10482-9)",
            declaredPurpose = "Continuidade assistencial",
            contextScope = "Observação clínica 48h",
            evidencias = "PA 128/80 mmHg, ECG normal, troponina negativa.",
            raciocinio = "Sem sinais de alarme coronariano agudo.",
            decisao = "Alta assistida e retorno em 72h.",
            deidentificationConfirmed = true
        )
        assertEquals(com.example.governance.GovernanceStatus.VALID, validEval.overallStatus)

        val blockedPiiEval = com.example.governance.TcriaGovernanceEngine.evaluateCase(
            projectClinicAnchor = "Clínica Médicos a Postos",
            caseReference = "CASO-004",
            responsibleHuman = "Dra. Helena Vasconcelos",
            declaredPurpose = "Triagem",
            contextScope = "Recepção",
            evidencias = "CPF 123.456.789-00 e email teste@clinica.com",
            raciocinio = "Indexação direta",
            decisao = "Aguardar revisão",
            deidentificationConfirmed = true
        )
        assertEquals(com.example.governance.GovernanceStatus.BLOCKED, blockedPiiEval.overallStatus)
    }

    @Test
    fun lgpdTextRedactor_redactsCpfEmailPhoneRgAndCns() {
        val rawInput =
            "Paciente CPF 123.456.789-00, e-mail maria.silva@pessoal.com.br, celular (21) 99876-5432, RG 12.345.678-9 e CNS 700001234567890"
        val report = com.example.governance.LgpdTextRedactor.redactWithReport(rawInput)

        assertTrue(report.wasRedacted)
        assertTrue(report.redactedCount >= 5)
        assertTrue(report.sanitizedText.contains("[CPF REDIGIDO - LGPD]"))
        assertTrue(report.sanitizedText.contains("[EMAIL REDIGIDO - LGPD]"))
        assertTrue(report.sanitizedText.contains("[TELEFONE REDIGIDO - LGPD]"))
        assertTrue(report.sanitizedText.contains("[RG REDIGIDO - LGPD]"))
        assertTrue(report.sanitizedText.contains("[CNS REDIGIDO - LGPD]"))
        assertTrue(!report.sanitizedText.contains("123.456.789-00"))
        assertTrue(!report.sanitizedText.contains("maria.silva@pessoal.com.br"))
    }
}


