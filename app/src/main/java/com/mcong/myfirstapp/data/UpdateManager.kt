package com.mcong.myfirstapp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mcong.myfirstapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String
)

interface UpdateApi {
    @GET("version.json")
    suspend fun getVersionInfo(): VersionInfo
}

class UpdateManager(private val context: Context) {
    private val serverUrl = "http://20.55.41.231/" // Replace with your server IP

    private val retrofit = Retrofit.Builder()
        .baseUrl(serverUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val updateApi = retrofit.create(UpdateApi::class.java)

    suspend fun getLatestVersion(): VersionInfo? {
        return try {
            withContext(Dispatchers.IO) {
                updateApi.getVersionInfo()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAndInstall(
        versionInfo: VersionInfo,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = "app_v${versionInfo.versionCode}.apk"
                val file = File(context.getExternalFilesDir(null), fileName)

                if (file.exists()) {
                    // File already exists, skip download
                    withContext(Dispatchers.Main) {
                        onProgress(1.0f)
                        onComplete()
                        installApk(file)
                    }
                    return@withContext
                }

                val client = OkHttpClient()
                val request = Request.Builder().url(versionInfo.url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) { onError("Download failed: ${response.code()}") }
                    return@withContext
                }

                val body = response.body() ?: run {
                    withContext(Dispatchers.Main) { onError("Empty response body") }
                    return@withContext
                }

                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    onComplete()
                    installApk(file)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError("Error: ${e.message}") }
            }
        }
    }

    private fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
