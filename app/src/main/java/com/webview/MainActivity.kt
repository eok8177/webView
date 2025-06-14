package com.webview


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val PERMISSIONS_REQUEST_CODE = 100

    // For handling file uploads
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // List of required permissions (reduced for stability)
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initializeFilePickerLauncher()

            createLayoutProgrammatically()

            checkAndRequestPermissions()

            setupWebView()

            webView.loadUrl("https://app.streaming.abovedemo.com/")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            handleFilePickerResult(result)
        }
    }

    private fun handleFilePickerResult(result: ActivityResult) {
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uriResults = mutableListOf<Uri>()

                // Handle single file
                data?.data?.let { uri ->
                    uriResults.add(uri)
                }

                fileUploadCallback?.onReceiveValue(uriResults.toTypedArray())
            } else {
                fileUploadCallback?.onReceiveValue(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fileUploadCallback?.onReceiveValue(null)
        } finally {
            fileUploadCallback = null
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

                // override fun onReceivedError(
                //     view: WebView?,
                //     request: WebResourceRequest?,
                //     error: WebResourceError?
                // ) {
                //     super.onReceivedError(view, request, error)
                //     Toast.makeText(this@MainActivity, "Loading error: ${error?.description}", Toast.LENGTH_SHORT).show()
                // }

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

                // Handle file selection (Android 5.0+)
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return try {
                        // Cancel previous callback if it exists
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = filePathCallback

                        // Create Intent for file selection
                        val intent = createFileChooserIntent(fileChooserParams)
                        filePickerLauncher.launch(intent)
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        filePathCallback?.onReceiveValue(null)
                        false
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

    private fun createFileChooserIntent(fileChooserParams: WebChromeClient.FileChooserParams?): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // Determine file type based on parameters
        val acceptTypes = fileChooserParams?.acceptTypes
        if (!acceptTypes.isNullOrEmpty() && acceptTypes[0].isNotEmpty()) {
            intent.type = acceptTypes[0]
        } else {
            intent.type = "*/*" // All file types
        }

        // Create chooser with additional options
        val chooserIntent = Intent.createChooser(intent, "Chose the file")

        return chooserIntent
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