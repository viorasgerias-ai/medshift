package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.network.CloudRunServerlessClient
import com.example.network.ServerlessReadinessDiagnostic
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch

private val BrandEmerald = Color(0xFF087F67)
private val BrandEmeraldDark = Color(0xFF066350)
private val BrandForest = Color(0xFF14302A)
private val BrandSoftMint = Color(0xFFE7F7F3)

private const val WHATSAPP_EARLY_ACCESS_URL =
    "https://wa.me/5521997875539?text=Quero%20testar%20o%20M%C3%A9dicos%20a%20Postos"

@Composable
fun MarketingPortalScreen(
    onNavigateToGovernance: () -> Unit,
    onNavigateToShifts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRunningDiagnostic by remember { mutableStateOf(false) }
    var diagnostic by remember { mutableStateOf<ServerlessReadinessDiagnostic?>(null) }

    LaunchedEffect(Unit) {
        isRunningDiagnostic = true
        diagnostic = CloudRunServerlessClient.runPublicationReadinessDiagnostic()
        isRunningDiagnostic = false
    }

    val openWhatsApp: () -> Unit = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_EARLY_ACCESS_URL))
        runCatching { context.startActivity(intent) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("marketing_portal_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(BrandSoftMint, Color.White)
                            )
                        )
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = BrandEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = BrandEmeraldDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "MÉDICOS A POSTOS • FIREBASE & CLOUD RUN SERVERLESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = BrandEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Documentação clínica rastreável para decisões mais claras e seguras.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BrandForest,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Transforme atendimentos, plantões e acompanhamentos em registros estruturados com evidências, raciocínio clínico, conduta e redação automática LGPD — integrado ao Firebase Firestore, Firebase AI Logic e backend serverless no Google Cloud Run.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4F6963)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = openWhatsApp,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_marketing_whatsapp")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Testar no WhatsApp", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToGovernance,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_marketing_open_governance")
                        ) {
                            Text("Abrir TCRIA ao Vivo", color = BrandEmeraldDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Cloud Run Serverless & Publication Readiness Card
        item {
            ServerlessReadinessCard(
                diagnostic = diagnostic,
                isRunning = isRunningDiagnostic,
                onRetest = {
                    scope.launch {
                        isRunningDiagnostic = true
                        diagnostic = CloudRunServerlessClient.runPublicationReadinessDiagnostic()
                        isRunningDiagnostic = false
                    }
                }
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = BrandForest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXEMPLO DE REGISTRO RASTREÁVEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = Color(0xFF9EE7D4),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Caso #001 — Atendimento inicial",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    SampleRecordBox(
                        label = "Evidências",
                        content = "Queixa álgica há 48h, exames iniciais anexados e sinais vitais registrados."
                    )
                    SampleRecordBox(
                        label = "Raciocínio clínico",
                        content = "Evolução compatível com quadro subagudo sem sinais de alarme no momento."
                    )
                    SampleRecordBox(
                        label = "Decisão / conduta",
                        content = "Conduta conservadora, orientação registrada e reavaliação programada."
                    )
                    SampleRecordBox(
                        label = "Status de Governança TCRIA & LGPD",
                        content = "Registro íntegro • PII redigido automaticamente antes de salvar no Room/Firestore."
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ARQUITETURA SERVERLESS OTIMIZADA PARA PUBLICAÇÃO",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = BrandEmeraldDark,
                    fontWeight = FontWeight.Bold
                )
                MarketingFeatureCard(
                    title = "1. App Android (Room Offline + Firebase Firestore + AI Logic)",
                    description = "Persistência local imediata com redação automática LGPD (CPF, e-mail, telefone, RG, CNS) antes de salvar no Room e sincronização segura por usuário autenticado."
                )
                MarketingFeatureCard(
                    title = "2. Backend Kotlin/JVM Serverless no Cloud Run (Dockerfile + MedShiftKotlinServer.kt)",
                    description = "Container multi-stage Eclipse Temurin 21 JRE Alpine compilando a lógica Kotlin (LgpdTextRedactor, TcriaGovernanceEngine e ponte Firebase Firestore/Auth) em medshift-kotlin-backend.jar."
                )
                MarketingFeatureCard(
                    title = "3. Script Automatizado de Deploy (./deploy-cloudrun.sh)",
                    description = "Executa gcloud run deploy medshift-tcria-api, publica firestore.rules + Firebase Hosting (public/index.html) e valida o endpoint /api/health."
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrandForest),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloudrun_deploy_commands_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "COMANDO DE DEPLOY SERVERLESS (KOTLIN CLOUD RUN)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = Color(0xFF9EE7D4),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "./deploy-cloudrun.sh --project gen-lang-client-0265308632 --region southamerica-east1",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = Color.White
                    )
                    Text(
                        text = "Inclui: Multi-stage Kotlin 2.1/JDK 21 Dockerfile • /api/health • /api/lgpd/redact • /api/governance/evaluate • Firebase Firestore & Hosting",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }
        }


        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSoftMint),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrandEmerald.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ACESSO INICIAL • PLANO PROFISSIONAL INDIVIDUAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = BrandEmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "R$ 46",
                            style = MaterialTheme.typography.headlineLarge,
                            color = BrandForest,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "/ mês por médico na fase inicial",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4F6963)
                        )
                    }

                    HorizontalDivider(color = BrandEmerald.copy(alpha = 0.2f))

                    Text("• Cadastro da clínica e contexto profissional (RULE_1.1)", style = MaterialTheme.typography.bodySmall, color = BrandForest)
                    Text("• Redação automática LGPD em transcrições de voz e entradas manuais", style = MaterialTheme.typography.bodySmall, color = BrandForest)
                    Text("• Estrutura de evidências, raciocínio, conduta e 5 Gates TCRIA", style = MaterialTheme.typography.bodySmall, color = BrandForest)
                    Text("• Pronto para geração de APK/AAB de publicação e deploy Cloud Run", style = MaterialTheme.typography.bodySmall, color = BrandForest)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = openWhatsApp,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Assinar / Falar no WhatsApp")
                        }
                        OutlinedButton(
                            onClick = onNavigateToShifts,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver Escala de Plantões", color = BrandForest)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerlessReadinessCard(
    diagnostic: ServerlessReadinessDiagnostic?,
    isRunning: Boolean,
    onRetest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandEmerald.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .testTag("serverless_readiness_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Checklist de Publicação: Firebase & Cloud Run",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandForest
                        )
                        Text(
                            text = "Diagnóstico Serverless em Tempo Real",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = BrandEmeraldDark
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRetest,
                    enabled = !isRunning,
                    modifier = Modifier.testTag("btn_retest_serverless")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Verificar")
                    }
                }
            }

            if (diagnostic != null) {
                ReadinessCheckRow(
                    title = "1. Autenticação Firebase & Credential Manager",
                    detail = diagnostic.authSummary
                )
                ReadinessCheckRow(
                    title = "2. Firestore Database & Security Rules",
                    detail = diagnostic.firestoreSummary
                )
                ReadinessCheckRow(
                    title = "3. Redação Automática LGPD + Room Database",
                    detail = diagnostic.lgpdSummary
                )
                ReadinessCheckRow(
                    title = "4. Backend Serverless Cloud Run (medshift-tcria-api)",
                    detail = diagnostic.cloudRunSummary
                )
                ReadinessCheckRow(
                    title = "5. Empacotamento APK / AAB & Firebase Hosting",
                    detail = diagnostic.releaseSummary
                )
            }
        }
    }
}

@Composable
private fun ReadinessCheckRow(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandSoftMint.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BrandEmerald,
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = BrandForest
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF334E48)
            )
        }
    }
}

@Composable
private fun SampleRecordBox(label: String, content: String) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                color = Color(0xFF9EE7D4),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MarketingFeatureCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BrandForest
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
