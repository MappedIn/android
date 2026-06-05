package com.mappedin.demo

import android.graphics.Color
import android.webkit.WebView

/**
 * Renders a Mappedin icon SVG string inside a [WebView].
 *
 * Mappedin icon SVGs use `fill="currentColor"`, so the icon color is controlled
 * by the CSS `color` of the container. This mirrors how the web examples tint
 * icons and lets the native demos recolor an icon without re-fetching it.
 *
 * @param svg The raw SVG markup returned by `mapView.icons.fetchSvg(...)`
 * @param color A CSS color string (e.g. "#2266ff") applied via `currentColor`
 */
fun WebView.renderIconSvg(svg: String, color: String) {
    settings.javaScriptEnabled = false
    setBackgroundColor(Color.TRANSPARENT)
    val html =
        """
        <!DOCTYPE html>
        <html>
          <head><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"></head>
          <body style="margin:0;padding:0;height:100%;display:flex;align-items:center;justify-content:center;color:$color;">
            <div style="width:70%;height:70%;display:flex;align-items:center;justify-content:center;">$svg</div>
          </body>
        </html>
        """.trimIndent()
    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}
