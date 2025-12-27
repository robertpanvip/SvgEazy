package com.pan.plugin

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SvgPreviewEditor(
    project: Project,
    file: VirtualFile
) : TextEditorWithPreview(
    TextEditorProvider.getInstance().createEditor(project, file) as TextEditor,
    SvgPreviewPanel(project, file),
    "SVG Easy"
)