package com.example.data

import com.example.governance.ClinicalCaseEntity
import com.example.governance.CloudClinicalCase
import com.example.governance.GovernanceStatus
import com.example.governance.LgpdTextRedactor
import com.example.governance.TcriaGovernanceEngine
import kotlinx.coroutines.flow.Flow

class ShiftRepository(private val shiftDao: ShiftDao) {

    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShifts()

    val currentShifts: Flow<List<ShiftEntity>> = shiftDao.getShiftsByStatus("CURRENT")

    val upcomingShifts: Flow<List<ShiftEntity>> = shiftDao.getShiftsByStatus("UPCOMING")

    val allClinicalCases: Flow<List<ClinicalCaseEntity>> = shiftDao.getAllClinicalCases()

    suspend fun insert(shift: ShiftEntity): Long {
        val sanitized = LgpdTextRedactor.sanitizeShiftEntity(shift)
        return shiftDao.insertShift(sanitized)
    }

    suspend fun update(shift: ShiftEntity) {
        val sanitized = LgpdTextRedactor.sanitizeShiftEntity(shift)
        shiftDao.updateShift(sanitized)
    }

    suspend fun deleteById(id: Int) {
        shiftDao.deleteShiftById(id)
    }

    suspend fun deleteByIds(ids: List<Int>) {
        if (ids.isNotEmpty()) {
            shiftDao.deleteShiftsByIds(ids)
        }
    }

    suspend fun insertClinicalCase(clinicalCase: ClinicalCaseEntity): Long {
        val sanitized = LgpdTextRedactor.sanitizeClinicalCaseEntity(clinicalCase)
        return shiftDao.insertClinicalCase(sanitized)
    }

    suspend fun deleteClinicalCaseById(id: Int) {
        shiftDao.deleteClinicalCaseById(id)
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
            val mapped = LgpdTextRedactor.sanitizeShiftEntity(
                ShiftEntity(
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
            )
            shiftDao.insertShift(mapped)
        }
    }

    suspend fun syncCloudCasesToRoom(cloudCases: List<CloudClinicalCase>) {
        for (cloud in cloudCases) {
            if (cloud.id.isBlank()) continue
            val existing = shiftDao.getClinicalCaseByFirebaseDocId(cloud.id)
            val mapped = LgpdTextRedactor.sanitizeClinicalCaseEntity(
                ClinicalCaseEntity(
                    id = existing?.id ?: 0,
                    firebaseDocId = cloud.id,
                    projectClinicAnchor = cloud.projectClinicAnchor,
                    clinicUf = cloud.clinicUf,
                    clinicSpecialty = cloud.clinicSpecialty,
                    caseReference = cloud.caseReference,
                    responsibleHuman = cloud.responsibleHuman,
                    declaredPurpose = cloud.declaredPurpose,
                    contextScope = cloud.contextScope,
                    evidencias = cloud.evidencias,
                    raciocinio = cloud.raciocinio,
                    decisao = cloud.decisao,
                    deidentificationConfirmed = cloud.deidentificationConfirmed,
                    governanceStatus = cloud.governanceStatus,
                    governanceReport = cloud.governanceReport,
                    createdViaVoice = cloud.createdViaVoice,
                    timestamp = cloud.updatedAt?.toDate()?.time
                        ?: cloud.createdAt?.toDate()?.time
                        ?: System.currentTimeMillis()
                )
            )
            shiftDao.insertClinicalCase(mapped)
        }
    }


    suspend fun seedInitialDataIfEmpty() {
        if (shiftDao.getShiftCount() == 0) {
            shiftDao.insertAll(defaultSampleShifts())
        }
        if (shiftDao.getClinicalCaseCount() == 0) {
            shiftDao.insertAllClinicalCases(defaultSampleClinicalCases())
        }
    }

