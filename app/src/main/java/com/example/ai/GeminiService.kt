package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class GeminiMediaResult(
    val filePath: String,
    val title: String,
    val description: String,
    val isMap: Boolean,
    val promptUsed: String,
    val isGeneratedViaApi: Boolean
)

class GeminiService(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Generates a tactical map or artistic visual scene using Gemini (gemini-2.5-flash-image)
     * with resilient on-device cartographic fallback.
     */
    suspend fun generateMedia(
        rawPrompt: String,
        isMap: Boolean,
        currentLocation: String? = null,
        worldGenre: String? = null
    ): GeminiMediaResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val enrichedPrompt = if (isMap) {
            val loc = currentLocation ?: "Zona actual"
            "Top-down tabletop RPG tactical battle map grid of $loc. $rawPrompt. Overhead bird's-eye view cartography, detailed architectural layout, walls, rooms, corridors, tactical grid, high fantasy / sci-fi atmospheric lighting, crisp clean cartography."
        } else {
            "Cinematic concept art illustration for roleplay story. $rawPrompt. Highly detailed, atmospheric lighting, evocative digital painting, master quality, dramatic composition."
        }

        // Try calling Gemini API if a valid key is provided
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val apiResult = callGeminiImageApi(apiKey, enrichedPrompt, isMap)
                if (apiResult != null) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                // If API quota or network error, proceed to fallback gracefully
            }
        }

        // Intelligent local tactical cartography / art generator fallback
        generateLocalTacticalFallback(rawPrompt, isMap, currentLocation, worldGenre)
    }

    private fun callGeminiImageApi(
        apiKey: String,
        prompt: String,
        isMap: Boolean
    ): GeminiMediaResult? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", "1:1")
                    put("imageSize", "1K")
                })
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null

        var imageBase64: String? = null
        var companionText = ""

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("inlineData")) {
                val inline = part.getJSONObject("inlineData")
                imageBase64 = inline.optString("data")
            } else if (part.has("text")) {
                companionText += part.optString("text") + " "
            }
        }

        if (imageBase64.isNullOrBlank()) return null

        val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
        val dir = File(context.filesDir, "gemini_creations").apply { mkdirs() }
        val filename = "gemini_${if (isMap) "map" else "img"}_${System.currentTimeMillis()}.png"
        val file = File(dir, filename)
        file.writeBytes(bytes)

        val title = if (isMap) "Mapa Táctico de Exploración" else "Ilustración de la Escena"
        val desc = companionText.trim().ifBlank {
            if (isMap) "Mapa detallado generado por Gemini para la aventura actual."
            else "Visualización de la escena generada por Gemini para la historia."
        }

        return GeminiMediaResult(
            filePath = file.absolutePath,
            title = title,
            description = desc,
            isMap = isMap,
            promptUsed = prompt,
            isGeneratedViaApi = true
        )
    }

    private fun generateLocalTacticalFallback(
        prompt: String,
        isMap: Boolean,
        location: String?,
        genre: String?
    ): GeminiMediaResult {
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (isMap) {
            drawTacticalMap(canvas, width, height, location ?: "Zona de Aventura", prompt, genre)
        } else {
            drawAtmosphericScene(canvas, width, height, prompt, genre)
        }

        val dir = File(context.filesDir, "gemini_creations").apply { mkdirs() }
        val filename = "gemini_${if (isMap) "map" else "art"}_${System.currentTimeMillis()}.png"
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val title = if (isMap) "Mapa Táctico: ${location ?: "Territorio Inexplorado"}" else "Ilustración de la Escena"
        val desc = if (isMap) {
            "Plano táctico con cuadrícula de combate, zonas clave y accesos estratégicos generados para: \"$prompt\"."
        } else {
            "Escena visual ambientada para la situación actual: \"$prompt\"."
        }

        return GeminiMediaResult(
            filePath = file.absolutePath,
            title = title,
            description = desc,
            isMap = isMap,
            promptUsed = prompt,
            isGeneratedViaApi = false
        )
    }

    private fun drawTacticalMap(
        canvas: Canvas,
        width: Int,
        height: Int,
        locationName: String,
        prompt: String,
        genre: String?
    ) {
        val isSciFi = genre?.contains("Espacial", ignoreCase = true) == true ||
                genre?.contains("Cyber", ignoreCase = true) == true

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        val bgColor = if (isSciFi) Color.rgb(18, 24, 38) else Color.rgb(38, 30, 22)
        canvas.drawColor(bgColor)

        // Grid lines
        paint.color = if (isSciFi) Color.argb(45, 56, 189, 248) else Color.argb(40, 217, 119, 6)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE

        val cellSize = 50f
        var x = 50f
        while (x < width - 50f) {
            canvas.drawLine(x, 50f, x, height - 70f, paint)
            x += cellSize
        }
        var y = 50f
        while (y < height - 70f) {
            canvas.drawLine(50f, y, width - 50f, y, paint)
            y += cellSize
        }

        // Rooms & Structures
        paint.style = Paint.Style.FILL
        val roomFill = if (isSciFi) Color.argb(120, 15, 23, 42) else Color.argb(140, 60, 48, 36)
        paint.color = roomFill

        // Central Chamber
        val mainRoom = RectF(200f, 150f, 600f, 450f)
        canvas.drawRoundRect(mainRoom, 12f, 12f, paint)

        // Secondary Rooms
        val roomWest = RectF(80f, 220f, 200f, 380f)
        val roomSouth = RectF(260f, 450f, 540f, 650f)
        val roomEast = RectF(600f, 240f, 720f, 400f)
        canvas.drawRoundRect(roomWest, 8f, 8f, paint)
        canvas.drawRoundRect(roomSouth, 8f, 8f, paint)
        canvas.drawRoundRect(roomEast, 8f, 8f, paint)

        // Corridors & Walls
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = if (isSciFi) Color.rgb(56, 189, 248) else Color.rgb(217, 119, 6)
        canvas.drawRoundRect(mainRoom, 12f, 12f, paint)
        canvas.drawRoundRect(roomWest, 8f, 8f, paint)
        canvas.drawRoundRect(roomSouth, 8f, 8f, paint)
        canvas.drawRoundRect(roomEast, 8f, 8f, paint)

        // Doors & Access Points
        paint.color = Color.rgb(239, 68, 68)
        paint.strokeWidth = 6f
        canvas.drawLine(195f, 280f, 205f, 320f, paint) // Door West
        canvas.drawLine(380f, 445f, 420f, 455f, paint) // Door South
        canvas.drawLine(595f, 300f, 605f, 340f, paint) // Door East

        // POI Markers
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(234, 179, 8) // Gold marker
        canvas.drawCircle(400f, 300f, 14f, paint) // Main objective / altar / console
        paint.color = Color.rgb(16, 185, 129) // Spawn marker
        canvas.drawCircle(400f, 600f, 12f, paint)

        // Coordinate Labels
        paint.textSize = 14f
        paint.color = Color.argb(160, 200, 200, 200)
        var colIndex = 1
        var cx = 75f
        while (cx < width - 50f) {
            canvas.drawText("$colIndex", cx - 4f, 40f, paint)
            colIndex++
            cx += cellSize
        }
        val letters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M")
        var rowIndex = 0
        var ry = 75f
        while (ry < height - 70f && rowIndex < letters.size) {
            canvas.drawText(letters[rowIndex], 25f, ry + 5f, paint)
            rowIndex++
            ry += cellSize
        }

        // Header Title Bar
        paint.color = Color.argb(220, 20, 20, 24)
        canvas.drawRect(50f, height - 60f, width - 50f, height - 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = if (isSciFi) Color.rgb(56, 189, 248) else Color.rgb(217, 119, 6)
        canvas.drawRect(50f, height - 60f, width - 50f, height - 10f, paint)

        // Title text
        paint.style = Paint.Style.FILL
        paint.textSize = 20f
        paint.color = Color.WHITE
        canvas.drawText("🗺️ $locationName", 70f, height - 26f, paint)

        // Legend
        paint.textSize = 13f
        paint.color = Color.rgb(16, 185, 129)
        canvas.drawText("🟢 Inicio", width - 260f, height - 26f, paint)
        paint.color = Color.rgb(234, 179, 8)
        canvas.drawText("🟡 Objetivo", width - 180f, height - 26f, paint)
        paint.color = Color.rgb(239, 68, 68)
        canvas.drawText("🔴 Puerta", width - 90f, height - 26f, paint)
    }

    private fun drawAtmosphericScene(
        canvas: Canvas,
        width: Int,
        height: Int,
        prompt: String,
        genre: String?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Gradient layers
        val isSciFi = genre?.contains("Espacial", ignoreCase = true) == true ||
                genre?.contains("Cyber", ignoreCase = true) == true

        val topColor = if (isSciFi) Color.rgb(15, 23, 42) else Color.rgb(24, 18, 36)
        val bottomColor = if (isSciFi) Color.rgb(30, 41, 59) else Color.rgb(44, 28, 48)

        // Sky / Horizon gradient
        val steps = 40
        for (i in 0 until steps) {
            val ratio = i.toFloat() / steps
            val r = (Color.red(topColor) * (1 - ratio) + Color.red(bottomColor) * ratio).toInt()
            val g = (Color.green(topColor) * (1 - ratio) + Color.green(bottomColor) * ratio).toInt()
            val b = (Color.blue(topColor) * (1 - ratio) + Color.blue(bottomColor) * ratio).toInt()
            paint.color = Color.rgb(r, g, b)
            canvas.drawRect(0f, i * (height.toFloat() / steps), width.toFloat(), (i + 1) * (height.toFloat() / steps), paint)
        }

        // Stars / Embers
        paint.color = Color.argb(180, 255, 255, 255)
        val rng = Random(42)
        for (i in 0 until 90) {
            val sx = rng.nextFloat() * width
            val sy = rng.nextFloat() * (height * 0.6f)
            val size = rng.nextFloat() * 3f + 1f
            canvas.drawCircle(sx, sy, size, paint)
        }

        // Celestial moon or holo-sun
        paint.color = if (isSciFi) Color.argb(190, 56, 189, 248) else Color.argb(200, 245, 158, 11)
        canvas.drawCircle(width * 0.75f, height * 0.28f, 75f, paint)

        // Mountain or city silhouette
        paint.color = Color.rgb(10, 10, 14)
        val path = android.graphics.Path()
        path.moveTo(0f, height * 0.65f)
        path.lineTo(width * 0.25f, height * 0.45f)
        path.lineTo(width * 0.5f, height * 0.58f)
        path.lineTo(width * 0.75f, height * 0.40f)
        path.lineTo(width.toFloat(), height * 0.62f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        canvas.drawPath(path, paint)

        // Foreground subtle banner with description
        paint.color = Color.argb(220, 15, 15, 18)
        canvas.drawRoundRect(RectF(40f, height - 100f, width - 40f, height - 20f), 16f, 16f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        val displayPrompt = if (prompt.length > 55) prompt.take(52) + "..." else prompt
        canvas.drawText("🎨 \"$displayPrompt\"", 60f, height - 54f, paint)
    }
}
