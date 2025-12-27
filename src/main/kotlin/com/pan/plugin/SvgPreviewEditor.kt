package com.pan.plugin

import com.intellij.icons.AllIcons
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefBrowserBuilder
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.BorderFactory
import javax.swing.UIManager
import kotlin.math.log10
import kotlin.math.pow

class SvgPreviewEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor, Disposable {

    private val browser = JBCefBrowserBuilder().build()
    private val component = JPanel(BorderLayout())
    private lateinit var codeEditor: EditorEx   // 新增：代码编辑器
    private val splitter = JBSplitter(false, 0.5f)  // 垂直分屏，初始比例 5

    private enum class SplitMode { EDITOR_ONLY, PREVIEW_ONLY, SPLIT }
    private var currentMode = SplitMode.SPLIT  // 当前模式

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            updatePreview(event.document.text)
        }
    }

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

    fun getSizeInfo() :JBCefJSQuery.Response {
        return try {
            // 1. 获取文件大小（人类可读格式）
            val fileSizeBytes = file.length
            val fileSizeFormatted = formatFileSize(fileSizeBytes)
            JBCefJSQuery.Response(fileSizeFormatted)
        } catch (e: Exception) {
            JBCefJSQuery.Response("获取文件信息失败: ${e.message}")
        }
    }

    private val jsSyncQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val jsInfoQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    private fun syncWebTheme() {
        ApplicationManager.getApplication().invokeLater {
            if (browser.isDisposed) return@invokeLater
            // 获取当前编辑器背景色（最准确，跟代码区完全一致）
            val bgColor = UIUtil.getPanelBackground()  // 返回当前主题的面板背景色
            val textColor = UIUtil.getLabelForeground()  // 文字前景色
            val f =  UIUtil.getFocusedBoundsColor()
            val a = JBColor.namedColor(
                "Button.foreground.pressed",
                // 兜底：如果主题没定义 pressed，就用普通前景色
                JBColor.namedColor("Button.foreground", JBColor.WHITE)
            );
            /*print("b--rgb(${bgColor.red}, ${bgColor.green}, ${bgColor.blue})")
            print("t--rgb(${textColor.red}, ${textColor.green}, ${textColor.blue})")
            print("f--rgb(${f.red}, ${f.green}, ${f.blue})")
            print("a--rgb(${a.red}, ${a.green}, ${a.blue})")*/
            // 方式：最常用（成功回调打印结果，失败回调打印错误）
            val js = """
                    // 设置 CSS 变量
            document.documentElement.style.setProperty('--bg-color', 'rgb(${bgColor.red}, ${bgColor.green}, ${bgColor.blue})');
            document.documentElement.style.setProperty('--text-color', 'rgb(${textColor.red}, ${textColor.green}, ${textColor.blue})');
            document.documentElement.style.setProperty('--focused-color', 'rgb(${f.red}, ${f.green}, ${f.blue})');
            document.documentElement.style.setProperty('--active-text-color', 'rgb(${a.red}, ${a.green}, ${a.blue})');
                     
            window.JBCefSyncSvg = function(param,resolve,reject) {
                ${
                jsSyncQuery.inject(
                    "param",
                    "function(response) {resolve(response);}",
                    "function(code, msg) {reject(msg);}"
                )
            }
            };
             window.JBCefSvgInfo = function(resolve,reject) {
                ${
                jsInfoQuery.inject(
                    "''",
                    "function(response) {resolve(response);}",
                    "function(code, msg) {reject(msg);}"
                )
            }
            };
        """.trimIndent()
            browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        }
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

// 在页面加载完成后注入桥接函数（推荐方式）
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                syncWebTheme()  // 主题切换时立即同步
            }
        }, browser.cefBrowser)
    }

    private fun applyMode(mode: SplitMode) {
        when (mode) {
            SplitMode.SPLIT -> {
                splitter.firstComponent = codeEditor.component
                splitter.secondComponent = browser.component
                splitter.dividerWidth = 6
                splitter.proportion = 0.5f  // 可记住上次比例
            }
            SplitMode.EDITOR_ONLY -> {
                splitter.firstComponent = codeEditor.component
                splitter.secondComponent = null  // 隐藏预览
                splitter.dividerWidth = 1
            }
            SplitMode.PREVIEW_ONLY -> {
                splitter.firstComponent = null   // 隐藏编辑器
                splitter.secondComponent = browser.component
                splitter.dividerWidth = 1
            }
        }

        // 强制刷新布局
        splitter.revalidate()
        splitter.repaint()
    }

    private fun createToggleAction(): AnAction {
        return object : ToggleAction("Split Mode", "切换编辑器/预览布局", null) {

            override fun isSelected(e: AnActionEvent): Boolean {
                return true  // 总是启用（我们用按钮文本显示当前模式）
            }

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                // 点击时循环切换三种模式
                currentMode = when (currentMode) {
                    SplitMode.EDITOR_ONLY -> SplitMode.PREVIEW_ONLY
                    SplitMode.PREVIEW_ONLY -> SplitMode.SPLIT
                    SplitMode.SPLIT -> SplitMode.EDITOR_ONLY
                }
                applyMode(currentMode)
                updateButtonText(e.presentation)
            }

            override fun update(e: AnActionEvent) {
                updateButtonText(e.presentation)
            }

            private fun updateButtonText(presentation: Presentation) {
                presentation.text = when (currentMode) {
                    SplitMode.EDITOR_ONLY -> "Editor Only"
                    SplitMode.PREVIEW_ONLY -> "Preview Only"
                    SplitMode.SPLIT -> "Split"
                }
                presentation.description = "当前布局: ${presentation.text}"
            }

            // 可选：添加图标（用内置的或自定义）
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }
    }


    init {
        val document = getDocument() ?: error("No document for file $file")

// 创建工具栏（使用 ActionToolbar）
       /* val actionGroup = DefaultActionGroup()
        actionGroup.add(createToggleAction())

        val toolbar = ActionManager.getInstance()
            .createActionToolbar("SvgPreviewToolbar", actionGroup, true)
        toolbar.targetComponent = component  // 重要：绑定到你的主 component
        toolbar.component.border = JBUI.Borders.empty(4, 8)  // 左右内边距
        toolbar.setMiniMode(false)  // 可选：false 为正常大小
        component.add(toolbar.component, BorderLayout.NORTH)*/

        // 主面板布局：上边工具栏，下边 splitter
        component.layout = BorderLayout()

        component.add(splitter, BorderLayout.CENTER)

        // 创建编辑器（只读或可编辑都行，这里保持可编辑）
        codeEditor = EditorFactory.getInstance().createEditor(document, project, file, false) as EditorEx
        // 可选：配置编辑器设置（隐藏行号、折叠等，根据需要）
        codeEditor.settings.apply {
            isLineNumbersShown = true
            isFoldingOutlineShown = true
            // ... 其他自定义
        }

        // 布局：左侧代码编辑器，右侧 JCEF 预览
        splitter.firstComponent = codeEditor.component
        splitter.secondComponent = browser.component

        // 初始应用 Split 模式
        applyMode(SplitMode.SPLIT)

        val separatorColor = UIUtil.getTooltipSeparatorColor()
        // 给左侧代码编辑器加边框（右边框突出分隔）
        codeEditor.component.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.emptyRight(0),
            BorderFactory.createMatteBorder(0, 0, 0, 1, separatorColor)
        )
    // 给右侧预览加边框（左边框对应）
        browser.component.border = JBUI.Borders.empty(8)
        browser.setProperty(JBCefBrowserBase.Properties.NO_CONTEXT_MENU, true)

        component.add(splitter, BorderLayout.CENTER)

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

    override fun getComponent(): JComponent = component

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
