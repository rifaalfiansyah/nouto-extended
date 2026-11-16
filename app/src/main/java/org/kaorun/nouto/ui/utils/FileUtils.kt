package org.kaorun.nouto.ui.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object FileUtils {
    suspend fun writeNoteToFile(
        context: Context,
        uri: Uri,
        title: String?,
        content: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw NullPointerException()

            outputStream.use { stream ->
                stream.bufferedWriter().use { writer ->
                    val jsonObject = JSONObject().apply {
                        put("title", title ?: "")
                        put("content", content ?: "")
                    }
                    writer.write(jsonObject.toString())
                }
            }
        }
    }

    suspend fun readFromFile(
        context: Context,
        uri: Uri
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw NullPointerException()

            inputStream.use { stream ->
                stream.bufferedReader().use { reader ->
                    val jsonString = reader.readText()
                    val jsonObject = JSONObject(jsonString)

                    val title = jsonObject.optString("title", "")
                    val content = jsonObject.optString("content", "")

                    Pair(title, content)
                }
            }
        }
    }
}