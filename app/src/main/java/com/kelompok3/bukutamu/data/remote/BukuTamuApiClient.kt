package com.kelompok3.bukutamu.data.remote

import com.kelompok3.bukutamu.data.entity.Guest
import com.kelompok3.bukutamu.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pembungkus hasil pemanggilan HTTP ke Backend PHP API (NIM 187).
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data_187: T) : ApiResult<T>()
    data class HttpError(val code_187: Int, val message_187: String) : ApiResult<Nothing>()
    data class NetworkError(val message_187: String) : ApiResult<Nothing>()
}

/**
 * Kontrak layanan REST API Buku Tamu Digital (NIM 187).
 */
interface BukuTamuApiService {
    suspend fun register(username_187: String, password_187: String): ApiResult<User>
    suspend fun login(username_187: String, password_187: String): ApiResult<User>
    suspend fun getGuests(userId_187: Int, query_187: String = ""): ApiResult<List<Guest>>
    suspend fun createGuest(userId_187: Int, nama_187: String, alamat_187: String, keperluan_187: String): ApiResult<Guest>
    suspend fun updateGuest(guestId_187: Int, userId_187: Int, nama_187: String, alamat_187: String, keperluan_187: String): ApiResult<Guest>
    suspend fun deleteGuest(guestId_187: Int, userId_187: Int): ApiResult<Boolean>
}

/**
 * Implementasi klien HTTP ringan menggunakan HttpURLConnection + org.json bawaan Android SDK (NIM 187).
 * Seluruh pemanggilan jaringan dijalankan pada Dispatchers.IO.
 */
class BukuTamuApiClient(
    private val baseUrl_187: String = ApiConfig.BASE_URL_187
) : BukuTamuApiService {

    override suspend fun register(username_187: String, password_187: String): ApiResult<User> {
        val body_187 = JSONObject().apply {
            put("username", username_187)
            put("password", password_187)
        }
        return executeJsonRequest_187(
            endpoint_187 = "register.php",
            method_187 = "POST",
            jsonBody_187 = body_187
        ) { json_187 ->
            val userObj_187 = json_187.getJSONObject("user")
            User(
                id = userObj_187.getInt("id"),
                username = userObj_187.getString("username"),
                password = ""
            )
        }
    }

    override suspend fun login(username_187: String, password_187: String): ApiResult<User> {
        val body_187 = JSONObject().apply {
            put("username", username_187)
            put("password", password_187)
        }
        return executeJsonRequest_187(
            endpoint_187 = "login.php",
            method_187 = "POST",
            jsonBody_187 = body_187
        ) { json_187 ->
            val userObj_187 = json_187.getJSONObject("user")
            User(
                id = userObj_187.getInt("id"),
                username = userObj_187.getString("username"),
                password = ""
            )
        }
    }

    override suspend fun getGuests(userId_187: Int, query_187: String): ApiResult<List<Guest>> {
        val cleanQuery_187 = query_187.trim()
        val endpoint_187 = if (cleanQuery_187.isEmpty()) {
            "guests.php?user_id=$userId_187"
        } else {
            val encoded_187 = URLEncoder.encode(cleanQuery_187, "UTF-8")
            "guests.php?user_id=$userId_187&q=$encoded_187"
        }
        return executeJsonRequest_187(
            endpoint_187 = endpoint_187,
            method_187 = "GET"
        ) { json_187 ->
            val array_187 = json_187.optJSONArray("guests") ?: JSONArray()
            buildList {
                for (i_187 in 0 until array_187.length()) {
                    add(parseGuest_187(array_187.getJSONObject(i_187)))
                }
            }
        }
    }

    override suspend fun createGuest(
        userId_187: Int,
        nama_187: String,
        alamat_187: String,
        keperluan_187: String
    ): ApiResult<Guest> {
        val body_187 = JSONObject().apply {
            put("user_id", userId_187)
            put("nama", nama_187)
            put("alamat", alamat_187)
            put("keperluan", keperluan_187)
        }
        return executeJsonRequest_187(
            endpoint_187 = "guests.php",
            method_187 = "POST",
            jsonBody_187 = body_187
        ) { json_187 ->
            parseGuest_187(json_187.getJSONObject("guest"))
        }
    }

    override suspend fun updateGuest(
        guestId_187: Int,
        userId_187: Int,
        nama_187: String,
        alamat_187: String,
        keperluan_187: String
    ): ApiResult<Guest> {
        val body_187 = JSONObject().apply {
            put("id", guestId_187)
            put("user_id", userId_187)
            put("nama", nama_187)
            put("alamat", alamat_187)
            put("keperluan", keperluan_187)
        }
        return executeJsonRequest_187(
            endpoint_187 = "guests.php",
            method_187 = "PUT",
            jsonBody_187 = body_187
        ) { json_187 ->
            parseGuest_187(json_187.getJSONObject("guest"))
        }
    }

    override suspend fun deleteGuest(guestId_187: Int, userId_187: Int): ApiResult<Boolean> {
        return executeJsonRequest_187(
            endpoint_187 = "guests.php?id=$guestId_187&user_id=$userId_187",
            method_187 = "DELETE"
        ) { json_187 ->
            json_187.optBoolean("success", true)
        }
    }

    private fun parseGuest_187(obj_187: JSONObject): Guest {
        return Guest(
            id = obj_187.getInt("id"),
            userId = obj_187.getInt("user_id"),
            nama = obj_187.getString("nama"),
            alamat = obj_187.getString("alamat"),
            keperluan = obj_187.getString("keperluan"),
            timestamp = obj_187.getLong("timestamp")
        )
    }

    private suspend fun <T> executeJsonRequest_187(
        endpoint_187: String,
        method_187: String,
        jsonBody_187: JSONObject? = null,
        parser_187: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        var connection_187: HttpURLConnection? = null
        try {
            val normalizedBase_187 = if (baseUrl_187.endsWith("/")) baseUrl_187 else "$baseUrl_187/"
            val url_187 = URL(normalizedBase_187 + endpoint_187.removePrefix("/"))
            connection_187 = (url_187.openConnection() as HttpURLConnection).apply {
                requestMethod = method_187
                connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS_187
                readTimeout = ApiConfig.READ_TIMEOUT_MS_187
                setRequestProperty("Accept", "application/json")
                if (jsonBody_187 != null) {
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                }
            }

            if (jsonBody_187 != null) {
                OutputStreamWriter(connection_187.outputStream, Charsets.UTF_8).use { writer_187 ->
                    writer_187.write(jsonBody_187.toString())
                    writer_187.flush()
                }
            }

            val statusCode_187 = connection_187.responseCode
            val stream_187 = if (statusCode_187 in 200..299) {
                connection_187.inputStream
            } else {
                connection_187.errorStream
            }

            val responseText_187 = if (stream_187 != null) {
                BufferedReader(InputStreamReader(stream_187, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }

            val json_187 = if (responseText_187.isNotBlank()) JSONObject(responseText_187) else JSONObject()
            if (statusCode_187 in 200..299 && json_187.optBoolean("success", true)) {
                ApiResult.Success(parser_187(json_187))
            } else {
                val message_187 = json_187.optString("message").ifBlank {
                    "Permintaan gagal dengan kode HTTP $statusCode_187"
                }
                ApiResult.HttpError(statusCode_187, message_187)
            }
        } catch (e_187: Exception) {
            ApiResult.NetworkError(
                "Tidak dapat terhubung ke server PHP ($baseUrl_187). Pastikan server aktif."
            )
        } finally {
            connection_187?.disconnect()
        }
    }
}
