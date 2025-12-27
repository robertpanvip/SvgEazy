const $ = document.querySelector.bind(document);
let cleanup = [];
const options = [
    {
        key: "cleanupAttrs",
        label: "Clean up attributes",
    }, {
        key: "cleanupEnableBackground",
        label: "Clean up enable background",
    }, {
        key: "cleanupIds",
        label: "Clean up IDs",
    }, {
        key: "cleanupListOfValues",
        label: "Round numeric values in lists",
        checked: false
    }, {
        key: "cleanupNumericValues",
        label: "Round numeric values",
    }, {
        key: "collapseGroups",
        label: "Collapse groups",
    }, {
        key: "moveElemsAttrsToGroup",
        label: "Move element attributes to groups",
    }, {
        key: "moveGroupAttrsToElems",
        label: "Move group attributes to elements",
    }, {
        key: "mergePaths",
        label: "Merge paths",
    }, {
        key: "reusePaths",
        label: "Reuse paths",
        checked: false
    }, {
        key: "sortAttrs",
        label: "Sort attributes",
        checked: false
    }, {
        key: "sortDefsChildren",
        label: "Sort children of <defs>",
    }, {
        key: "prefixIds",
        label: "Prefix IDs with classname",
        checked: false
    }, {
        key: "convertColors",
        label: "Convert colors to RGB",
    }, {
        key: "convertPathData",
        label: "Convert path data",
    }, {
        key: "convertShapeToPath",
        label: "Convert shapes to paths",
    }, {
        key: "convertStyleToAttrs",
        label: "Convert styles to attributes",
    }, {
        key: "convertTransform",
        label: "Convert transforms",
    }, {
        key: "inlineStyles",
        label: "Inline styles",
    }, {
        key: "mergeStyles",
        label: "Merge styles",
    }, {
        key: "minifyStyles",
        label: "Minify styles",
    }, {
        key: "removeComments",
        label: "Remove comments",
    }, {
        key: "removeDesc",
        label: "Remove <desc>"
    }, {
        key: "removeDimensions",
        label: "Remove dimensions",
        checked: false
    }, {
        key: "removeDoctype",
        label: "Remove doctype"
    }, {
        key: "removeEditorsNSData",
        label: "Remove namespace"
    }, {
        key: "removeEmptyAttrs",
        label: "Remove empty attributes"
    }, {
        key: "removeEmptyText",
        label: "Remove empty text"
    }, {
        key: "removeHiddenElems",
        label: "Remove hidden elements"
    }, {
        key: "removeNonInheritableGroupAttrs",
        label: "Remove non-inheritable groups"
    }, {
        key: "removeOffCanvasPaths",
        label: "Remove elements outside viewbox",
        checked: false
    }, {
        key: "removeRasterImages",
        label: "Remove raster images",
        checked: false
    }, {
        key: "removeScripts",
        label: "Remove <script>"
    }, {
        key: "removeStyleElement",
        label: "Remove <style>"
    }, {
        key: "removeTitle",
        label: "Remove <title>"
    }, {
        key: "removeUnknownsAndDefaults",
        label: "Remove unknown content"
    }, {
        key: "removeUnusedNS",
        label: "Remove unused namespaces"
    }, {
        key: "removeUselessDefs",
        label: "Remove <defs> w/out <id>"
    }, {
        key: "removeUselessStrokeAndFill",
        label: "Remove unused stroke and fill"
    }, {
        key: "removeViewBox",
        label: "Remove viewBox",
        checked: false
    }, {
        key: "removeXMLProcInst",
        label: "Remove XML processing instructions"
    }];

function updateSvgContent(svg) {
    const viewer = $('.viewer');
    viewer.innerHTML = svg;
    const v = optimizeSvg(svg);
    const template = document.createElement('template')
    template.innerHTML = v;
    if (template.innerHTML !== viewer.innerHTML) {
        $('.svgo-start').classList.remove('active');
    }
    cleanup.forEach(clean => clean());
    bootstrap();
}

