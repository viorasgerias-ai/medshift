package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudShiftRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleCloudShift() = CloudShift(
        staffName = "Dra. Mariana Costa",
        roleTitle = "Médica Intensivista",
        crmOrRegistry = "CRM-SP 186.420",
        department = "UTI Adulto",
        category = "MEDICAL",
        shiftStatus = "CURRENT",
        shiftTimeLabel = "07:00 – 19:00 (12h)",
        dateLabel = "Hoje • Em Plantão",
        locationRoom = "Bloco A • Leitos 01 a 12",
        permissionLevel = "Acesso Clínico Plantonista",
        notes = "Plantão criado por comando de voz.",
        voiceTranscript = "Dra. Mariana Costa intensivista na UTI Adulto plantão hoje",
        isOnCall = true,
        createdViaVoice = true
    )

    @Test
    fun createShift_authenticatedOwner_createsAndEmitsShift() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudShiftRepository(firestore)

        val shiftId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createShift(sampleCloudShift())
        }
        assertTrue(shiftId.isNotBlank())

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeShifts().first { list -> list.any { it.id == shiftId } }
        }
        assertTrue(emitted.any { it.id == shiftId && it.createdViaVoice })
    }

    @Test
    fun getShiftById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudShiftRepository(firestore)
        val shiftId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createShift(sampleCloudShift())
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudShiftRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getShiftById(aliceUid, shiftId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun observeShifts_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repository = CloudShiftRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeShifts().first()
            }
            fail("Expected unauthenticated observeShifts to fail")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException() != null)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@medshift.test"
        const val BOB_EMAIL = "bob@medshift.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
