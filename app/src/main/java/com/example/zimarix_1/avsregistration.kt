package com.example.zimarix_1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView

class avsregistration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avsregistration)

        val b = intent.extras
        var value = "" // or other values
        if (b != null) value = b.getString("avskey").toString()

        var txt: TextView =findViewById<TextView>(R.id.textView2)
        txt.setText("ALEXA REGISTRATION CODE : " + value)

        var webView: WebView =findViewById<WebView>(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://amazon.com/us/code")
        webView.settings.javaScriptEnabled = true
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }
}