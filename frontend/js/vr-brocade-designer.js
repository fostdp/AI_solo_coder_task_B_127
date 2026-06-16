var VrBrocadeDesigner = (function() {
    var currentDesign = null;
    var designCanvas, designCtx;
    var previewCanvas, previewCtx;
    var isDrawing = false;
    var currentTool = 'brush';
    var brushSize = 1;
    var currentColor = '#8B4513';
    var cellSize = 5;
    var warpCount = 120;
    var weftCount = 200;
    var colorPalette = ['#8B4513', '#E8D4A8', '#DC143C', '#1B4F91', '#FFD700', '#2E8B57', '#483D8B'];
    var patternData = [];
    var isSimulating = false;
    var simSteps = 0;
    var API_BASE = '/api/virtual-weaving';

    function init() {
        initCanvas();
        initPatternData();
        loadSelectOptions();
        bindEvents();
        loadGallery();
        renderColorPalette();
        renderDesignInfo();
        renderPreview();
    }

    function initCanvas() {
        designCanvas = document.getElementById('design-canvas');
        designCtx = designCanvas.getContext('2d');
        previewCanvas = document.getElementById('design-preview-canvas');
        previewCtx = previewCanvas.getContext('2d');

        designCanvas.width = 600;
        designCanvas.height = 800;
    }

    function initPatternData() {
        patternData = [];
        for (var i = 0; i < weftCount; i++) {
            var row = [];
            for (var j = 0; j < warpCount; j++) {
                row.push((i + j) % 2);
            }
            patternData.push(row);
        }
        drawDesignCanvas();
    }

    function loadSelectOptions() {
        fetch('/api/patterns/popular?limit=20')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var select = document.getElementById('pattern-template-select');
                data.forEach(function(p) {
                    var opt = document.createElement('option');
                    opt.value = p.id;
                    opt.textContent = p.name;
                    select.appendChild(opt);
                });
            });

        fetch('/api/varieties')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var select = document.getElementById('design-variety-select');
                data.forEach(function(v) {
                    var opt = document.createElement('option');
                    opt.value = v.id;
                    opt.textContent = v.name;
                    select.appendChild(opt);
                });
            });
    }

    function bindEvents() {
        designCanvas.addEventListener('mousedown', startDrawing);
        designCanvas.addEventListener('mousemove', draw);
        designCanvas.addEventListener('mouseup', stopDrawing);
        designCanvas.addEventListener('mouseleave', stopDrawing);

        document.querySelectorAll('.tool-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                document.querySelectorAll('.tool-btn').forEach(function(b) {
                    b.classList.remove('active');
                });
                btn.classList.add('active');
                currentTool = btn.getAttribute('data-tool');
            });
        });

        document.getElementById('brush-size').addEventListener('input', function(e) {
            brushSize = parseInt(e.target.value);
        });

        document.getElementById('design-warp-count').addEventListener('input', function(e) {
            warpCount = parseInt(e.target.value);
            document.getElementById('warp-count-val').textContent = warpCount;
            resizePattern();
        });

        document.getElementById('design-weft-count').addEventListener('input', function(e) {
            weftCount = parseInt(e.target.value);
            document.getElementById('weft-count-val').textContent = weftCount;
            resizePattern();
        });

        document.getElementById('pattern-template-select').addEventListener('change', function(e) {
            if (e.target.value) {
                loadPattern(parseInt(e.target.value));
            }
        });

        document.getElementById('design-variety-select').addEventListener('change', function(e) {
            if (e.target.value) {
                createFromVariety(parseInt(e.target.value));
            }
        });

        document.getElementById('btn-save-design').addEventListener('click', saveDesign);
        document.getElementById('btn-simulate-design').addEventListener('click', startSimulation);
        document.getElementById('btn-clear-design').addEventListener('click', clearDesign);
        document.getElementById('btn-load-designs').addEventListener('click', loadGallery);
    }

    function startDrawing(e) {
        isDrawing = true;
        handleDraw(e);
    }

    function draw(e) {
        if (!isDrawing) return;
        handleDraw(e);
    }

    function stopDrawing() {
        isDrawing = false;
        renderPreview();
    }

    function handleDraw(e) {
        var rect = designCanvas.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;

        var col = Math.floor(x / cellSize);
        var row = Math.floor(y / cellSize);

        if (col < 0 || col >= warpCount || row < 0 || row >= weftCount) return;

        if (currentTool === 'picker') {
            var value = patternData[row][col];
            currentColor = value === 1 ? '#E8D4A8' : '#8B4513';
            return;
        }

        for (var dr = -brushSize + 1; dr < brushSize; dr++) {
            for (var dc = -brushSize + 1; dc < brushSize; dc++) {
                var r = row + dr;
                var c = col + dc;
                if (r >= 0 && r < weftCount && c >= 0 && c < warpCount) {
                    if (currentTool === 'eraser') {
                        patternData[r][c] = 0;
                    } else if (currentTool === 'fill') {
                        floodFill(r, c, patternData[r][c], 1);
                    } else {
                        patternData[r][c] = 1;
                    }
                }
            }
        }

        drawDesignCanvas();
    }

    function floodFill(row, col, target, replacement) {
        if (target === replacement) return;
        var stack = [[row, col]];
        while (stack.length > 0) {
            var cell = stack.pop();
            var r = cell[0], c = cell[1];
            if (r < 0 || r >= weftCount || c < 0 || c >= warpCount) continue;
            if (patternData[r][c] !== target) continue;
            patternData[r][c] = replacement;
            stack.push([r + 1, c], [r - 1, c], [r, c + 1], [r, c - 1]);
        }
    }

    function drawDesignCanvas() {
        var displayWidth = Math.min(designCanvas.width, warpCount * cellSize);
        var displayHeight = Math.min(designCanvas.height, weftCount * cellSize);

        designCtx.clearRect(0, 0, designCanvas.width, designCanvas.height);
        designCtx.fillStyle = '#f5f5f5';
        designCtx.fillRect(0, 0, displayWidth, displayHeight);

        for (var i = 0; i < weftCount && i * cellSize < designCanvas.height; i++) {
            for (var j = 0; j < warpCount && j * cellSize < designCanvas.width; j++) {
                var value = patternData[i][j];
                designCtx.fillStyle = value === 1 ? '#E8D4A8' : '#8B4513';
                designCtx.fillRect(j * cellSize, i * cellSize, cellSize - 1, cellSize - 1);
            }
        }

        if (simSteps > 0) {
            designCtx.fillStyle = 'rgba(34, 139, 34, 0.3)';
            designCtx.fillRect(0, 0, displayWidth, simSteps * cellSize);
        }

        renderDesignInfo();
    }

    function renderColorPalette() {
        var container = document.getElementById('design-color-palette');
        var html = '';
        colorPalette.forEach(function(color, idx) {
            html += '<div class="design-color-swatch" style="background:' + color + '" ' +
                    'data-color="' + color + '"></div>';
        });
        container.innerHTML = html;

        container.querySelectorAll('.design-color-swatch').forEach(function(swatch) {
            swatch.addEventListener('click', function() {
                currentColor = swatch.getAttribute('data-color');
                container.querySelectorAll('.design-color-swatch').forEach(function(s) {
                    s.classList.remove('selected');
                });
                swatch.classList.add('selected');
            });
        });
    }

    function renderDesignInfo() {
        var info = document.getElementById('design-info');
        info.innerHTML = '<span>' + warpCount + ' × ' + weftCount + ' 像素</span>';

        var ones = 0, zeros = 0;
        for (var i = 0; i < Math.min(weftCount, 50); i++) {
            for (var j = 0; j < warpCount; j++) {
                if (patternData[i][j] === 1) ones++;
                else zeros++;
            }
        }
        var total = ones + zeros;
        if (total > 0) {
            var warpPercent = Math.round(ones / total * 10000) / 100;
            info.innerHTML += ' <span class="warp-info">经浮: ' + warpPercent + '%</span>';
        }
    }

    function renderPreview() {
        var w = previewCanvas.width;
        var h = previewCanvas.height;
        previewCtx.clearRect(0, 0, w, h);

        var cellW = w / warpCount;
        var cellH = h / weftCount;
        var maxRows = Math.min(weftCount, Math.floor(h / cellH));

        for (var i = 0; i < maxRows; i++) {
            for (var j = 0; j < warpCount; j++) {
                var value = patternData[i][j];
                previewCtx.fillStyle = value === 1 ? '#E8D4A8' : '#8B4513';
                previewCtx.fillRect(j * cellW, i * cellH, cellW + 0.5, cellH + 0.5);
            }
        }

        if (simSteps > 0) {
            previewCtx.fillStyle = 'rgba(34, 139, 34, 0.15)';
            var simH = Math.min(simSteps * cellH, h);
            previewCtx.fillRect(0, 0, w, simH);

            previewCtx.strokeStyle = '#228B22';
            previewCtx.lineWidth = 2;
            previewCtx.beginPath();
            previewCtx.moveTo(0, simH);
            previewCtx.lineTo(w, simH);
            previewCtx.stroke();
        }
    }

    function resizePattern() {
        var newData = [];
        for (var i = 0; i < weftCount; i++) {
            var row = [];
            for (var j = 0; j < warpCount; j++) {
                if (patternData[i] && patternData[i][j] !== undefined) {
                    row.push(patternData[i][j]);
                } else {
                    row.push((i + j) % 2);
                }
            }
            newData.push(row);
        }
        patternData = newData;
        drawDesignCanvas();
        renderPreview();
    }

    function loadPattern(patternId) {
        fetch('/api/patterns/' + patternId + '/matrix')
            .then(function(r) { return r.json(); })
            .then(function(matrix) {
                if (matrix && matrix.length > 0) {
                    warpCount = matrix[0].length;
                    weftCount = Math.min(matrix.length, 500);
                    document.getElementById('design-warp-count').value = warpCount;
                    document.getElementById('design-weft-count').value = weftCount;
                    document.getElementById('warp-count-val').textContent = warpCount;
                    document.getElementById('weft-count-val').textContent = weftCount;

                    patternData = matrix.slice(0, weftCount).map(function(row) {
                        return row.slice(0, warpCount);
                    });

                    while (patternData.length < weftCount) {
                        var newRow = [];
                        for (var j = 0; j < warpCount; j++) {
                            newRow.push((patternData.length + j) % 2);
                        }
                        patternData.push(newRow);
                    }

                    drawDesignCanvas();
                    renderPreview();
                    fetchDesignPreview();
                }
            })
            .catch(function(err) {
                console.error('加载纹样失败:', err);
            });
    }

    function createFromVariety(varietyId) {
        var designerName = prompt('请输入您的设计师名称�?, '游客设计�?) || '游客设计�?;
        fetch(API_BASE + '/create/from-variety?varietyId=' + varietyId + '&designer=' +
              encodeURIComponent(designerName), { method: 'POST' })
            .then(function(r) { return r.json(); })
            .then(function(design) {
                currentDesign = design;
                if (design.patternMatrix) {
                    patternData = parseMatrix(design.patternMatrix);
                    warpCount = design.warpCount || 120;
                    weftCount = design.weftCount || 200;
                    drawDesignCanvas();
                    renderPreview();
                }
                alert('已基�?' + design.baseVarietyName + '"创建设计�?);
            })
            .catch(function(err) {
                console.error('创建设计失败:', err);
            });
    }

    function parseMatrix(str) {
        var lines = str.trim().split('\n');
        var matrix = [];
        for (var i = 0; i < lines.length; i++) {
            var cells = lines[i].trim().split(',');
            var row = [];
            for (var j = 0; j < cells.length; j++) {
                row.push(parseInt(cells[j].trim()) || 0);
            }
            matrix.push(row);
        }
        return matrix;
    }

    function matrixToString(matrix) {
        var lines = [];
        for (var i = 0; i < matrix.length; i++) {
            lines.push(matrix[i].join(','));
        }
        return lines.join('\n');
    }

    function saveDesign() {
        var name = prompt('请输入设计名称：', '我的云锦设计');
        if (!name) return;

        var designerName = prompt('请输入设计师名称�?, '游客设计�?) || '游客设计�?;

        var design = {
            name: name,
            designer: designerName,
            warpCount: warpCount,
            weftCount: weftCount,
            colorCount: colorPalette.length,
            patternMatrix: matrixToString(patternData),
            colorPalette: colorPalette.join('\n'),
            isPublic: true
        };

        var url = API_BASE + '/create/blank?warpCount=' + warpCount + '&weftCount=' + weftCount +
                  '&designer=' + encodeURIComponent(designerName);

        fetch(url, { method: 'POST' })
            .then(function(r) { return r.json(); })
            .then(function(saved) {
                currentDesign = saved;
                var updateData = {
                    name: name,
                    patternMatrix: matrixToString(patternData),
                    warpCount: warpCount,
                    weftCount: weftCount,
                    colorCount: colorPalette.length
                };
                return fetch(API_BASE + '/designs/' + saved.id, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(updateData)
                });
            })
            .then(function(r) { return r.json(); })
            .then(function(updated) {
                currentDesign = updated;
                alert('设计保存成功！ID: ' + updated.id);
                loadGallery();
            })
            .catch(function(err) {
                console.error('保存失败:', err);
                alert('保存失败，请重试');
            });
    }

    function startSimulation() {
        if (!currentDesign) {
            alert('请先保存设计�?);
            return;
        }

        if (isSimulating) {
            isSimulating = false;
            document.getElementById('btn-simulate-design').textContent = '▶️ 模拟织�?;
            return;
        }

        isSimulating = true;
        simSteps = 0;
        document.getElementById('btn-simulate-design').textContent = '⏸️ 暂停';

        fetchDesignPreview();
        runSimulationStep();
    }

    function runSimulationStep() {
        if (!isSimulating || simSteps >= weftCount) {
            isSimulating = false;
            document.getElementById('btn-simulate-design').textContent = '▶️ 模拟织�?;
            return;
        }

        var batchSize = 10;
        simSteps = Math.min(weftCount, simSteps + batchSize);

        var progress = Math.round(simSteps / weftCount * 100);
        document.querySelector('#sim-progress span').textContent = '模拟进度 ' + progress + '%';
        document.getElementById('sim-progress-fill').style.width = progress + '%';

        drawDesignCanvas();
        renderPreview();

        fetch(API_BASE + '/simulate/' + currentDesign.id + '?steps=' + batchSize, { method: 'POST' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderSimulationStats(data);
            })
            .catch(function() {});

        setTimeout(runSimulationStep, 200);
    }

    function renderSimulationStats(data) {
        var container = document.getElementById('design-stats');
        var html = '';
        html += '<div class="sim-stat"><label>已织</label><span>' + data.steps + ' �?/span></div>';
        html += '<div class="sim-stat"><label>进度</label><span>' + data.progressPercent + '%</span></div>';
        html += '<div class="sim-stat"><label>平均张力</label><span>' + data.averageTension + ' N</span></div>';
        html += '<div class="sim-stat"><label>断头�?/label><span class="' +
                (data.warpBreakCount > 0 ? 'danger' : '') + '">' + data.warpBreakCount + '</span></div>';
        html += '<div class="sim-stat"><label>已耗时</label><span>' + data.estimatedTimeHours.toFixed(2) + ' h</span></div>';
        container.innerHTML = html;
    }

    function fetchDesignPreview() {
        if (!currentDesign) return;
        fetch(API_BASE + '/designs/' + currentDesign.id + '/preview')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderPreviewStats(data);
            })
            .catch(function() {});
    }

    function renderPreviewStats(data) {
        var html = '';
        html += '<div class="sim-stat"><label>组织</label><span>' +
                (data.isBalancedWeave ? '平衡' : '不平�?) + '</span></div>';
        html += '<div class="sim-stat"><label>平均浮点</label><span>' + data.averageFloatLength + '</span></div>';
        html += '<div class="sim-stat"><label>经覆盖率</label><span>' + data.warpCoveragePercent + '%</span></div>';
        html += '<div class="sim-stat"><label>花回</label><span>' + (data.detectedPatternRepeat || []).join('×') + '</span></div>';
        var container = document.getElementById('design-stats');
        container.innerHTML += html;
    }

    function clearDesign() {
        if (confirm('确定要清空当前设计吗�?)) {
            simSteps = 0;
            isSimulating = false;
            document.getElementById('btn-simulate-design').textContent = '▶️ 模拟织�?;
            initPatternData();
            renderPreview();
            document.getElementById('design-stats').innerHTML = '';
            document.getElementById('sim-progress-fill').style.width = '0%';
            document.querySelector('#sim-progress span').textContent = '模拟进度 0%';
        }
    }

    function loadGallery() {
        fetch(API_BASE + '/designs/popular?limit=8')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderGallery(data);
            })
            .catch(function(err) {
                console.error('加载设计库失�?', err);
            });
    }

    function renderGallery(designs) {
        var container = document.getElementById('design-gallery-grid');
        if (!designs || designs.length === 0) {
            container.innerHTML = '<div class="empty-hint">暂无设计作品</div>';
            return;
        }

        var html = designs.map(function(d) {
            var matrix = parseMatrix(d.patternMatrix || '0,1\n1,0');
            var thumb = generateThumbnail(matrix);
            return '<div class="gallery-card" data-id="' + d.id + '">' +
                thumb +
                '<div class="gallery-info">' +
                    '<h5>' + (d.name || '未命�?) + '</h5>' +
                    '<p>' + (d.designer || '匿名设计�?) + '</p>' +
                    '<div class="gallery-stats">' +
                        '<span>💙 ' + (d.likeCount || 0) + '</span>' +
                        '<span>👁�?' + (d.viewCount || 0) + '</span>' +
                    '</div>' +
                '</div>' +
                '<button class="btn-load-design" data-id="' + d.id + '">加载</button>' +
            '</div>';
        }).join('');

        container.innerHTML = html;

        container.querySelectorAll('.btn-load-design').forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                var id = parseInt(btn.getAttribute('data-id'));
                loadDesignFromGallery(id);
            });
        });
    }

    function generateThumbnail(matrix) {
        var w = 80, h = 80;
        if (matrix.length === 0) return '<canvas width="' + w + '" height="' + h + '"></canvas>';

        var warp = matrix[0].length;
        var weft = matrix.length;

        var canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        var ctx = canvas.getContext('2d');

        for (var i = 0; i < h; i++) {
            for (var j = 0; j < w; j++) {
                var rowIdx = Math.floor(i / h * weft);
                var colIdx = Math.floor(j / w * warp);
                var val = matrix[rowIdx] && matrix[rowIdx][colIdx];
                ctx.fillStyle = val === 1 ? '#E8D4A8' : '#8B4513';
                ctx.fillRect(j, i, 1, 1);
            }
        }

        return canvas.outerHTML;
    }

    function loadDesignFromGallery(designId) {
        fetch(API_BASE + '/designs/' + designId)
            .then(function(r) { return r.json(); })
            .then(function(design) {
                currentDesign = design;
                if (design.patternMatrix) {
                    patternData = parseMatrix(design.patternMatrix);
                    warpCount = design.warpCount || 120;
                    weftCount = design.weftCount || 200;
                    document.getElementById('design-warp-count').value = warpCount;
                    document.getElementById('design-weft-count').value = weftCount;
                    document.getElementById('warp-count-val').textContent = warpCount;
                    document.getElementById('weft-count-val').textContent = weftCount;
                    drawDesignCanvas();
                    renderPreview();
                    fetchDesignPreview();
                }
                simSteps = 0;
                isSimulating = false;
                document.getElementById('btn-simulate-design').textContent = '▶️ 模拟织�?;
                document.getElementById('sim-progress-fill').style.width = '0%';
                document.querySelector('#sim-progress span').textContent = '模拟进度 0%';
            })
            .catch(function(err) {
                console.error('加载设计失败:', err);
            });
    }

    return {
        init: init,
        loadPattern: loadPattern
    };
})();
