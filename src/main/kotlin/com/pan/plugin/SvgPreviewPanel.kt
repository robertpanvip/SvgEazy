package com.pan.plugin

import SvgOption
import SvgSettingsDialog
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.callback.CefJSDialogCallback
import org.cef.handler.CefJSDialogHandler
import org.cef.handler.CefJSDialogHandlerAdapter
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent
import org.cef.misc.BoolRef
import javax.swing.SwingUtilities
import kotlin.math.log10
import kotlin.math.pow

class SvgPreviewPanel(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor, Disposable {

    private val browser = JBCefBrowserBuilder().build()

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            updatePreview(event.document.text)
        }
    }

    private val jsSyncQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    private val jsInfoQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    private val jsSettingQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    fun loadHtmlWithInlineResources(browser: JBCefBrowser) {
        // 读取 HTML
        val htmlStream = javaClass.getResourceAsStream("/web/index.html")
        val htmlContent = htmlStream?.bufferedReader()?.use { it.readText() } ?: return

        var inlinedHtml = htmlContent

        // 内联 CSS
        val linkRegex = Regex("""<link\s+rel=["']stylesheet["']\s+href=["'](.*?)["']\s*/?>""")
        inlinedHtml = linkRegex.replace(inlinedHtml) { match ->
            val cssPath = match.groups[1]?.value ?: ""
            val cssStream = javaClass.getResourceAsStream("/web/$cssPath")
            val cssContent = cssStream?.bufferedReader()?.use { it.readText() } ?: ""
            "<style>\n$cssContent\n</style>"
        }

        // 内联 JS
        val scriptRegex = Regex("""<script\s+src=["'](.*?)["']\s*></script>""")
        inlinedHtml = scriptRegex.replace(inlinedHtml) { match ->
            val jsPath = match.groups[1]?.value ?: ""
            val jsStream = javaClass.getResourceAsStream("/web/$jsPath")
            val jsContent = jsStream?.bufferedReader()?.use { it.readText() } ?: ""
            "<script>\n$jsContent\n</script>"
        }

        // 加载到浏览器
        browser.loadHTML(inlinedHtml)
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return "%.1f %s".format(bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
            .replace(".0 ", " ")  // 去掉 .0
    }

    fun getSizeInfo(): JBCefJSQuery.Response {
        return try {
            // 1. 获取文件大小（人类可读格式）
            val fileSizeBytes = file.length
            val fileSizeFormatted = formatFileSize(fileSizeBytes)
            JBCefJSQuery.Response(fileSizeFormatted)
        } catch (e: Exception) {
            JBCefJSQuery.Response("获取文件信息失败: ${e.message}")
        }
    }

    fun decodeFromString(jsonString: String): List<SvgOption> {
        val gson = Gson()
        val type = object : TypeToken<List<SvgOption>>() {}.type
        return gson.fromJson(jsonString, type)
    }

    fun stringify(json: List<SvgOption>): String {
        val gson = Gson()
        return gson.toJson(json)
    }

    fun handleSettingClick(jsonString:String): JBCefJSQuery.Response {
        SwingUtilities.invokeLater {
            // 假设 jsonString 是你的 JSON 字符串
            val options: List<SvgOption> = decodeFromString(jsonString);
            val dialog = SvgSettingsDialog(options)
            dialog.show();
            if (dialog.showAndGet()) {  // showAndGet() 返回 true 表示用户点击 OK
                val result = dialog.selectedOptions;
                val jsonStr = stringify(result);
                // 方式：最常用（成功回调打印结果，失败回调打印错误）
                val js = """
                    window.syncOptions(`$jsonStr`);
        """.trimIndent()
                browser.cefBrowser.executeJavaScript(js,browser.cefBrowser.url, 0)
                //println("用户选择: $jsonStr")
            } else {
                //println("用户取消了设置")
            }
        }
        return JBCefJSQuery.Response("")
    }

    private fun syncWebTheme() {
        ApplicationManager.getApplication().invokeLater {
            if (browser.isDisposed) return@invokeLater
            // 获取当前编辑器背景色（最准确，跟代码区完全一致）
            val bgColor = UIUtil.getPanelBackground()  // 返回当前主题的面板背景色
            val textColor = UIUtil.getLabelForeground()  // 文字前景色
            val f = UIUtil.getFocusedBoundsColor()
            val a = JBColor.namedColor(
                "Button.foreground.pressed",
                // 兜底：如果主题没定义 pressed，就用普通前景色
                JBColor.namedColor("Button.foreground", JBColor.WHITE)
            );
            // 方式：最常用（成功回调打印结果，失败回调打印错误）
            val js = """
                    // 设置 CSS 变量
            document.documentElement.style.setProperty('--bg-color', 'rgb(${bgColor.red}, ${bgColor.green}, ${bgColor.blue})');
            document.documentElement.style.setProperty('--text-color', 'rgb(${textColor.red}, ${textColor.green}, ${textColor.blue})');
            document.documentElement.style.setProperty('--focused-color', 'rgb(${f.red}, ${f.green}, ${f.blue})');
            document.documentElement.style.setProperty('--active-text-color', 'rgb(${a.red}, ${a.green}, ${a.blue})');
                     
            window.JBCefSyncSvg = function(param,resolve,reject) {
                ${jsSyncQuery.inject(
                    "param",
                    "function(response) {resolve(response);}",
                    "function(code, msg) {reject(msg);}"
                )}
            };
             window.JBCefSvgInfo = function(resolve,reject) {
                ${jsInfoQuery.inject(
                    "''",
                    "function(response) {resolve(response);}",
                    "function(code, msg) {reject(msg);}"
                )}
            };
             window.JBCefSetting = function(param,resolve,reject) {
                ${jsSettingQuery.inject(
                    "param",
                    "function(response) {resolve(response);}",
                    "function(code, msg) {reject(msg);}"
                )}
            };
        """.trimIndent()
            browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        }
    }

    fun addJSHandler() {
        browser.jbCefClient.addJSDialogHandler(
            object : CefJSDialogHandlerAdapter() {

                override fun onJSDialog(
                    browser: CefBrowser?,
                    originUrl: String?,
                    dialogType: CefJSDialogHandler.JSDialogType?,
                    messageText: String?,
                    defaultPromptText: String?,
                    callback: CefJSDialogCallback?,
                    suppressMessage: BoolRef?
                ): Boolean {

                    if (dialogType == CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage(
                                project,
                                messageText ?: "",
                                "SVG Preview"
                            )
                            callback?.Continue(true, null)
                        }
                        return true
                    }

                    return false
                }
            },
            browser.cefBrowser
        )
        browser.jbCefClient.addDisplayHandler(DisplayHandler(browser.component), browser.cefBrowser)
    }

    fun initJSBridge() {
        jsSyncQuery.addHandler { input ->
// input 就是前端传过来的最新 SVG 字符串（已经优化或手动编辑后的）
            if (input.isNullOrBlank()) {
                return@addHandler JBCefJSQuery.Response("错误: 接收到的 SVG 内容为空")
            }

            ApplicationManager.getApplication().invokeLater {
                val document = getDocument() ?: return@invokeLater
                val project = this.project

                // 使用 CommandProcessor 执行一个可命名、可 undo 的命令
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        ApplicationManager.getApplication().runWriteAction {
                            // 记录旧内容，用于可能的 undo
                            val oldText = document.text

                            // 替换整个内容（这一步本身不可 undo，但包裹在 command 中后就可）
                            document.setText(input)

                            // 可选：立即保存到磁盘
                            com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                                .saveDocument(document)
                        }
                    },
                    "Optimize and Save SVG",  // 显示在 Undo 菜单中的命令名
                    "SvgEazyPlugin"        // 分组名，可自定义，用于批量 undo
                )
            }
            JBCefJSQuery.Response("保存成功")
        }

        jsInfoQuery.addHandler { input ->
            return@addHandler getSizeInfo()
        }

        jsSettingQuery.addHandler { input ->
            return@addHandler handleSettingClick(input)
        }

        addJSHandler();
        syncWebTheme()  // 主题切换时立即同步
    }


    init {
        val document = getDocument() ?: error("No document for file $file")

        browser.setProperty(JBCefBrowserBase.Properties.NO_CONTEXT_MENU, true)

        // 加载 HTML（保持原样）
        loadHtmlWithInlineResources(browser)

        // 初始内容发送到前端预览
        val initialText = document.text
        updatePreview(initialText)

        initJSBridge()

        // 监听 Document 变化（用户在左侧代码编辑器修改时，实时更新右侧预览）
        document.addDocumentListener(documentListener, this)


        // 关键：监听主题变化（包括 Darcula、Light、系统主题切换等）
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener {
                syncWebTheme()  // 主题切换时立即同步
            })

    }

    private fun getDocument(): Document? {
        return com.intellij.openapi.fileEditor.FileDocumentManager
            .getInstance()
            .getDocument(file)
    }

    /**
     * 核心：把 SVG 内容发送到前端
     * 前端需要实现 window.svgApp.setSvg(svgText)
     */
    private fun updatePreview(svgContent: String) {
        val escaped = svgContent
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
        ApplicationManager.getApplication().invokeLater {
            if (browser.isDisposed) return@invokeLater
            browser.cefBrowser.zoomLevel = 0.0   // 初始化缩放为默认 1:1
            browser.component.addMouseWheelListener { e ->
                if (e.isControlDown) {
                    e.consume() // 阻止 JCEF 自带的 Ctrl+滚轮缩放
                }
            }
            browser.cefBrowser.executeJavaScript(
                """
                window.pendingSvg = `$escaped`;
                if (window.updateSvgContent && typeof window.updateSvgContent === 'function') {
                    updateSvgContent(`$escaped`);
                }
                """.trimIndent(),
                browser.cefBrowser.url,
                0
            )
        }
    }

    // ========== FileEditor 接口实现 ==========

    override fun getComponent(): JComponent = browser.component

    override fun getPreferredFocusedComponent(): JComponent = browser.component

    override fun getName(): String = "SVGEasy"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun selectNotify() {}

    override fun deselectNotify() {}

    override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getBackgroundHighlighter() = null

    override fun getStructureViewBuilder() = null

    override fun dispose() {
        browser.dispose()
    }

    // ========== 强制覆盖 getFile()，非空 @NotNull ==========
    override fun getFile(): @NotNull VirtualFile = file
}
