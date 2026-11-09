package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ShiftRepository
import com.example.ui.ShiftViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ShiftRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShiftRepository(database.shiftDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MedShift", appName)
    }

    @Test
    fun `seed and query current and upcoming shifts from Room`() = runBlocking {
        repository.seedInitialDataIfEmpty()
        val allShifts = repository.allShifts.first()
        assertTrue(allShifts.isNotEmpty())
        assertTrue(allShifts.any { it.shiftStatus == "CURRENT" })
        assertTrue(allShifts.any { it.shiftStatus == "UPCOMING" })
        assertTrue(allShifts.any { it.category == "MEDICAL" })
        assertTrue(allShifts.any { it.category == "ADMIN" })
    }

    @Test
    fun `bulk delete removes selected shifts from Room`() = runBlocking {
        repository.seedInitialDataIfEmpty()
        val initialShifts = repository.allShifts.first()
        val idsToDelete = initialShifts.take(3).map { it.id }

        repository.deleteByIds(idsToDelete)

        val remainingShifts = repository.allShifts.first()
        assertEquals(initialShifts.size - 3, remainingShifts.size)
        idsToDelete.forEach { deletedId ->
            assertFalse(remainingShifts.any { it.id == deletedId })
        }
    }

    @Test
    fun `parse voice command into structured shift`() {
        val parsed = ShiftViewModel.parseVoiceTranscript(
            "Dra. Mariana Costa intensivista na UTI Adulto plantão noturno sobreaviso"
        )
        assertEquals("Dra. Mariana Costa", parsed.staffName)
        assertEquals("UTI Adulto", parsed.department)
        assertEquals("UPCOMING", parsed.shiftStatus)
        assertTrue(parsed.isOnCall)
    }

    @Test
    fun `seed and query TCRIA clinical cases from Room`() = runBlocking {
        repository.seedInitialDataIfEmpty()
        val allCases = repository.allClinicalCases.first()
        assertTrue(allCases.isNotEmpty())
        assertTrue(allCases.any { it.governanceStatus == "VALID" })
        assertTrue(allCases.any { it.governanceStatus == "WEAK" })
        assertTrue(allCases.any { it.governanceStatus == "BLOCKED" })
    }

    @Test
    fun `automatic LGPD redaction strips CPF and email before saving shift to Room`() = runBlocking {
        val rawShift = com.example.data.ShiftEntity(
            staffName = "Dr. Carlos Mendes",
            roleTitle = "Médico Plantonista",
            crmOrRegistry = "CRM-RJ 112.340",
            department = "UTI Adulto",
            category = "MEDICAL",
            shiftStatus = "CURRENT",
            shiftTimeLabel = "07:00 – 19:00 (12h)",
            dateLabel = "Hoje • Em Plantão",
            locationRoom = "Leito 03",
            permissionLevel = "Acesso Clínico Plantonista",
            notes = "Acompanhamento de paciente CPF 111.222.333-44 e contato paciente@teste.com.br",
            voiceTranscript = "Dr. Carlos Mendes na UTI paciente CPF 111.222.333-44 email paciente@teste.com.br",
            createdViaVoice = true
        )

        val insertedId = repository.insert(rawShift).toInt()
        val savedShift = repository.allShifts.first().first { it.id == insertedId }

        assertFalse(savedShift.notes.contains("111.222.333-44"))
        assertFalse(savedShift.notes.contains("paciente@teste.com.br"))
        assertTrue(savedShift.notes.contains("[CPF REDIGIDO - LGPD]"))
        assertTrue(savedShift.notes.contains("[EMAIL REDIGIDO - LGPD]"))
        assertFalse(savedShift.voiceTranscript.contains("111.222.333-44"))
        assertTrue(savedShift.voiceTranscript.contains("[CPF REDIGIDO - LGPD]"))
    }
}


