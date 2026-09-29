package com.example.vitamin

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AiHelper {

    suspend fun callTextApi(sessionManager: SessionManager, promptText: String): String? = withContext(Dispatchers.IO) {
        val provider = sessionManager.getProvider()
        val apiKey = sessionManager.getApiKey()
        val model = sessionManager.getModel()

        if (apiKey.isEmpty()) return@withContext null

        try {
            if (provider == "OpenRouter") {
                val url = URL("https://openrouter.ai/api/v1/chat/completions")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("HTTP-Referer", "https://vitamin.app")
                conn.setRequestProperty("X-Title", "VITaMIN")
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.doOutput = true

                val root = JSONObject().apply {
                    put("model", model)
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", promptText)
                        })
                    }
                    put("messages", messages)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(root.toString())
                writer.flush()
                writer.close()

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val choices = responseJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val messageObj = choices.getJSONObject(0).optJSONObject("message")
                        return@withContext messageObj?.optString("content", "")?.trim()
                    }
                } else {
                    val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("AiHelperError", "OpenRouter Code: ${conn.responseCode}, Error: $errText")
                }
            } else {
                // Google Gemini
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.doOutput = true

                val root = JSONObject()
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", promptText) })
                contentObj.put("parts", parts)
                contents.put(contentObj)
                root.put("contents", contents)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(root.toString())
                writer.flush()
                writer.close()

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val respParts = content?.optJSONArray("parts")
                        if (respParts != null && respParts.length() > 0) {
                            return@withContext respParts.getJSONObject(0).optString("text", "").trim()
                        }
                    }
                } else {
                    val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("AiHelperError", "Gemini Code: ${conn.responseCode}, Error: $errText")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun callVisionApi(sessionManager: SessionManager, bitmap: Bitmap): JSONObject? = withContext(Dispatchers.IO) {
        val provider = sessionManager.getProvider()
        val apiKey = sessionManager.getApiKey()
        val model = sessionManager.getModel()

        if (apiKey.isEmpty()) return@withContext null

        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            val promptText = "Sebutkan nama makanan/minuman ini dan berikan estimasi kandungan total kalorinya. Wajib mengembalikan respon hanya berupa data format JSON murni yang valid tanpa backticks markdown, dengan format seperti berikut: {\"food\": \"Nama Makanan\", \"calories\": 350}"

            if (provider == "OpenRouter") {
                val url = URL("https://openrouter.ai/api/v1/chat/completions")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("HTTP-Referer", "https://vitamin.app")
                conn.setRequestProperty("X-Title", "VITaMIN")
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.doOutput = true

                val contentArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", promptText)
                    })
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Image")
                        })
                    })
                }

                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    })
                }

                val root = JSONObject().apply {
                    put("model", model)
                    put("messages", messages)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(root.toString())
                writer.flush()
                writer.close()

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val choices = responseJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val messageObj = choices.getJSONObject(0).optJSONObject("message")
                        var aiText = messageObj?.optString("content", "")?.trim() ?: ""
                        if (aiText.startsWith("```")) {
                            aiText = aiText.replace("```json", "").replace("```", "").trim()
                        }
                        return@withContext JSONObject(aiText)
                    }
                } else {
                    val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("AiHelperError", "OpenRouter Code: ${conn.responseCode}, Error: $errText")
                }
            } else {
                // Google Gemini
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.doOutput = true

                val root = JSONObject()
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()

                parts.put(JSONObject().apply { put("text", promptText) })
                parts.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    })
                })

                contentObj.put("parts", parts)
                contents.put(contentObj)
                root.put("contents", contents)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(root.toString())
                writer.flush()
                writer.close()

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        if (content != null) {
                            val respParts = content.optJSONArray("parts")
                            if (respParts != null && respParts.length() > 0) {
                                var aiTextText = respParts.getJSONObject(0).optString("text", "").trim()
                                if (aiTextText.startsWith("```")) {
                                    aiTextText = aiTextText.replace("```json", "").replace("```", "").trim()
                                }
                                return@withContext JSONObject(aiTextText)
                            }
                        }
                    }
                } else {
                    val errText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("AiHelperError", "Gemini Code: ${conn.responseCode}, Error: $errText")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
