package com.example.policemobiledirectory.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.nudi.SankaEngine
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

enum class ConverterMode {
    ASCII_TO_UNICODE,
    UNICODE_TO_ASCII
}

data class NudiState(
    val inputText: String = "",
    val outputText: String = "",
    val fileName: String? = null,
    val mode: ConverterMode = ConverterMode.ASCII_TO_UNICODE,
    val removeSpace: Boolean = true
)

class NudiConverterViewModel(app: Application) : AndroidViewModel(app) {

    private val sanka = SankaEngine(app.applicationContext)

    private val _uiState = MutableStateFlow(NudiState())
    val uiState = _uiState.asStateFlow()

    fun updateInput(text: String) {
        convert(text, _uiState.value.mode, _uiState.value.removeSpace)
    }

    fun updateOutput(text: String) {
        _uiState.value = _uiState.value.copy(outputText = text)
    }

    fun toggleMode(mode: ConverterMode) {
        convert(_uiState.value.inputText, mode, _uiState.value.removeSpace)
    }

    fun toggleSpace() {
        val cur = _uiState.value
        convert(cur.inputText, cur.mode, !cur.removeSpace)
    }

    fun attachContext(context: Context) {
        sanka.attachContext(context)
    }

    fun reset() {
        _uiState.value = NudiState()
    }

    private fun convert(raw: String, mode: ConverterMode, removeSpace: Boolean) {

        val processed = if (removeSpace)
            raw.replace(Regex("[ \\t]+"), " ").trim()
        else raw

        _uiState.value = _uiState.value.copy(
            inputText = raw,
            mode = mode,
            removeSpace = removeSpace
        )

        if (processed.isBlank()) {
            _uiState.value = _uiState.value.copy(outputText = "")
            return
        }

        val callback = object : SankaEngine.Callback {
            override fun onResult(result: String) {
                _uiState.value = _uiState.value.copy(outputText = result)
            }

            override fun onError(error: String) {
                _uiState.value = _uiState.value.copy(outputText = "Error: $error")
            }
        }

        if (mode == ConverterMode.ASCII_TO_UNICODE) {
            sanka.asciiToUnicode(processed,callback)
        } else {
            sanka.unicodeToAscii(processed,  callback)
        }
    }

    fun handleFile(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch(Dispatchers.IO) {

            val ctx = getApplication<Application>().applicationContext
            val resolver = ctx.contentResolver

            val text = try {
                val mime = resolver.getType(uri)
                resolver.openInputStream(uri)?.use { stream ->
                    when {
                        mime == "text/plain" ||
                                uri.toString().endsWith(".txt", ignoreCase = true) -> readTxt(stream)

                        mime == "application/pdf" ||
                                uri.toString().endsWith(".pdf", ignoreCase = true) -> readPdf(stream)

                        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                                uri.toString().endsWith(".docx", ignoreCase = true) -> readDocx(stream)

                        else -> readTxt(stream)
                    }
                } ?: ""
            } catch (e: Exception) {
                "File read error: ${e.message}"
            }

            withContext(Dispatchers.Main) {
                val current = _uiState.value
                _uiState.value = current.copy(
                    fileName = uri.lastPathSegment ?: "File",
                    mode = ConverterMode.ASCII_TO_UNICODE
                )
                updateInput(text)
            }
        }
    }
}

/* -------- FILE READERS ---------- */

private fun readTxt(input: InputStream): String =
    BufferedReader(InputStreamReader(input)).use { it.readText() }

private fun readDocx(input: InputStream): String = try {
    val doc = XWPFDocument(input)
    val text = doc.paragraphs.joinToString("\n") { it.text }
    doc.close()
    text
} catch (e: Exception) {
    "DOCX read error: ${e.message}"
}

private fun readPdf(input: InputStream): String = try {
    val pdf = PDDocument.load(input)
    val strip = PDFTextStripper()
    val t = strip.getText(pdf)
    pdf.close()
    t.trim()
} catch (e: Exception) {
    "PDF read error: ${e.message}"
}
