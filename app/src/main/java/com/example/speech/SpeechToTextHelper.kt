package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.governance.LgpdTextRedactor
import com.example.ui.ParsedVoiceShift
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "SpeechToTextHelper"

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data object Listening : SpeechRecognitionState
    data class PartialResult(val partialText: String) : SpeechRecognitionState
    data class FinalResult(val transcript: String) : SpeechRecognitionState
    data class Error(val message: String) : SpeechRecognitionState
}

/**
 * Encapsulates Android SpeechRecognizer boilerplate, automatic LGPD PII redaction,
 * and Firebase AI Logic (Gemini) structured voice command extraction.
 */
class SpeechToTextManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _recognitionState =
        MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageTag: String = Locale.getDefault().toLanguageTag()) {
        if (!isAvailable) {
            _recognitionState.value = SpeechRecognitionState.Error(
                "Reconhecimento de voz nativo indisponível neste dispositivo. Use um comando de voz rápido ou digite a frase."
            )
            return
        }

        stopListening()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _recognitionState.value = SpeechRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                _recognitionState.value = SpeechRecognitionState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorMessage = mapSpeechError(error)
                Log.w(TAG, "SpeechRecognizer error code=$error: $errorMessage")
                _recognitionState.value = SpeechRecognitionState.Error(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topTranscript = matches?.firstOrNull()?.trim().orEmpty()
                if (topTranscript.isNotEmpty()) {
                    val sanitizedTranscript = LgpdTextRedactor.redact(topTranscript)
                    _recognitionState.value = SpeechRecognitionState.FinalResult(sanitizedTranscript)
                } else {
                    _recognitionState.value =
                        SpeechRecognitionState.Error("Nenhuma fala reconhecida. Tente novamente.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = partialMatches?.firstOrNull()?.trim().orEmpty()
                if (partial.isNotEmpty()) {
                    val sanitizedPartial = LgpdTextRedactor.redact(partial)
                    _recognitionState.value = SpeechRecognitionState.PartialResult(sanitizedPartial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer.startListening(intent)
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    fun resetState() {
        _recognitionState.value = SpeechRecognitionState.Idle
    }

    private fun mapSpeechError(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Erro na captura de áudio do microfone."
        SpeechRecognizer.ERROR_CLIENT -> "Falha no cliente de reconhecimento de voz."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária (RECORD_AUDIO)."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Sem conexão para reconhecimento em nuvem; use os comandos de voz rápidos abaixo."
        SpeechRecognizer.ERROR_NO_MATCH -> "Não foi possível compreender o comando falado."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O serviço de voz já está ocupado."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala detectada no microfone."
        else -> "Áudio não detectado. Selecione um comando de voz abaixo ou edite manualmente."
    }
}

/**
 * Uses Firebase AI Logic (Gemini 3.8 Flash) with structured JSON schema output
 * to parse spoken Speech-to-Text transcripts into structured shift records after LGPD PII redaction.
 */
class FirebaseVoiceAiExtractor {

    private val shiftSchema = Schema.obj(
        properties = mapOf(
            "staffName" to Schema.string(description = "Full name of the doctor or staff member with title (e.g., Dra. Mariana Costa)"),
            "roleTitle" to Schema.string(description = "Medical specialty or administrative role"),
            "crmOrRegistry" to Schema.string(description = "CRM or administrative ID code"),
            "department" to Schema.string(description = "Hospital department or unit name"),
            "category" to Schema.enumeration(
                values = listOf("MEDICAL", "ADMIN"),
                description = "MEDICAL for clinical staff, ADMIN for administrative staff"
            ),
            "shiftStatus" to Schema.enumeration(
                values = listOf("CURRENT", "UPCOMING"),
                description = "CURRENT if shift is active now, UPCOMING if scheduled for later/night/tomorrow"
            ),
            "shiftTimeLabel" to Schema.string(description = "Shift time range and duration, e.g. 07:00 – 19:00 (12h)"),
            "dateLabel" to Schema.string(description = "Date and turn label in Portuguese"),
            "locationRoom" to Schema.string(description = "Hospital wing, room, or bed numbers"),
            "permissionLevel" to Schema.string(description = "Access permission role in Portuguese"),
            "notes" to Schema.string(description = "Summary note in Portuguese"),
            "isOnCall" to Schema.boolean(description = "True if the command mentions sobreaviso or on-call")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = shiftSchema
            }
        )
    }

    suspend fun extractShiftFromSpeech(transcript: String): Result<ParsedVoiceShift> {
        return try {
            val sanitizedTranscript = LgpdTextRedactor.redact(transcript)
            val prompt = """
                Extraia os dados estruturados de um plantão médico ou administrativo a partir deste comando de voz (Speech-to-Text já saneado pela LGPD):
                "$sanitizedTranscript"
                Preencha campos ausentes com valores clínicos brasileiros realistas em português. Preserve marcadores [CPF REDIGIDO - LGPD] ou [EMAIL REDIGIDO - LGPD] caso apareçam nas notas.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Modelo Gemini retornou resposta vazia.")

            val json = JSONObject(jsonString)
            val parsed = LgpdTextRedactor.sanitizeParsedVoiceShift(
                ParsedVoiceShift(
                    staffName = json.optString("staffName").ifBlank { "Dr. Eduardo Santoro" },
                    roleTitle = json.optString("roleTitle").ifBlank { "Médico Plantonista" },
                    crmOrRegistry = json.optString("crmOrRegistry").ifBlank { "CRM-SP 186.420" },
                    department = json.optString("department").ifBlank { "UTI Adulto" },
                    category = json.optString("category", "MEDICAL"),
                    shiftStatus = json.optString("shiftStatus", "CURRENT"),
                    shiftTimeLabel = json.optString("shiftTimeLabel").ifBlank { "07:00 – 19:00 (12h)" },
                    dateLabel = json.optString("dateLabel").ifBlank { "Hoje • Criado por Voz" },
                    locationRoom = json.optString("locationRoom").ifBlank { "Ala Central • Leitos 01–10" },
                    permissionLevel = json.optString("permissionLevel").ifBlank { "Acesso Clínico Plantonista" },
                    notes = json.optString("notes").ifBlank { "Comando de voz: \"$sanitizedTranscript\"" },
                    isOnCall = json.optBoolean("isOnCall", false)
                )
            )
            Result.success(parsed)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini voice extraction timed out", e)
            Result.failure(Exception("Tempo limite excedido ao processar comando de voz com IA."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI voice extraction failed", e)
            Result.failure(e)
        }
    }
}

