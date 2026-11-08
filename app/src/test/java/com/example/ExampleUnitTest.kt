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
}
