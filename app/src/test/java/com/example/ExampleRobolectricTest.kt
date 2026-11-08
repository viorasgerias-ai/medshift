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
    fun `parse voice command into structured shift`() {
        val parsed = ShiftViewModel.parseVoiceTranscript(
            "Dra. Mariana Costa intensivista na UTI Adulto plantão noturno sobreaviso"
        )
        assertEquals("Dra. Mariana Costa", parsed.staffName)
        assertEquals("UTI Adulto", parsed.department)
        assertEquals("UPCOMING", parsed.shiftStatus)
        assertTrue(parsed.isOnCall)
    }
}