if (window.pendingSvg) {
    updateSvgContent(window.pendingSvg);
} else {
    bootstrap();
}

function syncSvg(text) {
    return new Promise((resolve, reject) => {
        window.JBCefSyncSvg?.(text, resolve, reject);
    })
}

function getSvgInfo() {
    return new Promise((resolve, reject) => {
        window.JBCefSvgInfo?.(resolve, reject);
    })
}

function optimizeSvg(svgString) {
    const viewer = $('.viewer');
    const settingForm = $('.svgo-list');
    const f = new FormData(settingForm);
    const v = Array.from(f.values());
    const extra = [
        "sortAttrs",
        "sortDefsChildren",
        "prefixIds",
        "reusePaths",
        "convertStyleToAttrs",
        "convertTransform",
        "removeDimensions",
        "mergeStyles",
        "cleanupListOfValues",
        "removeOffCanvasPaths",
        "removeRasterImages",
        "removeScripts",
        "removeStyleElement",
        "removeTitle",
        "removeViewBox"
    ]

    const entries = options.filter(item => !extra.includes(item.key)).map(o => [o.key, v.includes(o.key)])
    const overrides = Object.fromEntries(entries);
    const result = window.svgo?.optimize?.(svgString, {
        multipass: true,  // 推荐开启多次优化
        plugins: [
            {
                name: 'preset-default',
                params: {overrides}
            },
            ...v.filter(item => extra.includes(item))
        ],  // 或自定义插件
    });
    viewer.innerHTML = result.data;
    return result.data
}

function bootstrap() {
    const viewer = $('.viewer');
    const settingForm = $('.svgo-list');
    const chess = $('.svgo-chess');
    const grid = $('.svgo-grid');
    const center = $('.svgo-middle');
    const sizeInfo = $('.size-info');
    const svgoStart = $('.svgo-start');
    const svg = $('.viewer>svg');

    getSvgInfo().then(res => {
        let size = ''
        const viewBox = svg.getAttribute('viewBox');
        if (viewBox) {
            const parts = viewBox.trim().split(/\s+/);
            if (parts.length === 4) {
                size = `${parts[2]}x${parts[3]} SVG`
            }
        }
        svg.getAttribute('viewBox')
        sizeInfo.innerHTML = `${size}  ${res}`
    })


    const gridLikeClick = function (token) {
        return function () {
            viewer.classList.toggle(token)
            this.classList.toggle('active');
        }
    }
    const handleChessClick = gridLikeClick('chess');
    const handleGridClick = gridLikeClick('grid')

    function handleCenterClick() {
        this.classList.toggle('active');
        if (this.classList.contains('active')) {
            fitContentToViewBox(svg);
            syncSvg(viewer.innerHTML);
        }
    }


    function handleSvgoStartClick() {
        this.classList.toggle('active');
        if (this.classList.contains('active')) {
            const data = optimizeSvg(viewer.innerHTML)
            syncSvg(data);
        }
    }

    chess.addEventListener('click', handleChessClick)
    grid.addEventListener('click', handleGridClick)
    center.addEventListener('click', handleCenterClick)
    svgoStart.addEventListener('click', handleSvgoStartClick)

    initSettingPanel()

    const createTemplate = (title, value, checked = true) => `<label class="svgo-list-item">
                <label class="toggle-button_checkbox">
                    <input type="checkbox" name="plugin" class="toggle-button_input" ${checked ? "checked=true" : ""} value=${value}>
                    <span class="toggle-button_checkmark"></span>
                </label>
                <span>${title}</span>
            </label>`
    settingForm.innerHTML = options.map(op => createTemplate(op.label, op.key, op.checked)).join('\n');


    zoom(svg, viewer)

    initPan(svg, center)

    cleanup.push(() => {
        chess.removeEventListener('click', handleChessClick)
        grid.removeEventListener('click', handleGridClick);
        center.removeEventListener('click', handleCenterClick);
        svgoStart.removeEventListener('click', handleSvgoStartClick);
    })
}