    companion object {
        fun defaultSampleClinicalCases(): List<ClinicalCaseEntity> {
            val rawCases = listOf(
                ClinicalCaseEntity(
                    projectClinicAnchor = "Clínica Médicos a Postos • Unidade Central",
                    clinicUf = "RJ",
                    clinicSpecialty = "Clínica Geral & Emergência",
                    caseReference = "CASO-001 • Maria S. (Desidentificado)",
                    responsibleHuman = "Dra. Helena Vasconcelos (CRM-RJ 52.10482-9)",
                    declaredPurpose = "Continuidade assistencial e passagem de plantão auditável",
                    contextScope = "Atendimento inicial em Sala de Observação • Quadro subagudo 48h",
                    evidencias = "Queixa álgica torácica atípica há 48h, PA 138/86 mmHg, SpO2 98%, ECG sem supra de ST e troponina seriada negativa.",
                    raciocinio = "Evolução clínica compatível com dor osteomuscular sem sinais de alarme coronariano agudo após seriação enzimática.",
                    decisao = "Conduta conservadora analgésica, orientação de sinais de alarme por escrito e retorno ambulatorial em 72h.",
                    deidentificationConfirmed = true,
                    createdViaVoice = false
                ),
                ClinicalCaseEntity(
                    projectClinicAnchor = "Clínica Médicos a Postos • Unidade Central",
                    clinicUf = "RJ",
                    clinicSpecialty = "Terapia Intensiva Adulto",
                    caseReference = "CASO-002 • Leito 04 UTI (J.P.M.)",
                    responsibleHuman = "Dr. Rafael Mendonça (CRM-SP 162.814)",
                    declaredPurpose = "Rastreabilidade de ajuste hemodinâmico em plantão diurno",
                    contextScope = "Pós-operatório imediato de trauma abdominal • 6ª hora de UTI",
                    evidencias = "PAM 68 mmHg, lactato arterial caiu de 3.4 para 1.8 mmol/L, diurese 1.1 mL/kg/h após expansão volêmica cristalóide.",
                    raciocinio = "Melhora da perfusão tecidual periférica e clareamento de lactato demonstram resposta adequada à ressuscitação inicial.",
                    decisao = "Desmame gradual de noradrenalina para 0.04 mcg/kg/min e nova gasometria arterial em 4 horas.",
                    deidentificationConfirmed = true,
                    createdViaVoice = true
                ),
                ClinicalCaseEntity(
                    projectClinicAnchor = "Clínica Médicos a Postos • Unidade Central",
                    clinicUf = "RJ",
                    clinicSpecialty = "Auditoria & Regulação Administrativa",
                    caseReference = "CASO-003 • Revisão de Prontuário Setor B",
                    responsibleHuman = "Dr. Lucas Cavalcanti (CRM-SP 191.044)",
                    declaredPurpose = "Auditoria de conformidade de prescrição antimicrobiana",
                    contextScope = "Enfermaria Clínica • Revisão de 2ª Camada TCRIA",
                    evidencias = "",
                    raciocinio = "Solicitação de troca de esquema antibiótico empírico durante turno noturno.",
                    decisao = "Escalonamento imediato para meropenem aguardando revisão da CCIH pela manhã.",
                    deidentificationConfirmed = true,
                    createdViaVoice = false
                ),
                ClinicalCaseEntity(
                    projectClinicAnchor = "Clínica Médicos a Postos • Unidade Central",
                    clinicUf = "RJ",
                    clinicSpecialty = "Triagem & Admissão Digital",
                    caseReference = "CASO-004 • Ficha Bruta Não Saneada",
                    responsibleHuman = "Marcos Vinícius Prado (ADM-CL 0892)",
                    declaredPurpose = "Importação automática de formulário externo para triagem",
                    contextScope = "Recepção Central • Registro bloqueado pelo Gate LGPD",
                    evidencias = "Ficha recebida com CPF 123.456.789-00 e contato paciente.teste@exemplo.com no corpo da evolução.",
                    raciocinio = "Tentativa de indexação direta antes da anonimização dos dados sensíveis.",
                    decisao = "Retido na camada de revisão de artefatos bloqueados até remoção de PII.",
                    deidentificationConfirmed = true,
                    createdViaVoice = false
                )
            )

            return rawCases.map { item ->
                val eval = TcriaGovernanceEngine.evaluateCase(
                    projectClinicAnchor = item.projectClinicAnchor,
                    caseReference = item.caseReference,
                    responsibleHuman = item.responsibleHuman,
                    declaredPurpose = item.declaredPurpose,
                    contextScope = item.contextScope,
                    evidencias = item.evidencias,
                    raciocinio = item.raciocinio,
                    decisao = item.decisao,
                    deidentificationConfirmed = item.deidentificationConfirmed
                )
                item.copy(
                    governanceStatus = eval.overallStatus.name,
                    governanceReport = eval.auditSummary
                )
            }
        }

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

