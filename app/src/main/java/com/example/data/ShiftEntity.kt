package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseDocId: String = "",
    val staffName: String,
    val roleTitle: String,
    val crmOrRegistry: String,
    val department: String,
    val category: String, // "MEDICAL" or "ADMIN"
    val shiftStatus: String, // "CURRENT", "UPCOMING", "COMPLETED"
    val shiftTimeLabel: String, // e.g., "07:00 – 19:00 (12h)"
    val dateLabel: String, // e.g., "Hoje • Plantão Diurno"
    val locationRoom: String, // e.g., "Bloco A • UTI Leitos 01–10"
    val permissionLevel: String, // e.g., "Coordenação Clínica", "Acesso Plantonista", "Gestão Admin"
    val notes: String,
    val voiceTranscript: String = "",
    val progressFraction: Float = 0.0f, // 0.0f..1.0f for active shifts
    val isOnCall: Boolean = false,
    val createdViaVoice: Boolean = false,
    val sortPriority: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
