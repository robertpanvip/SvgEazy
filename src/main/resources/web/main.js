const $ = document.querySelector.bind(document);
let cleanup = [];
let options = [
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
    const v = optimizeSvg(svg, options);
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
    if(!window.JBCefSyncSvg){
        document.documentElement.setAttribute('browser',"browser");
        $('.viewer').innerHTML=`
        <svg xmlns="http://www.w3.org/2000/svg"
             xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img"
             class="iconify iconify--logos" width="31.88" height="32" preserveAspectRatio="xMidYMid meet"
             viewBox="0 0 256 257">
            <defs>
                <linearGradient id="IconifyId1813088fe1fbc01fb466" x1="-.828%" x2="57.636%" y1="7.652%" y2="78.411%">
                    <stop offset="0%" stop-color="#41D1FF"></stop>
                    <stop offset="100%" stop-color="#BD34FE"></stop>
                </linearGradient>
                <linearGradient id="IconifyId1813088fe1fbc01fb467" x1="43.376%" x2="50.316%" y1="2.242%" y2="89.03%">
                    <stop offset="0%" stop-color="#FFEA83"></stop>
                    <stop offset="8.333%" stop-color="#FFDD35"></stop>
                    <stop offset="100%" stop-color="#FFA800"></stop>
                </linearGradient>
            </defs>
            <path fill="url(#IconifyId1813088fe1fbc01fb466)"
                  d="M255.153 37.938L134.897 252.976c-2.483 4.44-8.862 4.466-11.382.048L.875 37.958c-2.746-4.814 1.371-10.646 6.827-9.67l120.385 21.517a6.537 6.537 0 0 0 2.322-.004l117.867-21.483c5.438-.991 9.574 4.796 6.877 9.62Z"></path>
            <path fill="url(#IconifyId1813088fe1fbc01fb467)"
                  d="M185.432.063L96.44 17.501a3.268 3.268 0 0 0-2.634 3.014l-5.474 92.456a3.268 3.268 0 0 0 3.997 3.378l24.777-5.718c2.318-.535 4.413 1.507 3.936 3.838l-7.361 36.047c-.495 2.426 1.782 4.5 4.151 3.78l15.304-4.649c2.372-.72 4.652 1.36 4.15 3.788l-11.698 56.621c-.732 3.542 3.979 5.473 5.943 2.437l1.313-2.028l72.516-144.72c1.215-2.423-.88-5.186-3.54-4.672l-25.505 4.922c-2.396.462-4.435-1.77-3.759-4.114l16.646-57.705c.677-2.35-1.37-4.583-3.769-4.113Z"></path>
        </svg>
        `
    }
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

function openSetting(param) {
    return new Promise((resolve, reject) => {
        window.JBCefSetting?.(param, resolve, reject);
    })
}

function syncOptions(jsonStr) {
    options = JSON.parse(jsonStr);
    const viewer = $('.viewer');
    optimizeSvg(viewer.innerHTML, options)
}

function optimizeSvg(svgString, options) {
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
    let svg = $('.viewer>svg');

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

    const handleChessClick = function () {
        viewer.classList.toggle("chess")
        this.classList.toggle('active');
        if (this.classList.contains('active')) {
            this.title = 'cancel chess board'
        } else {
            this.title = 'show chess board'
        }
    };

    const handleGridClick = function () {
        viewer.classList.toggle("grid");
        this.classList.toggle('active');
        if (this.classList.contains('active')) {
            this.title = 'cancel grid board'
        } else {
            this.title = 'show grid board'
        }
    };

    function handleCenterClick() {
        this.classList.toggle('active');
        if (this.classList.contains('active')) {
            this.title = 'cancel fit content'
            fitContentToViewBox(svg);
            syncSvg(viewer.innerHTML);
        } else {
            this.title = 'fit content'
        }
    }


    function handleSvgoStartClick() {
        this.classList.toggle('active');
        if (this.classList.contains('active')) {
            const data = optimizeSvg(viewer.innerHTML, options)
            syncSvg(data);
            this.title = 'Optimize'
            svg = $('.viewer>svg');
            updateSvgContent(data);
        } else {
            this.title = 'Optimize'
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

    initPan(center)

    cleanup.push(() => {
        chess.removeEventListener('click', handleChessClick)
        grid.removeEventListener('click', handleGridClick);
        center.removeEventListener('click', handleCenterClick);
        svgoStart.removeEventListener('click', handleSvgoStartClick);
    })
}

function initSettingPanel() {
    const setting = $('.svgo-setting');      // 设置按钮

    const settingClick = () => {
        const _options = options.map((op) => ({...op, checked: op.checked === undefined ? true : op.checked}))
        openSetting(JSON.stringify(_options)).then(res => {
            console.log(res)
        }, (err) => {
            console.log(err)
        });
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

function initPan(center) {
    const svg = $('.viewer>svg');
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
        const svg = $('.viewer>svg');
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
    let root = svg.querySelector(':scope > .data-root')
    if (root) return root

    root = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    root.setAttribute("class", 'data-root')

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

















