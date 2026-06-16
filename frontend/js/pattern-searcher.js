var PatternSearcher = (function() {
    var currentPage = 0;
    var pageSize = 12;
    var totalPages = 1;
    var API_BASE = '/api/patterns';

    function init() {
        loadFilters();
        loadPatterns();
        bindEvents();
    }

    function bindEvents() {
        document.getElementById('btn-pattern-search').addEventListener('click', function() {
            currentPage = 0;
            loadPatterns();
        });
        document.getElementById('pattern-search').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                currentPage = 0;
                loadPatterns();
            }
        });
    }

    function loadFilters() {
        fetch(API_BASE + '/categories')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var select = document.getElementById('pattern-category');
                data.forEach(function(cat) {
                    var opt = document.createElement('option');
                    opt.value = cat;
                    opt.textContent = cat;
                    select.appendChild(opt);
                });
            });

        fetch(API_BASE + '/dynasties')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var select = document.getElementById('pattern-dynasty');
                data.forEach(function(d) {
                    var opt = document.createElement('option');
                    opt.value = d;
                    opt.textContent = d;
                    select.appendChild(opt);
                });
            });

        fetch(API_BASE + '/statistics')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderStats(data);
            });
    }

    function renderStats(stats) {
        var container = document.getElementById('pattern-stats');
        var html = '<div class="stats-row">' +
            '<div class="stat-item"><span class="stat-value">' + stats.totalPatterns +
            '</span><span class="stat-label">纹样总数</span></div>' +
            '<div class="stat-item"><span class="stat-value">' +
            (stats.categories ? stats.categories.length : 0) +
            '</span><span class="stat-label">纹样分类</span></div>' +
            '<div class="stat-item"><span class="stat-value">' +
            (stats.dynasties ? stats.dynasties.length : 0) +
            '</span><span class="stat-label">历史朝代</span></div>' +
            '</div>';
        container.innerHTML = html;
    }

    function loadPatterns() {
        var keyword = document.getElementById('pattern-search').value;
        var category = document.getElementById('pattern-category').value;
        var dynasty = document.getElementById('pattern-dynasty').value;

        var url = API_BASE + '?page=' + currentPage + '&size=' + pageSize;
        if (keyword) url += '&keyword=' + encodeURIComponent(keyword);

        fetch(url)
            .then(function(r) { return r.json(); })
            .then(function(page) {
                renderPatterns(page.content);
                totalPages = page.totalPages;
                renderPagination();
            })
            .catch(function(err) {
                console.error('加载纹样失败:', err);
            });
    }

    function renderPatterns(patterns) {
        var grid = document.getElementById('patterns-grid');
        if (!patterns || patterns.length === 0) {
            grid.innerHTML = '<div class="empty-hint">暂无纹样数据</div>';
            return;
        }

        var html = patterns.map(function(p) {
            var colorCount = p.colorCount || 0;
            var complexity = p.complexityLevel || 0;
            var complexityPercent = Math.min(100, complexity);
            var tags = p.tags ? p.tags.split(/[,，]/).slice(0, 3).map(
                function(t) { return '<span class="tag">' + t.trim() + '</span>'; }
            ).join('') : '';

            return '<div class="pattern-card" data-id="' + p.id + '">' +
                '<div class="pattern-preview">' +
                    generatePatternPreview(p) +
                '</div>' +
                '<div class="pattern-card-body">' +
                    '<h4 class="pattern-name">' + p.name + '</h4>' +
                    '<div class="pattern-meta">' +
                        '<span class="meta-item">' + (p.category || '') + '</span>' +
                        '<span class="meta-item">' + (p.dynasty || '') + '</span>' +
                    '</div>' +
                    '<div class="pattern-tags">' + tags + '</div>' +
                    '<div class="pattern-footer">' +
                        '<span>🎨 ' + colorCount + '�?/span>' +
                        '<span>🔥 ' + (p.useCount || 0) + '</span>' +
                    '</div>' +
                    '<div class="pattern-complexity">' +
                        '<div class="complexity-bar">' +
                            '<div class="complexity-fill" style="width:' + complexityPercent + '%"></div>' +
                        '</div>' +
                        '<span>复杂�?' + complexity + '</span>' +
                    '</div>' +
                '</div>' +
                '<button class="btn-use-pattern" data-id="' + p.id + '">使用此纹�?/button>' +
            '</div>';
        }).join('');

        grid.innerHTML = html;

        grid.querySelectorAll('.btn-use-pattern').forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                var id = parseInt(btn.getAttribute('data-id'));
                usePattern(id);
            });
        });
    }

    function generatePatternPreview(p) {
        var warp = p.warpRepeat || 24;
        var weft = p.weftRepeat || 24;
        var colors = ['#8B4513', '#E8D4A8'];
        if (p.colorPalette) {
            var lines = p.colorPalette.split('\n');
            colors = lines.slice(0, 2).map(function(l) {
                var parts = l.split(',');
                return parts[1] || '#8B4513';
            });
        }

        var canvasData = 'data:image/svg+xml;utf8,' + encodeURIComponent(
            '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120">' +
            generateSvgPattern(warp, weft, colors) +
            '</svg>'
        );

        return '<div class="pattern-preview-svg" style="background-image:url(' + canvasData +
               ');width:100%;height:120px;background-size:cover"></div>';
    }

    function generateSvgPattern(warp, weft, colors) {
        var cellW = 120 / warp;
        var cellH = 120 / weft;
        var rows = Math.min(weft, 40);
        var cols = Math.min(warp, 40);
        var svg = '';

        for (var i = 0; i < rows; i++) {
            for (var j = 0; j < cols; j++) {
                var isWarp = (i + j) % 2 === 0;
                var color = isWarp ? colors[0] : colors[1];
                svg += '<rect x="' + (j * cellW) + '" y="' + (i * cellH) +
                       '" width="' + (cellW + 0.5) + '" height="' + (cellH + 0.5) +
                       '" fill="' + color + '"/>';
            }
        }
        return svg;
    }

    function renderPagination() {
        var container = document.getElementById('pattern-pagination');
        var html = '';

        if (currentPage > 0) {
            html += '<button class="page-btn" data-page="' + (currentPage - 1) + '">上一�?/button>';
        }

        for (var i = 0; i < totalPages; i++) {
            if (i === currentPage) {
                html += '<button class="page-btn active">' + (i + 1) + '</button>';
            } else if (Math.abs(i - currentPage) <= 2 || i === 0 || i === totalPages - 1) {
                html += '<button class="page-btn" data-page="' + i + '">' + (i + 1) + '</button>';
            } else if (Math.abs(i - currentPage) === 3) {
                html += '<span class="page-ellipsis">...</span>';
            }
        }

        if (currentPage < totalPages - 1) {
            html += '<button class="page-btn" data-page="' + (currentPage + 1) + '">下一�?/button>';
        }

        container.innerHTML = html;

        container.querySelectorAll('.page-btn[data-page]').forEach(function(btn) {
            btn.addEventListener('click', function() {
                currentPage = parseInt(btn.getAttribute('data-page'));
                loadPatterns();
            });
        });
    }

    function usePattern(patternId) {
        if (window.DesignStudio && typeof window.DesignStudio.loadPattern === 'function') {
            DesignStudio.loadPattern(patternId);
            switchTab('design');
        } else {
            alert('请前往虚拟织造页面使用此纹样');
            switchTab('design');
        }
    }

    function switchTab(tabName) {
        document.querySelectorAll('.tab-btn').forEach(function(t) {
            t.classList.remove('active');
        });
        document.querySelectorAll('.tab-content').forEach(function(c) {
            c.classList.remove('active');
        });
        document.querySelector('.tab-btn[data-tab="' + tabName + '"]').classList.add('active');
        document.getElementById('tab-' + tabName).classList.add('active');
    }

    function getPatternById(id) {
        return fetch(API_BASE + '/' + id).then(function(r) { return r.json(); });
    }

    function getPatternMatrix(id) {
        return fetch(API_BASE + '/' + id + '/matrix').then(function(r) { return r.json(); });
    }

    return {
        init: init,
        getPatternById: getPatternById,
        getPatternMatrix: getPatternMatrix
    };
})();
