package com.yunjin.system.pattern_library;

import com.yunjin.system.entity.PatternDesign;
import com.yunjin.system.repository.PatternDesignRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatternLibraryService {

    private final PatternDesignRepository patternRepository;

    public PatternLibraryService(PatternDesignRepository patternRepository) {
        this.patternRepository = patternRepository;
    }

    public Page<PatternDesign> searchPatterns(String keyword, int page, int size, String sortBy) {
        Pageable pageable = createPageable(page, size, sortBy);
        if (keyword == null || keyword.trim().isEmpty()) {
            return patternRepository.findAll(pageable);
        }
        return patternRepository.searchByKeyword(keyword.trim(), pageable);
    }

    public Page<PatternDesign> advancedSearch(String category, String dynasty,
                                              String weaveStructure,
                                              Integer minComplexity, Integer maxComplexity,
                                              int page, int size, String sortBy) {
        Pageable pageable = createPageable(page, size, sortBy);
        return patternRepository.advancedSearch(category, dynasty, weaveStructure,
                minComplexity, maxComplexity, pageable);
    }

    private Pageable createPageable(int page, int size, String sortBy) {
        int safeSize = Math.min(size, 100);
        Sort sort = switch (sortBy == null ? "" : sortBy) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "useCount");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "complexity" -> Sort.by(Sort.Direction.DESC, "complexityLevel");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(page, safeSize, sort);
    }

    public Optional<PatternDesign> getPatternById(Long id) {
        return patternRepository.findById(id);
    }

    public Optional<PatternDesign> getPatternByCode(String code) {
        return patternRepository.findByPatternCode(code);
    }

    public List<PatternDesign> getPatternsByCategory(String category) {
        return patternRepository.findByCategory(category);
    }

    public List<PatternDesign> getPatternsByVariety(Long varietyId) {
        return patternRepository.findByVarietyId(varietyId);
    }

    public List<PatternDesign> getPopularPatterns(int limit) {
        return patternRepository.findPopularPatterns(PageRequest.of(0, limit));
    }

    public List<String> getAllCategories() {
        return patternRepository.findAllCategories();
    }

    public List<String> getAllDynasties() {
        return patternRepository.findAllDynasties();
    }

    public List<String> getAllWeaveStructures() {
        return patternRepository.findAllWeaveStructures();
    }

    public List<String> getAllSymmetryTypes() {
        return patternRepository.findAllSymmetryTypes();
    }

    public PatternDesign createPattern(PatternDesign pattern) {
        if (pattern.getPatternCode() != null &&
            patternRepository.findByPatternCode(pattern.getPatternCode()).isPresent()) {
            throw new IllegalArgumentException("纹样编码已存在");
        }
        if (pattern.getIsPublic() == null) {
            pattern.setIsPublic(true);
        }
        return patternRepository.save(pattern);
    }

    public PatternDesign updatePattern(Long id, PatternDesign pattern) {
        PatternDesign existing = patternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("纹样不存在"));

        existing.setName(pattern.getName());
        existing.setAlias(pattern.getAlias());
        existing.setCategory(pattern.getCategory());
        existing.setSubCategory(pattern.getSubCategory());
        existing.setDynasty(pattern.getDynasty());
        existing.setOrigin(pattern.getOrigin());
        existing.setDescription(pattern.getDescription());
        existing.setCulturalMeaning(pattern.getCulturalMeaning());
        existing.setWeaveStructure(pattern.getWeaveStructure());
        existing.setWarpRepeat(pattern.getWarpRepeat());
        existing.setWeftRepeat(pattern.getWeftRepeat());
        existing.setColorCount(pattern.getColorCount());
        existing.setTags(pattern.getTags());
        existing.setPatternMatrix(pattern.getPatternMatrix());
        existing.setColorPalette(pattern.getColorPalette());
        existing.setComplexityLevel(pattern.getComplexityLevel());
        existing.setSymmetryScore(pattern.getSymmetryScore());
        existing.setSymmetryType(pattern.getSymmetryType());
        existing.setRepresentativeWorks(pattern.getRepresentativeWorks());
        existing.setImageUrl(pattern.getImageUrl());
        existing.setThumbnailUrl(pattern.getThumbnailUrl());
        existing.setVarietyId(pattern.getVarietyId());

        return patternRepository.save(existing);
    }

    public void deletePattern(Long id) {
        if (!patternRepository.existsById(id)) {
            throw new IllegalArgumentException("纹样不存在");
        }
        patternRepository.deleteById(id);
    }

    public void incrementUseCount(Long id) {
        patternRepository.findById(id).ifPresent(p -> {
            p.setUseCount(p.getUseCount() == null ? 1 : p.getUseCount() + 1);
            patternRepository.save(p);
        });
    }

    public void incrementLikeCount(Long id) {
        patternRepository.findById(id).ifPresent(p -> {
            p.setLikeCount(p.getLikeCount() == null ? 1 : p.getLikeCount() + 1);
            patternRepository.save(p);
        });
    }

    public Map<String, Object> findSimilarPatternsWithScores(Long patternId, int limit) {
        PatternDesign target = patternRepository.findById(patternId)
                .orElseThrow(() -> new IllegalArgumentException("纹样不存在"));

        List<PatternDesign> allPatterns = patternRepository.findAll();
        Set<String> targetTags = parseTags(target.getTags());

        List<Map<String, Object>> scored = allPatterns.stream()
                .filter(p -> !p.getId().equals(patternId))
                .filter(p -> Boolean.TRUE.equals(p.getIsPublic()))
                .map(p -> {
                    double sim = calculateSimilarity(target, p, targetTags);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pattern", p);
                    item.put("similarityScore", sim);
                    return item;
                })
                .sorted((a, b) -> Double.compare(
                        (double) b.get("similarityScore"),
                        (double) a.get("similarityScore")))
                .limit(limit)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetPattern", target);
        result.put("similarPatterns", scored);
        result.put("totalCandidates", allPatterns.size() - 1);
        return result;
    }

    public List<PatternDesign> findSimilarPatterns(Long patternId, int limit) {
        PatternDesign target = patternRepository.findById(patternId)
                .orElseThrow(() -> new IllegalArgumentException("纹样不存在"));

        List<PatternDesign> allPatterns = patternRepository.findAll();
        Set<String> targetTags = parseTags(target.getTags());

        List<PatternDesign> scored = allPatterns.stream()
                .filter(p -> !p.getId().equals(patternId))
                .filter(p -> Boolean.TRUE.equals(p.getIsPublic()))
                .map(p -> new AbstractMap.SimpleEntry<>(p, calculateSimilarity(target, p, targetTags)))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return scored;
    }

    private double calculateSimilarity(PatternDesign target, PatternDesign candidate,
                                       Set<String> targetTags) {
        double score = 0.0;
        double maxScore = 0.0;

        if (Objects.equals(target.getCategory(), candidate.getCategory())) {
            score += 20;
        }
        maxScore += 20;

        if (Objects.equals(target.getWeaveStructure(), candidate.getWeaveStructure())) {
            score += 15;
        }
        maxScore += 15;

        if (Objects.equals(target.getDynasty(), candidate.getDynasty())) {
            score += 12;
        }
        maxScore += 12;

        if (Objects.equals(target.getSymmetryType(), candidate.getSymmetryType())) {
            score += 8;
        }
        maxScore += 8;

        Set<String> candidateTags = parseTags(candidate.getTags());
        if (!targetTags.isEmpty() && !candidateTags.isEmpty()) {
            Set<String> intersection = new HashSet<>(targetTags);
            intersection.retainAll(candidateTags);
            Set<String> union = new HashSet<>(targetTags);
            union.addAll(candidateTags);
            double tagSimilarity = (double) intersection.size() / union.size();
            score += tagSimilarity * 25;
        }
        maxScore += 25;

        if (target.getComplexityLevel() != null && candidate.getComplexityLevel() != null) {
            int diff = Math.abs(target.getComplexityLevel() - candidate.getComplexityLevel());
            score += Math.max(0, 10 - diff * 2);
        }
        maxScore += 10;

        double matrixSim = calculateMatrixSimilarity(target, candidate);
        score += matrixSim * 15;
        maxScore += 15;

        double colorSim = calculateColorSimilarity(target, candidate);
        score += colorSim * 10;
        maxScore += 10;

        double densitySim = calculateDensitySimilarity(target, candidate);
        score += densitySim * 5;
        maxScore += 5;

        if (maxScore > 0) {
            score = Math.round(score * 100.0 / maxScore * 100.0) / 100.0;
        }

        return Math.min(100, Math.max(0, score));
    }

    private double calculateMatrixSimilarity(PatternDesign target, PatternDesign candidate) {
        int[][] targetMatrix = parseMatrixSafe(target.getPatternMatrix(),
                target.getWarpRepeat(), target.getWeftRepeat());
        int[][] candidateMatrix = parseMatrixSafe(candidate.getPatternMatrix(),
                candidate.getWarpRepeat(), candidate.getWeftRepeat());

        if (targetMatrix.length == 0 || candidateMatrix.length == 0) {
            return 0.5;
        }

        int size = 16;
        long targetHash = computePerceptualHash(targetMatrix, size);
        long candidateHash = computePerceptualHash(candidateMatrix, size);

        int hammingDistance = Long.bitCount(targetHash ^ candidateHash);
        double similarity = 1.0 - (double) hammingDistance / (size * size);

        return Math.max(0, similarity);
    }

    private long computePerceptualHash(int[][] matrix, int size) {
        int[][] reduced = new int[size][size];
        int rows = matrix.length;
        int cols = matrix[0].length;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int srcI = (i * rows) / size;
                int srcJ = (j * cols) / size;
                reduced[i][j] = matrix[srcI][srcJ];
            }
        }

        int total = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                total += reduced[i][j];
            }
        }
        int average = total / (size * size);

        long hash = 0L;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                hash <<= 1;
                if (reduced[i][j] >= average) {
                    hash |= 1;
                }
            }
        }
        return hash;
    }

    private int[][] parseMatrixSafe(String matrixStr, Integer warp, Integer weft) {
        if (matrixStr == null || matrixStr.trim().isEmpty()) {
            if (warp != null && weft != null) {
                return generateDefaultMatrix(warp, weft);
            }
            return new int[0][0];
        }
        try {
            return parseMatrixString(matrixStr);
        } catch (Exception e) {
            return new int[0][0];
        }
    }

    private double calculateColorSimilarity(PatternDesign target, PatternDesign candidate) {
        if (target.getColorCount() == null || candidate.getColorCount() == null) {
            return 0.5;
        }
        int diff = Math.abs(target.getColorCount() - candidate.getColorCount());
        return Math.max(0, 1.0 - (double) diff / 30.0);
    }

    private double calculateDensitySimilarity(PatternDesign target, PatternDesign candidate) {
        if (target.getWarpRepeat() == null || target.getWeftRepeat() == null ||
            candidate.getWarpRepeat() == null || candidate.getWeftRepeat() == null) {
            return 0.5;
        }
        int targetSize = target.getWarpRepeat() * target.getWeftRepeat();
        int candidateSize = candidate.getWarpRepeat() * candidate.getWeftRepeat();
        if (targetSize == 0 || candidateSize == 0) return 0.5;
        double ratio = (double) Math.min(targetSize, candidateSize) / Math.max(targetSize, candidateSize);
        return ratio;
    }

    private Set<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(tagsStr.split("[,，;；、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public Map<String, Object> getPatternStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = patternRepository.count();
        stats.put("totalPatterns", total);

        List<String> categories = patternRepository.findAllCategories();
        stats.put("categories", categories);

        List<String> dynasties = patternRepository.findAllDynasties();
        stats.put("dynasties", dynasties);

        List<PatternDesign> popular = getPopularPatterns(5);
        stats.put("top5Popular", popular.stream()
                .map(p -> Map.of("id", p.getId(), "name", p.getName(), "useCount", p.getUseCount()))
                .collect(Collectors.toList()));

        return stats;
    }

    public int[][] getPatternMatrixAsArray(Long patternId) {
        PatternDesign pattern = patternRepository.findById(patternId)
                .orElseThrow(() -> new IllegalArgumentException("纹样不存在"));

        String matrixStr = pattern.getPatternMatrix();
        if (matrixStr == null || matrixStr.trim().isEmpty()) {
            if (pattern.getWarpRepeat() != null && pattern.getWeftRepeat() != null) {
                return generateDefaultMatrix(pattern.getWarpRepeat(), pattern.getWeftRepeat());
            }
            return new int[0][0];
        }

        try {
            return parseMatrixString(matrixStr);
        } catch (Exception e) {
            return new int[0][0];
        }
    }

    private int[][] parseMatrixString(String matrixStr) {
        String[] rows = matrixStr.trim().split("[\\n;]");
        List<int[]> result = new ArrayList<>();
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) continue;
            String[] cells = trimmed.split("[,\\s]+");
            int[] rowArr = new int[cells.length];
            for (int i = 0; i < cells.length; i++) {
                rowArr[i] = Integer.parseInt(cells[i].trim());
            }
            result.add(rowArr);
        }
        return result.toArray(new int[0][]);
    }

    private int[][] generateDefaultMatrix(int warp, int weft) {
        int[][] matrix = new int[weft][warp];
        for (int i = 0; i < weft; i++) {
            for (int j = 0; j < warp; j++) {
                matrix[i][j] = (i + j) % 2;
            }
        }
        return matrix;
    }
}
