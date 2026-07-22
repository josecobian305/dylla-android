package app.dylla.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DyllaWebViewScreen(
    url: String,
    onReload: Boolean = false,
    onSignOut: Boolean = false,
    onReloadHandled: () -> Unit = {},
    onSignOutHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val backgroundColor = Color(0xFFFAFAF9).toArgb()

    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(onReload) {
        if (onReload) {
            webView?.reload()
            onReloadHandled()
        }
    }

    LaunchedEffect(onSignOut) {
        if (onSignOut) {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            webView?.clearCache(true)
            webView?.clearHistory()
            webView?.loadUrl(url)
            onSignOutHandled()
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(backgroundColor)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false

                        if (requestUrl.startsWith("tel:") || requestUrl.startsWith("sms:")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                            ctx.startActivity(intent)
                            return true
                        }

                        if (requestUrl.contains("stripe.com")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                            ctx.startActivity(intent)
                            return true
                        }

                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        val popupWebView = WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                setSupportMultipleWindows(false)
                            }
                        }

                        popupWebView.webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                popupView: WebView?,
                                pageUrl: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(popupView, pageUrl, favicon)
                                if (pageUrl != null && !pageUrl.contains("accounts.google.com")) {
                                    (popupView?.parent as? ViewGroup)?.removeView(popupView)
                                    popupView?.destroy()
                                }
                            }
                        }

                        val parentView = view?.parent as? ViewGroup
                        parentView?.addView(
                            popupWebView,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )

                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        transport?.webView = popupWebView
                        resultMsg?.sendToTarget()

                        return true
                    }

                    override fun onCloseWindow(window: WebView?) {
                        (window?.parent as? ViewGroup)?.removeView(window)
                        window?.destroy()
                    }
                }

                loadUrl(url)
                webView = this
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            webView = view
        }
    )
}
