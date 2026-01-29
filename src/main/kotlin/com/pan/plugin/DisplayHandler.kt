package com.pan.plugin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.BalloonBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.handler.CefDisplayHandlerAdapter
import java.awt.MouseInfo
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingUtilities

class DisplayHandler(
    private val component: JComponent
) : CefDisplayHandlerAdapter() {
    private var balloon: Balloon? = null
    override fun onTooltip(browser: CefBrowser?, text: String?): Boolean {
        if (browser === null) {
            return true
        }
        // mouse out
        if (text == null || text.isBlank()) {
            ApplicationManager.getApplication().invokeLater {
                balloon?.hide()
                balloon = null;
            }
            return true
        }

        ApplicationManager.getApplication().invokeLater {

            val screenPoint = MouseInfo.getPointerInfo().location
            val point = Point(screenPoint)
            SwingUtilities.convertPointFromScreen(point, component)
            point.translate(32, 46)
            if (balloon == null) {
                val factory = JBPopupFactory.getInstance()
                val builder: BalloonBuilder = factory.createBalloonBuilder(JLabel(text))
                    .setFillColor(UIUtil.getToolTipBackground())
                    .setBorderColor(UIUtil.getTooltipSeparatorColor())
                    .setHideOnClickOutside(true)
                    .setHideOnKeyOutside(true)
                    .setHideOnAction(true)
                    .setShadow(true)
                    .setShowCallout(false)
                    .setBorderInsets(JBUI.insets(6, 8))  // 内边距
                    .setCloseButtonEnabled(false)
                    .setAnimationCycle(150)
                balloon = builder.createBalloon()
                balloon?.show(RelativePoint(component, point), Balloon.Position.below)
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

