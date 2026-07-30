package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Koneksi AI tidak siap. Harap konfigurasikan GEMINI_API_KEY di panel Secrets AI Studio Anda."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"

        val root = JSONObject()
        
        // Setup contents array containing the user's actual question/prompt
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        // Setup systemInstruction to strictly bound and instruct the AI model
        val systemInstructionObj = JSONObject()
        val siPartsArray = JSONArray()
        val siPartObj = JSONObject()
        siPartObj.put("text", """
            Kamu adalah AI Barberteak, asisten kecerdasan buatan khusus untuk Barberteak (aplikasi potong rambut modern bertema Luxury Teak & Gold).
            Tugasmu adalah memberikan bantuan, rekomendasi gaya rambut, dan tips perawatan pria secara profesional dan ramah dalam Bahasa Indonesia.

            BATASAN PENTING:
            1. Kamu HANYA boleh menjawab pertanyaan yang berkaitan dengan Barberteak, ketersediaan capster, katalog produk, paket layanan (services), dan konsultasi gaya/perawatan rambut/janggut pria.
            2. Jika pengguna menanyakan hal di luar topik barber (seperti matematika, pemrograman, resep masakan, berita politik, pelajaran sekolah, dll), kamu harus MENOLAK dengan sopan dan ingatkan mereka bahwa kamu adalah asisten khusus Barberteak. Contoh: "Maaf, saya hanya dapat membantu menjawab pertanyaan seputar gaya rambut, layanan, produk, atau capster di Barberteak."
            3. JAWABAN JELAS, TEPAT & SINGKAT: Berikan jawaban yang ramah, sopan, sangat informatif, dan langsung ke inti pertanyaan. Batasi maksimal 2 hingga 3 kalimat saja agar respons sangat cepat dan penjelasannya UTUH (tidak terpotong). Jangan bertele-tele atau menjelaskan terlalu panjang.

            INFORMASI RESMI BARBERTEAK:

            [Daftar Capster (Barber) & Ketersediaan]:
            - Budi (Capster Senior): Pengalaman 8 Tahun, Rating 4.9. Keahlian: Classic Pompadour, Shaving, Hot Towel Massage. Status: Available (Tersedia), mendukung Home Service.
            - Agus (Capster Junior): Pengalaman 3 Tahun, Rating 4.6. Keahlian: Undercut, Fade Cut, Creambath. Status: Available (Tersedia), mendukung Home Service.
            - Rian (Hair Artist): Pengalaman 5 Tahun, Rating 4.8. Keahlian: Korean Hair Design, Hair Coloring, Perming. Status: Available (Tersedia), TIDAK mendukung Home Service.
            - Dendi (Kids Specialist): Pengalaman 4 Tahun, Rating 4.7. Keahlian: Kids Haircut, Flat Top, Beard Trim. Status: Busy, mendukung Home Service.

            [Daftar Produk]:
            - Teak & Clay Pomade Premium: Rp 125.000. Clay alami hold kuat, matte finish, aroma teakwood.
            - Woodland Beard Oil Nourish: Rp 85.000. Minyak brewok argan & jojoba.
            - Royal Teak Hair Tonic Active: Rp 95.000. Tonik ginseng mentol anti-rontok.
            - Carbon Premium Styling Comb: Rp 35.000. Sisir carbon anti-statis.

            [Daftar Layanan & Harga]:
            - Gentleman Classic Haircut: Rp 60.000
            - Premium Cut + Wash + Styling: Rp 90.000
            - Hair Coloring: Rp 120.000
            - Beard Shave & Hot Towel Massage: Rp 45.000
            - Full Package Royal Treatment: Rp 150.000

            Format tanggapanmu dengan rapi menggunakan emoji barber (✂️, 💈, 🪵) secara elegan, berikan jawaban sangat singkat, padat, langsung ke inti, dan super cepat.
        """.trimIndent())
        siPartsArray.put(siPartObj)
        systemInstructionObj.put("parts", siPartsArray)
        root.put("systemInstruction", systemInstructionObj)

        // Konfigurasi performa tinggi untuk kecepatan maksimal dan kepatuhan batasan
        val generationConfig = JSONObject()
        generationConfig.put("temperature", 0.1) // Lebih konsisten, terfokus, dan hemat waktu berpikir
        generationConfig.put("maxOutputTokens", 300) // Dinaikkan ke 300 agar kalimat utuh tidak terpotong, namun tetap sangat hemat token
        root.put("generationConfig", generationConfig)

        val requestBody = root.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (bodyStr == null) {
                    return@withContext "Gagal menghubungi Gemini AI: Tidak ada respon data."
                }
                
                val resJson = JSONObject(bodyStr)
                
                // Cek apakah ada error dari response JSON API
                if (resJson.has("error")) {
                    val errorObj = resJson.getJSONObject("error")
                    val errorMsg = errorObj.optString("message", "Kesalahan tidak dikenal")
                    return@withContext "Error Gemini: $errorMsg (Harap pastikan GEMINI_API_KEY di panel Secrets AI Studio Anda sudah terpasang dengan benar)."
                }

                if (!response.isSuccessful) {
                    return@withContext "Gagal menghubungi Gemini AI: Kode HTTP ${response.code}"
                }

                val candidates = resJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                
                if (candidates == null || candidates.length() == 0) {
                    val promptFeedback = resJson.optJSONObject("promptFeedback")
                    if (promptFeedback != null) {
                        return@withContext "Maaf, pertanyaan Anda tidak dapat diproses oleh sistem keamanan AI Barberteak."
                    }
                    return@withContext "Maaf, AI sedang tidak dapat menjawab saat ini. Silakan coba pertanyaan lain."
                }

                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                firstPart?.optString("text") ?: "Tidak ada respons dari AI."
            }
        } catch (e: Exception) {
            Log.e("GeminiHelper", "Error calling Gemini", e)
            "Error: ${e.localizedMessage ?: "Koneksi terganggu"}"
        }
    }
}
