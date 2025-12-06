package com.example.policemobiledirectory.nudi

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class SankaEngine(context: Context) {

    interface Callback {
        fun onResult(result: String)
        fun onError(error: String)
    }

    private var callback: Callback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webContext = MutableContextWrapper(context.applicationContext)

    private data class PendingCall(
        val func: String,
        val text: String,
        val callback: Callback
    )

    private var pendingCall: PendingCall? = null
    @Volatile
    private var isPageLoaded = false

    fun attachContext(context: Context) {
        webContext.baseContext = context
    }

    @SuppressLint("SetJavaScriptEnabled")
    private val webView: WebView = WebView(webContext).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                flushPending()
            }
        }
        addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        // ⭐ IMPORTANT: Load index.html from /assets/sanka/
        loadUrl("file:///android_asset/sanka/index.html")
    }

    // Bridge between JS → Android
    private inner class AndroidBridge {

        @JavascriptInterface
        fun onConverted(text: String) {
            callback?.onResult(text)
        }

        @JavascriptInterface
        fun onError(msg: String) {
            callback?.onError(msg)
        }
    }

    @Synchronized
    private fun flushPending() {
        if (!isPageLoaded) return
        val call = pendingCall ?: return
        pendingCall = null
        executeJsInternal(call.func, call.text, call.callback)
    }

    private fun executeJS(func: String, text: String, cb: Callback) {
        synchronized(this) {
            if (!isPageLoaded) {
                pendingCall = PendingCall(func, text, cb)
                return
            }
        }
        executeJsInternal(func, text, cb)
    }

    private fun executeJsInternal(func: String, text: String, cb: Callback) {
        callback = cb

        val escaped = text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

        val js = "javascript:$func('$escaped');"

        mainHandler.post { webView.evaluateJavascript(js, null) }
    }

    fun asciiToUnicode(text: String, cb: Callback) {
        if (text.isBlank()) {
            cb.onResult("")
            return
        }
        executeJS("convertAsciiToUnicode", text, cb)
    }

    fun unicodeToAscii(text: String, cb: Callback) {
        if (text.isBlank()) {
            cb.onResult("")
            return
        }
        executeJS("convertUnicodeToAscii", text, cb)
    }
}
