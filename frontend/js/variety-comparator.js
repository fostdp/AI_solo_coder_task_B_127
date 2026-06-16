var VarietyComparator = (function() {
    var allVarieties = [];
    var selectedIds = [];
    var API_BASE = '/api/varieties';

    function init() {
        loadVarieties();
        bindEvents();
    }

    function bindEvents() {
        document.getElementById('variety-search').addEventListener('input', function(e) {
            filterVarieties(e.target.value);
        });
        document.getElementById('btn-clear-compare').addEventListener('click', clearComparison);
    }

    function loadVarieties() {
        fetch(API_BASE)
            .then(function(r) { return r.json(); })
            .then(function(data) {
                allVarieties = data;
                renderVarieties(allVarieties);
            })
            .catch(function(err) {
                console.error('加载品种失败:', err);
            });
    }

    function filterVarieties(keyword) {
        var filtered = allVarieties.filter(function(v) {
            if (!keyword) return true;
            var kw = keyword.toLowerCase();
            return v.name.toLowerCase().indexOf(kw) >= 0 ||
                   (v.alias && v.alias.toLowerCase().indexOf(kw) >= 0) ||
                   (v.description && v.description.toLowerCase().indexOf(kw) >= 0);
        });
        renderVarieties(filtered);
    }

    function renderVarieties(varieties) {
        var grid = document.getElementById('variety-grid');
        if (!varieties || varieties.length === 0) {
            grid.innerHTML = '<div class="empty-hint">暂无品种数据</div>';
            return;
        }

        var html = varieties.map(function(v) {
            var isSelected = selectedIds.indexOf(v.id) >= 0;
            var complexityStars = getStars(v.complexityScore, 100);
            return '<div class="variety-card ' + (isSelected ? 'selected' : '') +
                   '" data-id="' + v.id + '">' +
                '<div class="variety-card-header">' +
                    '<h4>' + v.name + '</h4>' +
                    '<span class="variety-badge">' + (v.dynasty || '传统') + '</span>' +
                '</div>' +
                '<div class="variety-info">' +
                    '<div><span class="label">类型:</span> ' + (v.weaveType || '-') + '</div>' +
                    '<div><span class="label">经纱�?</span> ' + (v.warpCount || '-') + ' �?/div>' +
                    '<div><span class="label">用色:</span> ' + (v.colorCount || '-') + ' �?/div>' +
                    '<div><span class="label">综框:</span> ' + (v.harnessCount || '-') + ' �?/div>' +
                    '<div><span class="label">日产�?</span> ' + (v.productionSpeedCmPerDay || '-') + ' cm</div>' +
                '</div>' +
                '<div class="variety-complexity">' +
                    '<span class="label">工艺复杂�?/span>' +
                    '<div class="stars">' + complexityStars + '</div>' +
                    '<span class="score">' + (v.complexityScore || 0) + '</span>' +
                '</div>' +
                '<div class="variety-desc">' + (v.description || '') + '</div>' +
                '<div class="variety-select-indicator">' +
                    (isSelected ? '�?已选择' : '点击选择对比') +
                '</div>' +
            '</div>';
        }).join('');

        grid.innerHTML = html;

        grid.querySelectorAll('.variety-card').forEach(function(card) {
            card.addEventListener('click', function() {
                var id = parseInt(card.getAttribute('data-id'));
                toggleSelection(id);
            });
        });
    }

    function getStars(score, max) {
        if (!score) score = 0;
        var fullStars = Math.floor(score / max * 5);
        var stars = '';
        for (var i = 0; i < 5; i++) {
            stars += i < fullStars ? '�? : '�?;
        }
        return stars;
    }

    function toggleSelection(id) {
        var idx = selectedIds.indexOf(id);
        if (idx >= 0) {
            selectedIds.splice(idx, 1);
        } else {
            if (selectedIds.length >= 5) {
                alert('最多选择5个品种进行对�?);
                return;
            }
            selectedIds.push(id);
        }

        renderVarieties(filteredList());
        renderSelected();

        if (selectedIds.length >= 2) {
            doComparison();
        } else {
            document.getElementById('comparison-result').classList.add('hidden');
            document.getElementById('comparison-radar').classList.add('hidden');
        }
    }

    function filteredList() {
        var kw = document.getElementById('variety-search').value;
        if (!kw) return allVarieties;
        return allVarieties.filter(function(v) {
            return v.name.toLowerCase().indexOf(kw.toLowerCase()) >= 0 ||
                   (v.alias && v.alias.toLowerCase().indexOf(kw.toLowerCase()) >= 0);
        });
    }

    function renderSelected() {
        var container = document.getElementById('comparison-selected');
        if (selectedIds.length === 0) {
            container.innerHTML = '<span class="empty-hint">点击上方品种卡片选择要对比的品种�?~5个）</span>';
            return;
        }

        var selected = allVarieties.filter(function(v) {
            return selectedIds.indexOf(v.id) >= 0;
        });

        var html = selected.map(function(v) {
            return '<span class="selected-tag">' + v.name +
                   ' <span class="remove-tag" data-id="' + v.id + '">×</span></span>';
        }).join('');

        container.innerHTML = html;

        container.querySelectorAll('.remove-tag').forEach(function(tag) {
            tag.addEventListener('click', function(e) {
                e.stopPropagation();
                var id = parseInt(tag.getAttribute('data-id'));
                toggleSelection(id);
            });
        });
    }

    function doComparison() {
        fetch(API_BASE + '/compare', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(selectedIds)
        }).then(function(r) { return r.json(); })
          .then(function(data) {
              renderComparisonResult(data);
              return fetch(API_BASE + '/compare/radar', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify(selectedIds)
              });
          }).then(function(r) { return r.json(); })
          .then(function(radarData) {
              document.getElementById('comparison-radar').classList.remove('hidden');
              drawRadarChart(radarData);
          })
          .catch(function(err) {
              console.error('对比分析失败:', err);
          });
    }

    function renderComparisonResult(data) {
        var container = document.getElementById('comparison-result');
        container.classList.remove('hidden');

        var matrix = data.comparisonMatrix;
        var varieties = data.varieties;

        var html = '<div class="comparison-table"><table>';
        html += '<thead><tr><th>对比�?/th>';
        varieties.forEach(function(v) {
            html += '<th>' + v.name + '</th>';
        });
        html += '</tr></thead><tbody>';

        for (var key in matrix) {
            if (matrix.hasOwnProperty(key)) {
                html += '<tr><td class="item-name">' + key + '</td>';
                matrix[key].forEach(function(val) {
                    html += '<td>' + (val !== null && val !== undefined ? val : '-') + '</td>';
                });
                html += '</tr>';
            }
        }
        html += '</tbody></table></div>';

        if (data.analysis) {
            html += '<div class="comparison-analysis">';
            html += '<h5>📊 对比分析</h5>';
            html += '<div class="analysis-grid">';
            for (var aKey in data.analysis) {
                if (data.analysis.hasOwnProperty(aKey)) {
                    html += '<div class="analysis-item">' +
                            '<span class="label">' + aKey + '</span>' +
                            '<span class="value">' + data.analysis[aKey] + '</span>' +
                            '</div>';
                }
            }
            html += '</div></div>';
        }

        container.innerHTML = html;
    }

    function drawRadarChart(data) {
        var canvas = document.getElementById('radar-canvas');
        var ctx = canvas.getContext('2d');
        var w = canvas.width;
        var h = canvas.height;
        var cx = w / 2;
        var cy = h / 2;
        var radius = Math.min(w, h) / 2 - 60;

        ctx.clearRect(0, 0, w, h);

        var dimensions = [];
        if (data.length > 0 && data[0].dimensions) {
            for (var key in data[0].dimensions) {
                if (data[0].dimensions.hasOwnProperty(key)) {
                    dimensions.push(key);
                }
            }
        }

        var numAxes = dimensions.length;
        var angleStep = (Math.PI * 2) / numAxes;

        for (var level = 1; level <= 5; level++) {
            var r = radius * level / 5;
            ctx.beginPath();
            for (var i = 0; i < numAxes; i++) {
                var angle = -Math.PI / 2 + i * angleStep;
                var x = cx + r * Math.cos(angle);
                var y = cy + r * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.strokeStyle = '#ddd';
            ctx.lineWidth = 1;
            ctx.stroke();
        }

        for (var i = 0; i < numAxes; i++) {
            var angle = -Math.PI / 2 + i * angleStep;
            var x = cx + radius * Math.cos(angle);
            var y = cy + radius * Math.sin(angle);
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(x, y);
            ctx.strokeStyle = '#ccc';
            ctx.lineWidth = 1;
            ctx.stroke();

            var labelX = cx + (radius + 20) * Math.cos(angle);
            var labelY = cy + (radius + 20) * Math.sin(angle);
            ctx.fillStyle = '#333';
            ctx.font = '12px sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(dimensions[i], labelX, labelY);
        }

        var colors = ['#E8D4A8', '#DC143C', '#1B4F91', '#2E8B57', '#FFD700'];
        data.forEach(function(item, idx) {
            var color = colors[idx % colors.length];
            ctx.beginPath();
            for (var i = 0; i < numAxes; i++) {
                var dim = dimensions[i];
                var val = item.dimensions[dim] || 0;
                var r = radius * val / 100;
                var angle = -Math.PI / 2 + i * angleStep;
                var x = cx + r * Math.cos(angle);
                var y = cy + r * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.fillStyle = color + '40';
            ctx.fill();
            ctx.strokeStyle = color;
            ctx.lineWidth = 2;
            ctx.stroke();
        });

        var legendY = 20;
        data.forEach(function(item, idx) {
            var color = colors[idx % colors.length];
            ctx.fillStyle = color;
            ctx.fillRect(20, legendY + idx * 20, 12, 12);
            ctx.fillStyle = '#333';
            ctx.font = '12px sans-serif';
            ctx.textAlign = 'left';
            ctx.fillText(item.name, 40, legendY + idx * 20 + 10);
        });
    }

    function clearComparison() {
        selectedIds = [];
        renderVarieties(filteredList());
        renderSelected();
        document.getElementById('comparison-result').classList.add('hidden');
        document.getElementById('comparison-radar').classList.add('hidden');
    }

    function getAllVarieties() {
        return allVarieties;
    }

    return {
        init: init,
        getAllVarieties: getAllVarieties
    };
})();
