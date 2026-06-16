var ColorAnalyzer = (function() {
    var allPalettes = [];
    var selectedTradId = null;
    var selectedDigitalId = null;
    var API_BASE = '/api/colors';

    function init() {
        loadPalettes();
        bindEvents();
    }

    function bindEvents() {
        document.getElementById('trad-palette-select').addEventListener('change', function(e) {
            selectedTradId = parseInt(e.target.value);
            updateComparison();
        });
        document.getElementById('digital-palette-select').addEventListener('change', function(e) {
            selectedDigitalId = parseInt(e.target.value);
            updateComparison();
        });
        document.getElementById('color-picker').addEventListener('input', function(e) {
            matchTraditionalColor(e.target.value);
        });
    }

    function loadPalettes() {
        fetch(API_BASE + '/palettes')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                allPalettes = data;
                populateSelects(data);
                if (data.length > 0) {
                    var trad = data.find(function(p) { return p.paletteType === 'traditional'; });
                    var digital = data.find(function(p) { return p.paletteType === 'digital_printing'; });
                    if (trad) {
                        document.getElementById('trad-palette-select').value = trad.id;
                        selectedTradId = trad.id;
                    }
                    if (digital) {
                        document.getElementById('digital-palette-select').value = digital.id;
                        selectedDigitalId = digital.id;
                    }
                    if (trad) {
                        renderPaletteColors(trad, 'traditional');
                    }
                    updateComparison();
                }
            });
    }

    function populateSelects(palettes) {
        var tradSelect = document.getElementById('trad-palette-select');
        var digitalSelect = document.getElementById('digital-palette-select');

        palettes.forEach(function(p) {
            var opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = p.name + ' (' + p.colorCount + '�?';

            if (p.paletteType === 'traditional') {
                tradSelect.appendChild(opt.cloneNode(true));
            } else {
                digitalSelect.appendChild(opt.cloneNode(true));
            }
        });
    }

    function updateComparison() {
        if (!selectedTradId || !selectedDigitalId) return;

        fetch(API_BASE + '/compare?paletteId1=' + selectedTradId + '&paletteId2=' + selectedDigitalId)
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderColorSwatches(data);
                renderColorStats(data.stats);
                drawGamutComparison(data);
            })
            .catch(function(err) {
                console.error('色彩对比失败:', err);
            });

        var tradPalette = allPalettes.find(function(p) { return p.id === selectedTradId; });
        if (tradPalette) {
            renderCulturalNotes(tradPalette);
        }

        fetch(API_BASE + '/digital-comparison/' + selectedTradId)
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderDigitalComparison(data);
            })
            .catch(function() {});
    }

    function renderPaletteColors(palette, type) {
        var colors = parseColors(palette.colors);
        var html = '<h5>' + palette.name + '</h5>';
        html += '<div class="swatch-grid">';
        colors.forEach(function(c) {
            html += '<div class="swatch-item" title="' + c.name + '">' +
                    '<div class="swatch-color" style="background:' + c.hex + '"></div>' +
                    '<span class="swatch-name">' + c.name + '</span>' +
                    '</div>';
        });
        html += '</div>';
    }

    function renderColorSwatches(data) {
        var container = document.getElementById('color-swatches');

        var similarColors = data.similarColors || [];
        var html = '<div class="swatch-section">';
        html += '<h4>相近色彩匹配 (' + similarColors.length + ' �?</h4>';
        html += '<div class="similar-color-grid">';

        similarColors.slice(0, 10).forEach(function(pair) {
            var c1 = pair.color1;
            var c2 = pair.color2;
            html += '<div class="similar-color-pair">' +
                    '<div class="swatch-color large" style="background:' + c1.hex + '">' +
                    '<span class="swatch-label">' + c1.name + '</span></div>' +
                    '<div class="similar-arrow">�?/div>' +
                    '<div class="swatch-color large" style="background:' + c2.hex + '">' +
                    '<span class="swatch-label">' + c2.name + '</span></div>' +
                    '<div class="similarity-score">' + pair.similarity.toFixed(1) + '%</div>' +
                    '</div>';
        });

        html += '</div></div>';
        container.innerHTML = html;
    }

    function renderColorStats(stats) {
        var container = document.getElementById('color-stats');
        if (!stats) return;

        var html = '';
        var labels = {
            avgBrightness1: '传统平均亮度',
            avgBrightness2: '数码平均亮度',
            brightnessDiff: '亮度差异',
            avgSaturation1: '传统平均饱和�?,
            avgSaturation2: '数码平均饱和�?,
            warmColorRatio1: '传统暖色占比',
            warmColorRatio2: '数码暖色占比'
        };

        for (var key in stats) {
            if (stats.hasOwnProperty(key)) {
                html += '<div class="color-stat-item">' +
                        '<span class="stat-label">' + (labels[key] || key) + '</span>' +
                        '<span class="stat-value">' + stats[key] + '</span>' +
                        '</div>';
            }
        }

        container.innerHTML = html;
    }

    function drawGamutComparison(data) {
        var canvas = document.getElementById('color-gamut-canvas');
        var ctx = canvas.getContext('2d');
        var w = canvas.width;
        var h = canvas.height;

        ctx.clearRect(0, 0, w, h);

        var gamut1 = data.gamut1 || [0, 100, -128, 127, -128, 127];
        var gamut2 = data.gamut2 || [0, 100, -128, 127, -128, 127];

        var cx = w / 2;
        var cy = h / 2;

        ctx.fillStyle = '#f5f5f5';
        ctx.fillRect(0, 0, w, h);

        var scale = 1.2;
        var radius = Math.min(w, h) / 2.5 * scale;

        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(100, 149, 237, 0.3)';
        ctx.fill();
        ctx.strokeStyle = '#4169E1';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.fillStyle = '#4169E1';
        ctx.font = '12px sans-serif';
        ctx.fillText('数码印花色域', cx + radius - 20, cy - radius + 20);

        var innerRadius = radius * 0.6;
        ctx.beginPath();
        ctx.arc(cx, cy, innerRadius, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(220, 20, 60, 0.3)';
        ctx.fill();
        ctx.strokeStyle = '#DC143C';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.fillStyle = '#DC143C';
        ctx.fillText('传统云锦色域', cx + innerRadius - 20, cy - innerRadius + 20);

        var centerColor = '#F5E6D3';
        var gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, innerRadius);
        gradient.addColorStop(0, 'rgba(255, 230, 150, 0.6)');
        gradient.addColorStop(1, 'rgba(220, 20, 60, 0.2)');
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(cx, cy, innerRadius, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#333';
        ctx.font = '14px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('色域对比示意�?(a*b*平面投影)', cx, h - 20);
        ctx.textAlign = 'left';
    }

    function renderCulturalNotes(palette) {
        var container = document.getElementById('color-cultural');
        var html = '';

        if (palette.culturalNotes) {
            html += '<p class="cultural-desc">' + palette.culturalNotes + '</p>';
        }

        if (palette.source) {
            html += '<p><strong>来源:</strong> ' + palette.source + '</p>';
        }
        if (palette.dynasty) {
            html += '<p><strong>朝代:</strong> ' + palette.dynasty + '</p>';
        }
        if (palette.description) {
            html += '<p><strong>描述:</strong> ' + palette.description + '</p>';
        }

        container.innerHTML = html || '<p>选择色卡查看色彩文化解读</p>';
    }

    function renderDigitalComparison(data) {
        var container = document.getElementById('digital-comparison-table');
        var points = data.comparisonPoints || [];

        var html = '';
        points.forEach(function(p) {
            html += '<div class="compare-point-card">' +
                    '<h5>' + p.title + '</h5>' +
                    '<div class="compare-row">' +
                    '<div class="compare-col traditional">' +
                    '<span class="col-label">传统云锦</span>' +
                    '<span class="col-value">' + p.traditional + '</span>' +
                    '</div>' +
                    '<div class="compare-col digital">' +
                    '<span class="col-label">现代数码</span>' +
                    '<span class="col-value">' + p.digital + '</span>' +
                    '</div>' +
                    '</div>' +
                    '<p class="compare-note">' + p.note + '</p>' +
                    '</div>';
        });

        if (data.sampleColorMappings) {
            html += '<div class="sample-mapping">';
            html += '<h5>代表性色彩映射示�?/h5>';
            html += '<div class="mapping-row">';
            data.sampleColorMappings.forEach(function(m) {
                html += '<div class="mapping-item">' +
                        '<div class="swatch-color" style="background:' + m.hex + '"></div>' +
                        '<span class="mapping-name">' + m.name + '</span>' +
                        '<span class="mapping-score">还原�?' + m.reproductionScore + '%</span>' +
                        '</div>';
            });
            html += '</div></div>';
        }

        container.innerHTML = html;
    }

    function matchTraditionalColor(hex) {
        var rgb = hexToRgb(hex);
        fetch(API_BASE + '/match?r=' + rgb.r + '&g=' + rgb.g + '&b=' + rgb.b)
            .then(function(r) { return r.json(); })
            .then(function(data) {
                renderMatchResult(data);
            })
            .catch(function(err) {
                console.error('配色失败:', err);
            });
    }

    function renderMatchResult(data) {
        var container = document.getElementById('color-match-result');

        if (!data.matchedColor) {
            container.innerHTML = '<span>未找到匹配的传统色彩</span>';
            return;
        }

        var c = data.matchedColor;
        var html = '<div class="match-result">' +
                '<div class="swatch-color large" style="background:' + c.hex + '"></div>' +
                '<div class="match-info">' +
                '<h5>' + c.name + '</h5>' +
                '<p><span class="label">色�?</span> ' + c.hex + '</p>' +
                '<p><span class="label">相似�?</span> ' + data.similarity + '%</p>' +
                '</div>' +
                '</div>';

        if (data.matchedPalette) {
            html += '<p class="match-palette">出自: ' + data.matchedPalette.name +
                    ' (' + data.matchedPalette.dynasty + ')</p>';
        }

        container.innerHTML = html;
    }

    function parseColors(colorsStr) {
        if (!colorsStr) return [];
        var lines = colorsStr.split('\n');
        var result = [];
        lines.forEach(function(line) {
            var parts = line.split(',');
            if (parts.length >= 2) {
                result.push({
                    name: parts[0].trim(),
                    hex: parts[1].trim()
                });
            }
        });
        return result;
    }

    function hexToRgb(hex) {
        var h = hex.replace('#', '');
        return {
            r: parseInt(h.substring(0, 2), 16),
            g: parseInt(h.substring(2, 4), 16),
            b: parseInt(h.substring(4, 6), 16)
        };
    }

    return {
        init: init
    };
})();
