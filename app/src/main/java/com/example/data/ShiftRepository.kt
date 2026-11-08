package com.example.data

import kotlinx.coroutines.flow.Flow

class ShiftRepository(private val shiftDao: ShiftDao) {

    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShifts()

    val currentShifts: Flow<List<ShiftEntity>> = shiftDao.getShiftsByStatus("CURRENT")

    val upcomingShifts: Flow<List<ShiftEntity>> = shiftDao.getShiftsByStatus("UPCOMING")

    suspend fun insert(shift: ShiftEntity): Long {
        return shiftDao.insertShift(shift)
    }

    suspend fun update(shift: ShiftEntity) {
        shiftDao.updateShift(shift)
    }

    suspend fun deleteById(id: Int) {
        shiftDao.deleteShiftById(id)
    }

    suspend fun syncCloudShiftsToRoom(cloudShifts: List<CloudShift>) {
        for (cloud in cloudShifts) {
            if (cloud.id.isBlank()) continue
            val existing = shiftDao.getShiftByFirebaseDocId(cloud.id)
            val priority = when (cloud.shiftStatus) {
                "CURRENT" -> 1
                "UPCOMING" -> 5
                else -> 10
            }
            val progress = when (cloud.shiftStatus) {
                "CURRENT" -> existing?.progressFraction?.takeIf { it > 0f } ?: 0.35f
                "COMPLETED" -> 1.0f
                else -> 0.0f
            }
            val mapped = ShiftEntity(
                id = existing?.id ?: 0,
                firebaseDocId = cloud.id,
                staffName = cloud.staffName,
                roleTitle = cloud.roleTitle,
                crmOrRegistry = cloud.crmOrRegistry,
                department = cloud.department,
                category = cloud.category,
                shiftStatus = cloud.shiftStatus,
                shiftTimeLabel = cloud.shiftTimeLabel,
                dateLabel = cloud.dateLabel,
                locationRoom = cloud.locationRoom,
                permissionLevel = cloud.permissionLevel,
                notes = cloud.notes,
                voiceTranscript = cloud.voiceTranscript,
                progressFraction = progress,
                isOnCall = cloud.isOnCall,
                createdViaVoice = cloud.createdViaVoice,
                sortPriority = priority,
                timestamp = cloud.updatedAt?.toDate()?.time
                    ?: cloud.createdAt?.toDate()?.time
                    ?: System.currentTimeMillis()
            )
            shiftDao.insertShift(mapped)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        if (shiftDao.getShiftCount() == 0) {
            shiftDao.insertAll(defaultSampleShifts())
        }
    }

    companion object {
        fun defaultSampleShifts(): List<ShiftEntity> = listOf(
            ShiftEntity(
                staffName = "Dra. Helena Vasconcelos",
                roleTitle = "Médica Intensivista Chefe",
                crmOrRegistry = "CRM-SP 148.290",
                department = "UTI Adulto",
                category = "MEDICAL",
                shiftStatus = "CURRENT",
                shiftTimeLabel = "07:00 – 19:00 (12h)",
                dateLabel = "Hoje • Em Plantão",
                locationRoom = "Bloco A • Leitos 01 a 12",
                permissionLevel = "Coordenação Clínica Total",
                notes = "Supervisão de 9 pacientes críticos; passagem de plantão às 18:45.",
                progressFraction = 0.68f,
                isOnCall = false,
                createdViaVoice = false,
                sortPriority = 1
            ),
            ShiftEntity(
                staffName = "Dr. Rafael Mendonça",
                roleTitle = "Cirurgião de Trauma & Emergência",
                crmOrRegistry = "CRM-SP 162.814",
                department = "Pronto-Socorro / Trauma",
                category = "MEDICAL",
                shiftStatus = "CURRENT",
                shiftTimeLabel = "07:00 – 19:00 (12h)",
                dateLabel = "Hoje • Em Plantão",
                locationRoom = "Sala Vermelha & Box 1–6",
                permissionLevel = "Acesso Clínico & Cirúrgico",
                notes = "Equipe de trauma completa. Retaguarda ortopédica acionada.",
                voiceTranscript = "Dr. Rafael Mendonça cirurgião no Pronto-Socorro plantão hoje sobreaviso",
                progressFraction = 0.68f,
                isOnCall = true,
                createdViaVoice = true,
                sortPriority = 2
            ),
            ShiftEntity(
                staffName = "Carla Albuquerque",
                roleTitle = "Supervisora de Operações Clínicas",
                crmOrRegistry = "ADM-CL 0419",
                department = "Administração & Faturamento",
                category = "ADMIN",
                shiftStatus = "CURRENT",
                shiftTimeLabel = "08:00 – 18:00 (10h)",
                dateLabel = "Hoje • Turno Administrativo",
                locationRoom = "Central de Regulação • 2º Andar",
                permissionLevel = "Gestão de Escalas & Permissões",
                notes = "Auditoria de glosas convênios e liberação de leitos cirúrgicos.",
                progressFraction = 0.75f,
                isOnCall = false,
                createdViaVoice = false,
                sortPriority = 3
            ),
            ShiftEntity(
                staffName = "Dra. Beatriz Nogueira",
                roleTitle = "Pediatra Neonatologista",
                crmOrRegistry = "CRM-SP 179.503",
                department = "Pediatria & UTI Neonatal",
                category = "MEDICAL",
                shiftStatus = "CURRENT",
                shiftTimeLabel = "13:00 – 19:00 (6h)",
                dateLabel = "Hoje • Plantão Vespertino",
                locationRoom = "Ala Materno-Infantil • 3º Andar",
                permissionLevel = "Acesso Clínico Pediátrico",
                notes = "Acompanhamento de 4 recém-nascidos em observação.",
                progressFraction = 0.45f,
                isOnCall = false,
                createdViaVoice = false,
                sortPriority = 4
            ),
            ShiftEntity(
                staffName = "Dr. Lucas Cavalcanti",
                roleTitle = "Médico Plantonista Noturno",
                crmOrRegistry = "CRM-SP 191.044",
                department = "UTI Adulto",
                category = "MEDICAL",
                shiftStatus = "UPCOMING",
                shiftTimeLabel = "19:00 – 07:00 (12h)",
                dateLabel = "Hoje à Noite • Início em 2h",
                locationRoom = "Bloco A • Leitos 01 a 12",
                permissionLevel = "Acesso Clínico Plantonista",
                notes = "Assume escala noturna da Dra. Helena Vasconcelos.",
                voiceTranscript = "Dr. Lucas Cavalcanti na UTI Adulto plantão noturno",
                progressFraction = 0.0f,
                isOnCall = false,
                createdViaVoice = true,
                sortPriority = 5
            ),
            ShiftEntity(
                staffName = "Dra. Camila Fontes",
                roleTitle = "Anestesiologista de Sobreaviso",
                crmOrRegistry = "CRM-SP 154.992",
                department = "Centro Cirúrgico",
                category = "MEDICAL",
                shiftStatus = "UPCOMING",
                shiftTimeLabel = "19:00 – 07:00 (12h)",
                dateLabel = "Hoje à Noite • Sobreaviso",
                locationRoom = "Centro Cirúrgico • Salas 1 a 5",
                permissionLevel = "Coordenação Centro Cirúrgico",
                notes = "Duas cirurgias eletivas agendadas para início de turno.",
                progressFraction = 0.0f,
                isOnCall = true,
                createdViaVoice = false,
                sortPriority = 6
            ),
            ShiftEntity(
                staffName = "Marcos Vinícius Prado",
                roleTitle = "Coordenador de Recepção & Internação",
                crmOrRegistry = "ADM-CL 0892",
                department = "Recepção & Admissão",
                category = "ADMIN",
                shiftStatus = "UPCOMING",
                shiftTimeLabel = "19:00 – 07:00 (12h)",
                dateLabel = "Hoje à Noite • Turno Noturno",
                locationRoom = "Recepção Central • Térreo",
                permissionLevel = "Controle Administrativo & Leitos",
                notes = "Responsável pelo fechamento de caixa diurno e admissões noturnas.",
                progressFraction = 0.0f,
                isOnCall = false,
                createdViaVoice = false,
                sortPriority = 7
            ),
            ShiftEntity(
                staffName = "Dr. Henrique Azevedo",
                roleTitle = "Cardiologista Clínico",
                crmOrRegistry = "CRM-SP 133.710",
                department = "Unidade Coronariana",
                category = "MEDICAL",
                shiftStatus = "UPCOMING",
                shiftTimeLabel = "07:00 – 19:00 (12h)",
                dateLabel = "Amanhã • Plantão Diurno",
                locationRoom = "Bloco B • UCO Leitos 01–08",
                permissionLevel = "Coordenação Cardiologia",
                notes = "Avaliação pós-cateterismo e evolução matinal da ala coronariana.",
                progressFraction = 0.0f,
                isOnCall = true,
                createdViaVoice = false,
                sortPriority = 8
            )
        )
    }
}
