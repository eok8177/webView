package com.webview


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val PERMISSIONS_REQUEST_CODE = 100

    // List of required permissions (reduced for stability)
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Option 1: Create layout programmatically (without XML file)
            createLayoutProgrammatically()

            // Check and request permissions
            checkAndRequestPermissions()

            // WebView setup
            setupWebView()

            // Load the web page (start with a simple site)
            webView.loadUrl("https://app.streaming.abovedemo.com/")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Method to create layout programmatically
    private fun createLayoutProgrammatically() {
        try {
            // Create WebView programmatically
            webView = WebView(this)
            webView.layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )

            // Create LinearLayout as a container
            val layout = android.widget.LinearLayout(this)
            layout.orientation = android.widget.LinearLayout.VERTICAL
            layout.addView(webView)

            // Set the layout as the content view
            setContentView(layout)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (!allPermissionsGranted) {
                // If permissions are not granted, you can show a message to the user
                // or restrict functionality
            }
        }
    }

    private fun setupWebView() {
        try {
            // WebSettings configuration
            val webSettings: WebSettings = webView.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.allowFileAccess = true
            webSettings.allowContentAccess = true
            webSettings.mediaPlaybackRequiresUserGesture = false
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Additional settings for stability (without deprecated methods)
            webSettings.cacheMode = WebSettings.LOAD_DEFAULT
            webSettings.databaseEnabled = true
            webSettings.setSupportZoom(true)
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false
            webSettings.loadWithOverviewMode = true
            webSettings.useWideViewPort = true

            // Modern security and performance settings
            webSettings.allowFileAccessFromFileURLs = false
            webSettings.allowUniversalAccessFromFileURLs = false
            webSettings.setSupportMultipleWindows(false)

            // Set WebViewClient with error handling
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false  // Allow WebView to handle the URL
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Toast.makeText(this@MainActivity, "Loading error: ${error?.description}", Toast.LENGTH_SHORT).show()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Page loaded successfully
                }
            }

            // Set WebChromeClient with permission handling
            webView.webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    try {
                        // Check if the required Android system permissions are granted
                        val hasCamera = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        val hasAudio = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

                        if (hasCamera && hasAudio) {
                            request?.grant(request.resources)
                        } else {
                            request?.deny()
                            Toast.makeText(this@MainActivity, "Camera and microphone permissions are required", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        request?.deny()
                    }
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    // Logging JavaScript errors
                    consoleMessage?.let {
                        println("WebView Console: ${it.message()} at ${it.sourceId()}:${it.lineNumber()}")
                    }
                    return true
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "WebView setup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Handle the "Back" button
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}