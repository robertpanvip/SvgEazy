package com.pan.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.handler.CefDisplayHandlerAdapter
import java.awt.MouseInfo
import java.awt.Point
import javax.swing.JComponent
import javax.swing.SwingUtilities

class DisplayHandler(
    private val component: JComponent
) : CefDisplayHandlerAdapter() {

    private var popup: JBPopup? = null

    override fun onTooltip(browser: CefBrowser?, text: String?): Boolean {
        // mouse out
        if (text == null || text.isBlank()) {
            ApplicationManager.getApplication().invokeLater {
                popup?.cancel()
                popup = null
            }
            return true
        }

        ApplicationManager.getApplication().invokeLater {

            val screenPoint = MouseInfo.getPointerInfo().location
            val point = Point(screenPoint)
            SwingUtilities.convertPointFromScreen(point, component)
            point.translate(8, 26)
            if (popup == null) {
                popup = JBPopupFactory.getInstance()
                    .createMessage(text)
                    .apply {
                        setRequestFocus(false)
                    }
                popup?.show(RelativePoint(component, point))
            }
        }

        return true
    }


    override fun onConsoleMessage(
        browser: CefBrowser?,
        level: CefSettings.LogSeverity?,
        message: String?,
        source: String?,
        line: Int
    ): Boolean {
        if (!message.isNullOrEmpty()) {
            println("JS[$level] $message ($source:$line)")
        }
        return true
    }
}