function initSettingPanel() {
    const setting = $('.svgo-setting');      // 设置按钮
    const settingPanel = $('.svgo-list'); // 假设按钮和面板是同一个元素，或改成面板容器
    // 如果面板是独立的子元素，请改成 const settingPanel = $('.svgo-panel') 或类似
    const toggleSetting = (e) => {
        e.stopPropagation(); // 阻止事件冒泡到 window
        setting.classList.toggle('active');
    };

    const closePanelIfClickOutside = (e) => {
        if (!settingPanel.contains(e.target)) {
            setting.classList.remove('active');
            // 可选：移除自身监听器，避免常驻（性能更好）
            window.removeEventListener('click', closePanelIfClickOutside);
            document.removeEventListener('keydown', keydown);
        } else {
            window.addEventListener('click', closePanelIfClickOutside, {once: true});
        }
    };

    // 点击设置按钮：切换面板显示
    //setting.addEventListener('click', togglePanel);

    const settingClick = (e) => {
        toggleSetting(e);
        // 如果面板现在是打开状态，才添加外部点击关闭监听
        if (setting.classList.contains('active')) {
            // 使用 setTimeout 0 或 nextTick 确保点击按钮本身的事件已冒泡完毕
            setTimeout(() => {
                window.addEventListener('click', closePanelIfClickOutside, {once: true});
            }, 0);
        }
    }

    setting.addEventListener('click', settingClick);

    const keydown = function (e) {
        if (e.key === 'Escape' && setting.classList.contains('active')) {
            setting.classList.remove('active');
        }
    }

    // 可选：按 ESC 键关闭面板
    document.addEventListener('keydown', keydown);

    cleanup.push(() => {
        setting.removeEventListener('click', settingClick);
        document.removeEventListener('keydown', keydown);
    })
}

