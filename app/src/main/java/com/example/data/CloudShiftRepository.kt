package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class CloudShift(
    @DocumentId val id: String = "",
    val userId: String = "",
    val staffName: String = "",
    val roleTitle: String = "",
    val crmOrRegistry: String = "",
    val department: String = "",
    val category: String = "MEDICAL",
    val shiftStatus: String = "CURRENT",
    val shiftTimeLabel: String = "07:00 – 19:00 (12h)",
    val dateLabel: String = "Hoje • Em Plantão",
    val locationRoom: String = "Bloco A • Leitos 01 a 12",
    val permissionLevel: String = "Acesso Clínico Plantonista",
    val notes: String = "",
    val voiceTranscript: String = "",
    val isOnCall: Boolean = false,
    val createdViaVoice: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudShiftRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeShifts(): Flow<List<CloudShift>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/shifts"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("shifts")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudShift::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createShift(shift: CloudShift): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("shifts").document()
        val payload = mapOf(
            "userId" to uid,
            "staffName" to shift.staffName.ifBlank { "Profissional Plantonista" }.take(120),
            "roleTitle" to shift.roleTitle.ifBlank { "Médico Plantonista" }.take(120),
            "crmOrRegistry" to shift.crmOrRegistry.ifBlank { "CRM-SP 204.510" }.take(60),
            "department" to shift.department.ifBlank { "Clínica Geral" }.take(100),
            "category" to if (shift.category == "ADMIN") "ADMIN" else "MEDICAL",
            "shiftStatus" to when (shift.shiftStatus) {
                "UPCOMING", "COMPLETED" -> shift.shiftStatus
                else -> "CURRENT"
            },
            "shiftTimeLabel" to shift.shiftTimeLabel.ifBlank { "07:00 – 19:00 (12h)" }.take(80),
            "dateLabel" to shift.dateLabel.ifBlank { "Hoje • Em Plantão" }.take(80),
            "locationRoom" to shift.locationRoom.ifBlank { "Ala Central" }.take(120),
            "permissionLevel" to shift.permissionLevel.ifBlank { "Acesso Clínico Plantonista" }.take(100),
            "notes" to shift.notes.take(500),
            "voiceTranscript" to shift.voiceTranscript.take(500),
            "isOnCall" to shift.isOnCall,
            "createdViaVoice" to shift.createdViaVoice,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getShiftById(ownerUid: String, shiftId: String): CloudShift? {
        val docRef = db.collection("users").document(ownerUid).collection("shifts").document(shiftId)
        return try {
            val snap = docRef.get().await()
            snap.toObject(CloudShift::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateShiftStatus(shiftId: String, newStatus: String, newDateLabel: String) {
        val uid = requireUserId()
        require(shiftId.isNotBlank()) { "Shift ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("shifts").document(shiftId)
        val updates = mapOf(
            "shiftStatus" to newStatus,
            "dateLabel" to newDateLabel.take(80),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteShift(shiftId: String) {
        val uid = requireUserId()
        require(shiftId.isNotBlank()) { "Shift ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("shifts").document(shiftId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteShifts(shiftIds: List<String>) {
        val validIds = shiftIds.filter { it.isNotBlank() }
        if (validIds.isEmpty()) return
        val uid = requireUserId()
        val collectionPath = "users/$uid/shifts"
        try {
            val batch = db.batch()
            for (shiftId in validIds) {
                val docRef = db.collection("users").document(uid).collection("shifts").document(shiftId)
                batch.delete(docRef)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, collectionPath)
            throw e
        }
    }

    fun observeClinicalCases(): Flow<List< com.example.governance.CloudClinicalCase >> = flow {
        val uid = requireUserId()
        val path = "users/$uid/clinical_cases"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("clinical_cases")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        com.example.governance.CloudClinicalCase::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createClinicalCase(clinicalCase: com.example.governance.CloudClinicalCase): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clinical_cases").document()
        val validStatus = when (clinicalCase.governanceStatus) {
            "VALID", "WEAK", "BLOCKED", "INVALID" -> clinicalCase.governanceStatus
            else -> "VALID"
        }
        val payload = mapOf(
            "userId" to uid,
            "projectClinicAnchor" to clinicalCase.projectClinicAnchor.ifBlank { "Clínica Médicos a Postos" }.take(150),
            "clinicUf" to clinicalCase.clinicUf.ifBlank { "RJ" }.take(10),
            "clinicSpecialty" to clinicalCase.clinicSpecialty.ifBlank { "Clínica Geral" }.take(100),
            "caseReference" to clinicalCase.caseReference.ifBlank { "CASO-001" }.take(120),
            "responsibleHuman" to clinicalCase.responsibleHuman.ifBlank { "Médico Plantonista" }.take(120),
            "declaredPurpose" to clinicalCase.declaredPurpose.ifBlank { "Continuidade assistencial" }.take(300),
            "contextScope" to clinicalCase.contextScope.ifBlank { "Atendimento clínico" }.take(400),
            "evidencias" to clinicalCase.evidencias.take(1000),
            "raciocinio" to clinicalCase.raciocinio.take(1000),
            "decisao" to clinicalCase.decisao.take(1000),
            "deidentificationConfirmed" to clinicalCase.deidentificationConfirmed,
            "governanceStatus" to validStatus,
            "governanceReport" to clinicalCase.governanceReport.take(800),
            "createdViaVoice" to clinicalCase.createdViaVoice,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteClinicalCase(caseId: String) {
        val uid = requireUserId()
        require(caseId.isNotBlank()) { "Case ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("clinical_cases").document(caseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}

