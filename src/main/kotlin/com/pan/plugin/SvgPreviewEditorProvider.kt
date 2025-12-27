package com.pan.plugin

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SvgPreviewEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        !file.isDirectory && file.extension?.equals("svg", ignoreCase = true) == true

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        // 第一次打开文件时注册 scheme
        return SvgPreviewEditor(project, file)
    }

    override fun getEditorTypeId(): String = "SVG_PREVIEW"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}