function zoom(svg, viewer) {
    let scale = 1;
    let isCtrlPressed = false; // 跟踪 Ctrl 键状态

    // 原始尺寸（只算一次）
    const baseRect = viewer.getBoundingClientRect();
    const baseLeft = baseRect.left;
    const baseTop = baseRect.top;
    const baseWidth = baseRect.width;
    const baseHeight = baseRect.height;

    // 更新缩放原点（只在 Ctrl 按下时调用）
    const updateTransformOrigin = (e) => {
        if (!isCtrlPressed) return;

        const offsetX = e.clientX - baseLeft;
        const offsetY = e.clientY - baseTop;

        const originX = Math.max(0, Math.min(100, (offsetX / baseWidth) * 100));
        const originY = Math.max(0, Math.min(100, (offsetY / baseHeight) * 100));

        viewer.style.transformOrigin = `${originX}% ${originY}%`;
    };

    // 鼠标移动时：仅当 Ctrl 按下才更新 origin
    const handleMouseMove = (e) => {
        updateTransformOrigin(e);
    };

    viewer.addEventListener('mousemove', handleMouseMove);

    // 全局监听 Ctrl 键按下/松开
    const handleKeyDown = (e) => {
        if (e.key === 'Control') {
            isCtrlPressed = true;
            // 按下 Ctrl 时立即以当前鼠标位置设置 origin（避免延迟）
            updateTransformOrigin(e);
        }
    };

    const handleKeyUp = (e) => {
        if (e.key === 'Control') {
            isCtrlPressed = false;
            // 松开 Ctrl 时恢复默认中心（防止残留偏移感）
            viewer.style.transformOrigin = '50% 50%';
        }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    // 滚轮缩放
    const handleWheel = (e) => {
        if (!e.ctrlKey) return; // 必须按住 Ctrl 才缩放

        e.preventDefault();

        // 确保 origin 是最新的
        updateTransformOrigin(e);

        const delta = e.deltaY < 0 ? 1.1 : 0.9;
        scale *= delta;
        scale = Math.max(0.1, Math.min(scale, 50));

        viewer.style.transform = `scale(${scale})`;
    };

    document.addEventListener('wheel', handleWheel, {passive: false});

    // 重置缩放
    viewer.resetZoom = () => {
        scale = 1;
        viewer.style.transform = 'scale(1)';
        viewer.style.transformOrigin = '50% 50%';
    };

    // 双击复位（可选）
    viewer.addEventListener('dblclick', () => {
        viewer.resetZoom();
    });

    // 清理
    cleanup.push(() => {
        viewer.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('keydown', handleKeyDown);
        window.removeEventListener('keyup', handleKeyUp);
        document.removeEventListener('wheel', handleWheel);
        viewer.removeEventListener('dblclick', arguments.callee);
    });
}

function initPan(svg, center) {
    let activeEl = null
    let dragging = false

    let startMouse = {x: 0, y: 0}
    let startTranslate = {x: 0, y: 0}

    // 把屏幕坐标转成 SVG 坐标（非常关键）
    function getSvgPoint(e) {
        const pt = svg.createSVGPoint()
        pt.x = e.clientX
        pt.y = e.clientY
        return pt.matrixTransform(svg.getScreenCTM().inverse())
    }

    // mousedown：记录起始状态
    svg.addEventListener('mousedown', e => {
        // 只拖 svg 内的元素（排除 svg 本身）
        if (e.target === svg) return

        activeEl = e.target
        dragging = true
        e.preventDefault()
        e.stopPropagation()

        const p = getSvgPoint(e)
        startMouse.x = p.x
        startMouse.y = p.y

        // 读取当前 transform
        const transform = activeEl.transform.baseVal.consolidate()
        if (transform) {
            startTranslate.x = transform.matrix.e
            startTranslate.y = transform.matrix.f
        } else {
            startTranslate.x = 0
            startTranslate.y = 0
        }
    })

    const mousemove = e => {
        if (!dragging || !activeEl) return
        center.classList.remove('active');
        const p = getSvgPoint(e)
        const dx = p.x - startMouse.x
        const dy = p.y - startMouse.y

        activeEl.setAttribute(
            'transform',
            `translate(${startTranslate.x + dx}, ${startTranslate.y + dy})`
        )
    }
    const mouseup = () => {
        dragging = false
        activeEl = null;
        syncSvg(svg.parentNode.innerHTML)
    }

    // mousemove：绝对位移 = 起始位移 + 本次偏移
    window.addEventListener('mousemove', mousemove)
    // mouseup：结束拖动
    window.addEventListener('mouseup', mouseup)
    cleanup.push(() => {
        window.removeEventListener('mousemove', mousemove)
        window.removeEventListener('mouseup', mouseup)
    })
}

function ensureRootGroup(svg) {
    let root = svg.querySelector(':scope > g[data-root]')
    if (root) return root

    root = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    root.setAttribute("data-root", '')

    while (svg.firstChild) {
        root.appendChild(svg.firstChild)
    }
    svg.appendChild(root)
    return root
}


function fitContentToViewBox(svg) {
    const root = ensureRootGroup(svg)
    const bbox = root.getBBox()
    const vb = svg.viewBox.baseVal
    if (!vb.width || !vb.height) return

    const scaleX = vb.width / bbox.width
    const scaleY = vb.height / bbox.height
    const scale = Math.min(scaleX, scaleY, 1) // 防止放大

    const offsetX = vb.x + vb.width / 2 - (bbox.x + bbox.width / 2) * scale
    const offsetY = vb.y + vb.height / 2 - (bbox.y + bbox.height / 2) * scale

    root.setAttribute('transform', `translate(${offsetX}, ${offsetY}) scale(${scale})`)
}

















