package com.yunjin.system.virtual_weaving;

import com.yunjin.system.entity.UserWeavingDesign;
import com.yunjin.system.entity.PatternDesign;
import com.yunjin.system.entity.YunjinVariety;
import com.yunjin.system.repository.UserWeavingDesignRepository;
import com.yunjin.system.repository.PatternDesignRepository;
import com.yunjin.system.repository.YunjinVarietyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class VirtualWeavingService {

    private static final int CACHE_MAX_SIZE = 1000;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final Map<CacheKey, CacheEntry> matrixCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private static class CacheKey {
        private final Long designId;
        private final int version;

        CacheKey(Long designId, int version) {
            this.designId = designId;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return version == cacheKey.version &&
                    Objects.equals(designId, cacheKey.designId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(designId, version);
        }
    }

    private static class CacheEntry {
        final int[][] matrix;
        final long timestamp;
        final String weaveType;
        final int complexityHash;

        CacheEntry(int[][] matrix, long timestamp, String weaveType) {
            this.matrix = matrix;
            this.timestamp = timestamp;
            this.weaveType = weaveType;
            this.complexityHash = Arrays.deepHashCode(matrix);
        }
    }

    private final UserWeavingDesignRepository designRepository;
    private final PatternDesignRepository patternRepository;
    private final YunjinVarietyRepository varietyRepository;

    public VirtualWeavingService(UserWeavingDesignRepository designRepository,
                                  PatternDesignRepository patternRepository,
                                  YunjinVarietyRepository varietyRepository) {
        this.designRepository = designRepository;
        this.patternRepository = patternRepository;
        this.varietyRepository = varietyRepository;
    }

    public UserWeavingDesign createDesignFromVariety(Long varietyId, String designerName) {
        YunjinVariety variety = varietyRepository.findById(varietyId)
                .orElseThrow(() -> new IllegalArgumentException("云锦品种不存在"));

        UserWeavingDesign design = new UserWeavingDesign();
        design.setName("我的" + variety.getName() + "设计");
        design.setDescription("基于" + variety.getName() + "的自定义设计");
        design.setDesigner(designerName != null ? designerName : "匿名设计师");
        design.setBaseVarietyId(variety.getId());
        design.setBaseVarietyName(variety.getName());
        design.setWarpCount(variety.getWarpCount() != null ? variety.getWarpCount() : 120);
        design.setWeftCount(200);
        design.setColorCount(variety.getColorCount() != null ? variety.getColorCount() : 5);
        design.setColorPalette(variety.getPaletteColors());
        design.setStatus("draft");

        int[][] defaultMatrix = generateDefaultMatrix(
                design.getWarpCount(),
                design.getWeftCount(),
                variety.getWeaveType());
        design.setPatternMatrix(matrixToString(defaultMatrix));

        double complexity = calculateComplexity(defaultMatrix);
        design.setComplexityScore(complexity);
        design.setEstimatedProductionHours(estimateProductionHours(complexity, design.getWeftCount()));

        return designRepository.save(design);
    }

    public UserWeavingDesign createDesignFromPattern(Long patternId, String designerName) {
        PatternDesign pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new IllegalArgumentException("纹样不存在"));

        UserWeavingDesign design = new UserWeavingDesign();
        design.setName("基于" + pattern.getName() + "的设计");
        design.setDescription("从纹样库'" + pattern.getName() + "'创建的织造设计");
        design.setDesigner(designerName != null ? designerName : "匿名设计师");
        design.setPatternId(pattern.getId());
        design.setWarpCount(pattern.getWarpRepeat() != null ? pattern.getWarpRepeat() * 4 : 120);
        design.setWeftCount(pattern.getWeftRepeat() != null ? pattern.getWeftRepeat() * 4 : 120);
        design.setColorCount(pattern.getColorCount() != null ? pattern.getColorCount() : 5);
        design.setColorPalette(pattern.getColorPalette());
        design.setPatternMatrix(pattern.getPatternMatrix());
        design.setStatus("draft");

        if (pattern.getVarietyId() != null) {
            varietyRepository.findById(pattern.getVarietyId())
                    .ifPresent(v -> {
                        design.setBaseVarietyId(v.getId());
                        design.setBaseVarietyName(v.getName());
                    });
        }

        String matrixStr = pattern.getPatternMatrix();
        if (matrixStr != null && !matrixStr.isEmpty()) {
            int[][] matrix = parseMatrix(matrixStr);
            double complexity = calculateComplexity(matrix);
            design.setComplexityScore(complexity);
            design.setEstimatedProductionHours(estimateProductionHours(complexity, design.getWeftCount()));
        }

        return designRepository.save(design);
    }

    public UserWeavingDesign createBlankDesign(String designerName, int warpCount, int weftCount) {
        if (warpCount < 10 || warpCount > 5000) {
            throw new IllegalArgumentException("经纱数需在10~5000之间");
        }
        if (weftCount < 10 || weftCount > 10000) {
            throw new IllegalArgumentException("纬纱数需在10~10000之间");
        }

        UserWeavingDesign design = new UserWeavingDesign();
        design.setName("空白设计");
        design.setDescription("从零开始的织造设计");
        design.setDesigner(designerName != null ? designerName : "匿名设计师");
        design.setWarpCount(warpCount);
        design.setWeftCount(weftCount);
        design.setColorCount(5);
        design.setStatus("draft");

        int[][] defaultMatrix = generateDefaultMatrix(warpCount, weftCount, "plain");
        design.setPatternMatrix(matrixToString(defaultMatrix));

        design.setComplexityScore(calculateComplexity(defaultMatrix));
        design.setEstimatedProductionHours(estimateProductionHours(
                design.getComplexityScore(), weftCount));

        return designRepository.save(design);
    }

    public Optional<UserWeavingDesign> getDesignById(Long id) {
        return designRepository.findById(id);
    }

    public List<UserWeavingDesign> getDesignsByDesigner(String designer) {
        return designRepository.findByDesigner(designer);
    }

    public Page<UserWeavingDesign> getPublicDesigns(String keyword, int page, int size, String sortBy) {
        Pageable pageable = createPageable(page, size, sortBy);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return designRepository.searchPublicDesigns(keyword.trim(), pageable);
        }
        return designRepository.findByIsPublicTrue(pageable);
    }

    public List<UserWeavingDesign> getPopularDesigns(int limit) {
        return designRepository.findPopularDesigns(PageRequest.of(0, limit));
    }

    public List<UserWeavingDesign> getRecentDesigns(int limit) {
        return designRepository.findRecentDesigns(PageRequest.of(0, limit));
    }

    public int[][] getCachedMatrix(Long designId, int version) {
        CacheKey key = new CacheKey(designId, version);
        CacheEntry entry = matrixCache.get(key);
        if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS) {
            return entry.matrix;
        }
        return loadMatrixIntoCache(designId, version);
    }

    private int[][] loadMatrixIntoCache(Long designId, int version) {
        UserWeavingDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));
        String matrixStr = design.getPatternMatrix();
        int[][] matrix;
        if (matrixStr != null && !matrixStr.isEmpty()) {
            matrix = parseMatrix(matrixStr);
        } else {
            int warp = design.getWarpCount() != null ? design.getWarpCount() : 120;
            int weft = design.getWeftCount() != null ? design.getWeftCount() : 120;
            matrix = generateDefaultMatrix(warp, weft);
        }
        String weaveType = design.getBaseVarietyName() != null ? design.getBaseVarietyName() : "plain";
        cacheLock.writeLock().lock();
        try {
            if (matrixCache.size() >= CACHE_MAX_SIZE) {
                evictOldestEntries();
            }
            matrixCache.put(new CacheKey(designId, version),
                    new CacheEntry(matrix, System.currentTimeMillis(), weaveType));
        } finally {
            cacheLock.writeLock().unlock();
        }
        return matrix;
    }

    private void evictOldestEntries() {
        long threshold = System.currentTimeMillis() - CACHE_TTL_MS;
        matrixCache.entrySet().removeIf(e -> e.getValue().timestamp < threshold);
        if (matrixCache.size() >= CACHE_MAX_SIZE) {
            matrixCache.clear();
        }
    }

    public int invalidateCache(Long designId) {
        int removed = 0;
        Iterator<Map.Entry<CacheKey, CacheEntry>> it = matrixCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<CacheKey, CacheEntry> entry = it.next();
            if (entry.getKey().designId.equals(designId)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public Map<String, Object> getWebGLTextureData(Long designId, int version) {
        int[][] matrix = getCachedMatrix(designId, version);
        UserWeavingDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));

        int width = matrix[0].length;
        int height = matrix.length;

        byte[] rgbaData = new byte[width * height * 4];
        int[][] palette = parsePalette(design.getColorPalette());

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int colorIndex = matrix[i][j] % palette.length;
                int[] rgb = palette[colorIndex];
                int idx = (i * width + j) * 4;
                rgbaData[idx] = (byte) rgb[0];
                rgbaData[idx + 1] = (byte) rgb[1];
                rgbaData[idx + 2] = (byte) rgb[2];
                rgbaData[idx + 3] = (byte) 255;
            }
        }

        String base64Texture = Base64.getEncoder().encodeToString(rgbaData);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("designId", designId);
        result.put("textureWidth", width);
        result.put("textureHeight", height);
        result.put("rgbaData", base64Texture);
        result.put("warpCount", design.getWarpCount());
        result.put("weftCount", design.getWeftCount());
        result.put("paletteSize", palette.length);
        result.put("fromCache", true);
        return result;
    }

    public Map<String, Object> simulateWeavingBatch(Long designId, int batchSize, int steps) {
        UserWeavingDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));
        int warp = design.getWarpCount() != null ? design.getWarpCount() : 120;
        int weft = design.getWeftCount() != null ? design.getWeftCount() : 120;
        int totalSteps = warp * weft;
        if (steps > totalSteps) steps = totalSteps;
        int batches = (int) Math.ceil((double) steps / batchSize);

        List<Map<String, Object>> batchResults = new ArrayList<>();
        int currentStep = 0;
        int[][] matrix = getCachedMatrix(designId, 0);
        double baseTension = 0.5;
        Random random = new Random();

        for (int b = 0; b < batches; b++) {
            int batchSteps = Math.min(batchSize, steps - currentStep);
            double[] tensions = new double[warp];
            int startRow = (currentStep / warp) % weft;
            int endRow = ((currentStep + batchSteps - 1) / warp) % weft;

            for (int i = 0; i < warp; i++) {
                double factor = 1.0;
                for (int r = startRow; r <= endRow; r++) {
                    if (matrix[r][i] == 1) factor += 0.02;
                }
                double noise = (random.nextDouble() - 0.5) * 0.05;
                tensions[i] = Math.min(6.0, Math.max(0.1, baseTension * factor + noise));
            }

            currentStep += batchSteps;
            double progress = Math.min(100.0, (double) currentStep / totalSteps * 100.0);

            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("batchIndex", b);
            batch.put("stepsCompleted", currentStep);
            batch.put("progress", Math.round(progress * 100.0) / 100.0);
            batch.put("tensions", tensions);
            batch.put("avgTension", Math.round(Arrays.stream(tensions).average().orElse(0) * 100.0) / 100.0);
            batchResults.add(batch);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("designId", designId);
        result.put("totalBatches", batches);
        result.put("batchSize", batchSize);
        result.put("totalSteps", steps);
        result.put("finalProgress", Math.min(100.0, (double) currentStep / totalSteps * 100.0));
        result.put("batches", batchResults);
        return result;
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("cacheSize", matrixCache.size());
        stats.put("maxCacheSize", CACHE_MAX_SIZE);
        stats.put("cacheTTLSeconds", CACHE_TTL_MS / 1000);
        return stats;
    }

    private int[][] parsePalette(String paletteStr) {
        if (paletteStr == null || paletteStr.trim().isEmpty()) {
            return new int[][]{{139, 69, 19}, {232, 212, 168}, {205, 133, 63}, {255, 215, 0}, {178, 34, 34}};
        }
        String[] lines = paletteStr.split("\\n");
        List<int[]> colors = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                try {
                    int[] rgb = hexToRgb(parts[1].trim());
                    if (rgb != null) colors.add(rgb);
                } catch (Exception ignored) {}
            }
        }
        if (colors.isEmpty()) {
            return new int[][]{{139, 69, 19}, {232, 212, 168}};
        }
        return colors.toArray(new int[0][]);
    }

    private int[] hexToRgb(String hex) {
        String h = hex.replace("#", "");
        if (h.length() == 3) {
            h = String.valueOf(h.charAt(0)) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        }
        try {
            return new int[]{Integer.parseInt(h.substring(0, 2), 16),
                           Integer.parseInt(h.substring(2, 4), 16),
                           Integer.parseInt(h.substring(4, 6), 16)};
        } catch (Exception e) {
            return null;
        }
    }

    private Pageable createPageable(int page, int size, String sortBy) {
        int safeSize = Math.min(size, 100);
        Sort sort = switch (sortBy == null ? "" : sortBy) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "complex" -> Sort.by(Sort.Direction.DESC, "complexityScore");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(page, safeSize, sort);
    }

    public UserWeavingDesign updateDesign(Long id, UserWeavingDesign design) {
        UserWeavingDesign existing = designRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));

        existing.setName(design.getName());
        existing.setDescription(design.getDescription());
        existing.setPatternMatrix(design.getPatternMatrix());
        existing.setColorPalette(design.getColorPalette());
        existing.setColorCount(design.getColorCount());
        existing.setWarpCount(design.getWarpCount());
        existing.setWeftCount(design.getWeftCount());
        existing.setTags(design.getTags());
        existing.setNotes(design.getNotes());
        existing.setDesignLayers(design.getDesignLayers());
        existing.setThumbnailUrl(design.getThumbnailUrl());

        if (design.getPatternMatrix() != null) {
            int[][] matrix = parseMatrix(design.getPatternMatrix());
            existing.setComplexityScore(calculateComplexity(matrix));
            existing.setEstimatedProductionHours(estimateProductionHours(
                    existing.getComplexityScore(),
                    design.getWeftCount() != null ? design.getWeftCount() : existing.getWeftCount()));
        }

        return designRepository.save(existing);
    }

    public void deleteDesign(Long id) {
        if (!designRepository.existsById(id)) {
            throw new IllegalArgumentException("设计不存在");
        }
        designRepository.deleteById(id);
    }

    public UserWeavingDesign publishDesign(Long id) {
        UserWeavingDesign design = designRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));
        design.setIsPublic(true);
        design.setStatus("published");
        return designRepository.save(design);
    }

    public UserWeavingDesign unpublishDesign(Long id) {
        UserWeavingDesign design = designRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));
        design.setIsPublic(false);
        design.setStatus("draft");
        return designRepository.save(design);
    }

    public void incrementLikeCount(Long id) {
        designRepository.findById(id).ifPresent(d -> {
            d.setLikeCount(d.getLikeCount() == null ? 1 : d.getLikeCount() + 1);
            designRepository.save(d);
        });
    }

    public void incrementViewCount(Long id) {
        designRepository.findById(id).ifPresent(d -> {
            d.setViewCount(d.getViewCount() == null ? 1 : d.getViewCount() + 1);
            designRepository.save(d);
        });
    }

    public Map<String, Object> simulateWeaving(Long designId, int steps) {
        UserWeavingDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));

        int[][] fullMatrix = parseMatrix(design.getPatternMatrix());
        int totalWeft = fullMatrix.length;

        int currentWeft = Math.min(steps, totalWeft);
        int[][] wovenMatrix = Arrays.copyOfRange(fullMatrix, 0, currentWeft);

        double[] warpTension = computeTensionFromDesign(design, wovenMatrix);

        int breakCount = countBreaks(warpTension);

        double progress = (double) currentWeft / totalWeft * 100;
        int estimatedHours = design.getEstimatedProductionHours() != null
                ? design.getEstimatedProductionHours() : 100;
        double estimatedTimeHours = (double) estimatedHours * currentWeft / totalWeft;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("designId", design.getId());
        result.put("designName", design.getName());
        result.put("steps", currentWeft);
        result.put("totalWeft", totalWeft);
        result.put("progressPercent", Math.round(progress * 100.0) / 100.0);
        result.put("wovenMatrix", wovenMatrix);
        result.put("warpTension", warpTension);
        result.put("warpBreakCount", breakCount);
        result.put("estimatedTimeHours", Math.round(estimatedTimeHours * 100.0) / 100.0);
        result.put("averageTension", computeAverage(warpTension));

        return result;
    }

    public Map<String, Object> generateWeavingPreview(Long designId) {
        UserWeavingDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new IllegalArgumentException("设计不存在"));

        int[][] matrix = parseMatrix(design.getPatternMatrix());
        int warpCount = design.getWarpCount() != null ? design.getWarpCount() : 120;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("designId", design.getId());
        result.put("designName", design.getName());
        result.put("warpCount", warpCount);
        result.put("weftCount", matrix.length);
        result.put("colorCount", design.getColorCount());
        result.put("complexityScore", design.getComplexityScore());
        result.put("estimatedProductionHours", design.getEstimatedProductionHours());

        int floatLength = calculateAverageFloatLength(matrix);
        result.put("averageFloatLength", floatLength);

        boolean isBalanced = checkWeaveBalance(matrix);
        result.put("isBalancedWeave", isBalanced);

        double coverage = calculateCoverage(matrix);
        result.put("warpCoveragePercent", Math.round(coverage * 10000.0) / 100.0);

        int[] patternRepeat = detectPatternRepeat(matrix);
        result.put("detectedPatternRepeat", patternRepeat);

        return result;
    }

    private int[][] generateDefaultMatrix(int warp, int weft, String weaveType) {
        int[][] matrix = new int[weft][warp];
        if ("twill".equalsIgnoreCase(weaveType)) {
            for (int i = 0; i < weft; i++) {
                for (int j = 0; j < warp; j++) {
                    matrix[i][j] = ((i - j + warp) % 4 < 2) ? 1 : 0;
                }
            }
        } else if ("satin".equalsIgnoreCase(weaveType)) {
            for (int i = 0; i < weft; i++) {
                for (int j = 0; j < warp; j++) {
                    matrix[i][j] = ((j + 3 * i) % 8 == 0) ? 1 : 0;
                }
            }
        } else {
            for (int i = 0; i < weft; i++) {
                for (int j = 0; j < warp; j++) {
                    matrix[i][j] = (i + j) % 2;
                }
            }
        }
        return matrix;
    }

    private String matrixToString(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]);
                if (j < matrix[i].length - 1) sb.append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private int[][] parseMatrix(String str) {
        if (str == null || str.trim().isEmpty()) return new int[0][0];
        String[] lines = str.trim().split("\\n");
        List<int[]> rows = new ArrayList<>();
        for (String line : lines) {
            String[] cells = line.trim().split("[,\\s]+");
            int[] row = new int[cells.length];
            for (int i = 0; i < cells.length; i++) {
                try {
                    row[i] = Integer.parseInt(cells[i].trim());
                } catch (NumberFormatException e) {
                    row[i] = 0;
                }
            }
            rows.add(row);
        }
        return rows.toArray(new int[0][]);
    }

    private double calculateComplexity(int[][] matrix) {
        if (matrix.length == 0 || matrix[0].length == 0) return 0.0;

        int transitions = 0;
        for (int[] row : matrix) {
            for (int j = 1; j < row.length; j++) {
                if (row[j] != row[j - 1]) transitions++;
            }
        }
        for (int j = 0; j < matrix[0].length; j++) {
            for (int i = 1; i < matrix.length; i++) {
                if (matrix[i][j] != matrix[i - 1][j]) transitions++;
            }
        }

        double total = (double) matrix.length * matrix[0].length * 2;
        return Math.round((transitions / total * 100) * 100.0) / 100.0;
    }

    private int estimateProductionHours(double complexity, int weftCount) {
        double baseSpeed = 8;
        double complexityFactor = 1 + complexity / 50;
        double hours = weftCount / baseSpeed * complexityFactor / 60;
        return (int) Math.ceil(hours);
    }

    private double[] computeTensionFromDesign(UserWeavingDesign design, int[][] woven) {
        int warpCount = design.getWarpCount() != null ? design.getWarpCount() : 120;
        double[] tension = new double[warpCount];
        double baseTension = 2.2;

        for (int w = 0; w < warpCount; w++) {
            double posFactor = 1 + 0.05 * Math.sin(6 * Math.PI * w / warpCount);
            double wearFactor = 1 + 0.03 * Math.log1p(w);

            int interlacements = 0;
            for (int i = 0; i < woven.length; i++) {
                if (w < woven[i].length && woven[i][w] == 1) interlacements++;
            }
            double interlaceFactor = 1 + 0.1 * (double) interlacements / Math.max(1, woven.length);

            tension[w] = baseTension * posFactor * wearFactor * interlaceFactor;
            tension[w] += (Math.random() - 0.5) * 0.1;
            tension[w] = Math.max(0.1, Math.min(6.0, tension[w]));
        }

        return tension;
    }

    private int countBreaks(double[] tension) {
        int count = 0;
        for (double t : tension) {
            if (t < 0.1) count++;
        }
        return count;
    }

    private double computeAverage(double[] arr) {
        if (arr.length == 0) return 0;
        double sum = 0;
        for (double v : arr) sum += v;
        return Math.round(sum / arr.length * 100.0) / 100.0;
    }

    private int calculateAverageFloatLength(int[][] matrix) {
        if (matrix.length == 0 || matrix[0].length == 0) return 0;
        int totalFloats = 0;
        int count = 0;

        for (int[] row : matrix) {
            int currentFloat = 1;
            for (int j = 1; j < row.length; j++) {
                if (row[j] == row[j - 1]) {
                    currentFloat++;
                } else {
                    totalFloats += currentFloat;
                    count++;
                    currentFloat = 1;
                }
            }
            totalFloats += currentFloat;
            count++;
        }

        return count > 0 ? totalFloats / count : 0;
    }

    private boolean checkWeaveBalance(int[][] matrix) {
        if (matrix.length == 0 || matrix[0].length == 0) return true;
        int warpFloats = 0;
        int weftFloats = 0;
        for (int[] row : matrix) {
            for (int cell : row) {
                if (cell == 1) warpFloats++;
                else weftFloats++;
            }
        }
        double ratio = (double) warpFloats / weftFloats;
        return ratio > 0.5 && ratio < 2.0;
    }

    private double calculateCoverage(int[][] matrix) {
        if (matrix.length == 0 || matrix[0].length == 0) return 0;
        int count = 0;
        int total = 0;
        for (int[] row : matrix) {
            for (int cell : row) {
                if (cell == 1) count++;
                total++;
            }
        }
        return (double) count / total;
    }

    private int[] detectPatternRepeat(int[][] matrix) {
        if (matrix.length < 2 || matrix[0].length < 2) {
            return new int[]{matrix[0].length, matrix.length};
        }

        int warpRepeat = matrix[0].length;
        for (int period = 1; period <= matrix[0].length / 2; period++) {
            boolean matches = true;
            for (int i = 0; i < matrix.length && matches; i++) {
                for (int j = 0; j < matrix[i].length - period; j++) {
                    if (matrix[i][j] != matrix[i][j + period]) {
                        matches = false;
                        break;
                    }
                }
            }
            if (matches) {
                warpRepeat = period;
                break;
            }
        }

        int weftRepeat = matrix.length;
        for (int period = 1; period <= matrix.length / 2; period++) {
            boolean matches = true;
            for (int i = 0; i < matrix.length - period && matches; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (matrix[i][j] != matrix[i + period][j]) {
                        matches = false;
                        break;
                    }
                }
            }
            if (matches) {
                weftRepeat = period;
                break;
            }
        }

        return new int[]{warpRepeat, weftRepeat};
    }

    public Map<String, Object> getDesignStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDesigns", designRepository.count());

        List<UserWeavingDesign> popular = getPopularDesigns(5);
        stats.put("top5Popular", popular.stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(),
                        "designer", d.getDesigner(), "likes", d.getLikeCount()))
                .toList());

        List<UserWeavingDesign> recent = getRecentDesigns(5);
        stats.put("recent5", recent.stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(),
                        "designer", d.getDesigner(), "createdAt", d.getCreatedAt()))
                .toList());

        return stats;
    }
}
