package com.toolbox.util

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

object FileUtil {

    fun readTextFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        }
    }

    fun copyFile(context: Context, sourceUri: Uri, destUri: Uri) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun exportNoteToMarkdown(title: String, content: String): String {
        return "# $title\n\n$content"
    }

    fun exportNoteToTxt(title: String, content: String): String {
        return "$title\n${"=".repeat(title.length)}\n\n$content"
    }
}
