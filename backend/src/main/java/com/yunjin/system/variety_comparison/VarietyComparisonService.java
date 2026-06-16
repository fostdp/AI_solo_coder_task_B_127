package com.yunjin.system.variety_comparison;

import com.yunjin.system.entity.YunjinVariety;
import com.yunjin.system.repository.YunjinVarietyRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VarietyComparisonService {

    private final YunjinVarietyRepository varietyRepository;

    public VarietyComparisonService(YunjinVarietyRepository varietyRepository) {
        this.varietyRepository = varietyRepository;
    }

    public List<YunjinVariety> getAllVarieties() {
        return varietyRepository.findAll();
    }

    public Optional<YunjinVariety> getVarietyById(Long id) {
        return varietyRepository.findById(id);
    }

    public Optional<YunjinVariety> getVarietyByCode(String code) {
        return varietyRepository.findByCode(code);
    }

    public List<YunjinVariety> searchVarieties(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllVarieties();
        }
        return varietyRepository.searchByKeyword(keyword.trim());
    }

    public List<String> getAllDynasties() {
        return varietyRepository.findAllDynasties();
    }

    public List<String> getAllWeaveTypes() {
        return varietyRepository.findAllWeaveTypes();
    }

    public Map<String, Object> compareVarieties(List<Long> varietyIds) {
        if (varietyIds == null || varietyIds.size() < 2) {
            throw new IllegalArgumentException("至少选择2个品种进行对比");
        }
        if (varietyIds.size() > 5) {
            throw new IllegalArgumentException("最多支持同时对比5个品种");
        }

        List<YunjinVariety> varieties = varietyRepository.findAllById(varietyIds);
        if (varieties.size() != varietyIds.size()) {
            throw new IllegalArgumentException("部分品种ID不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("varieties", varieties);

        Map<String, List<Object>> comparisonMatrix = new LinkedHashMap<>();

        comparisonMatrix.put("名称", varieties.stream().map(YunjinVariety::getName).collect(Collectors.toList()));
        comparisonMatrix.put("别名", varieties.stream().map(YunjinVariety::getAlias).collect(Collectors.toList()));
        comparisonMatrix.put("朝代", varieties.stream().map(YunjinVariety::getDynasty).collect(Collectors.toList()));
        comparisonMatrix.put("组织类型", varieties.stream().map(YunjinVariety::getWeaveType).collect(Collectors.toList()));
        comparisonMatrix.put("经纱数", varieties.stream().map(YunjinVariety::getWarpCount).collect(Collectors.toList()));
        comparisonMatrix.put("纬纱组数", varieties.stream().map(YunjinVariety::getWeftCount).collect(Collectors.toList()));
        comparisonMatrix.put("经密(根/cm)", varieties.stream().map(YunjinVariety::getWarpDensityPerCm).collect(Collectors.toList()));
        comparisonMatrix.put("纬密(根/cm)", varieties.stream().map(YunjinVariety::getWeftDensityPerCm).collect(Collectors.toList()));
        comparisonMatrix.put("用色数", varieties.stream().map(YunjinVariety::getColorCount).collect(Collectors.toList()));
        comparisonMatrix.put("综框数", varieties.stream().map(YunjinVariety::getHarnessCount).collect(Collectors.toList()));
        comparisonMatrix.put("开口机构", varieties.stream().map(YunjinVariety::getSheddingMechanism).collect(Collectors.toList()));
        comparisonMatrix.put("花回尺寸(经×纬)", varieties.stream()
                .map(v -> v.getPatternRepeatWarp() + "×" + v.getPatternRepeatWeft())
                .collect(Collectors.toList()));
        comparisonMatrix.put("日产量(cm/天)", varieties.stream().map(YunjinVariety::getProductionSpeedCmPerDay).collect(Collectors.toList()));
        comparisonMatrix.put("原料", varieties.stream().map(YunjinVariety::getRawMaterial).collect(Collectors.toList()));
        comparisonMatrix.put("克重(g/㎡)", varieties.stream().map(YunjinVariety::getSilkWeightPerSqmGram).collect(Collectors.toList()));
        comparisonMatrix.put("复杂度评分", varieties.stream().map(YunjinVariety::getComplexityScore).collect(Collectors.toList()));
        comparisonMatrix.put("珍稀度", varieties.stream().map(YunjinVariety::getRarityScore).collect(Collectors.toList()));

        result.put("comparisonMatrix", comparisonMatrix);

        Map<String, Object> analysis = generateComparisonAnalysis(varieties);
        result.put("analysis", analysis);

        return result;
    }

    private Map<String, Object> generateComparisonAnalysis(List<YunjinVariety> varieties) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        IntSummaryStatistics warpStats = varieties.stream()
                .filter(v -> v.getWarpCount() != null)
                .mapToInt(YunjinVariety::getWarpCount)
                .summaryStatistics();
        analysis.put("经纱数范围", warpStats.getMin() + " ~ " + warpStats.getMax() + " 根");

        IntSummaryStatistics colorStats = varieties.stream()
                .filter(v -> v.getColorCount() != null)
                .mapToInt(YunjinVariety::getColorCount)
                .summaryStatistics();
        analysis.put("用色数范围", colorStats.getMin() + " ~ " + colorStats.getMax() + " 色");

        DoubleSummaryStatistics speedStats = varieties.stream()
                .filter(v -> v.getProductionSpeedCmPerDay() != null)
                .mapToDouble(YunjinVariety::getProductionSpeedCmPerDay)
                .summaryStatistics();
        analysis.put("日产量范围", String.format("%.1f ~ %.1f cm/天", speedStats.getMin(), speedStats.getMax()));

        YunjinVariety mostComplex = varieties.stream()
                .filter(v -> v.getComplexityScore() != null)
                .max(Comparator.comparingInt(YunjinVariety::getComplexityScore))
                .orElse(null);
        analysis.put("工艺最复杂", mostComplex != null ? mostComplex.getName() : "N/A");

        YunjinVariety mostColorful = varieties.stream()
                .filter(v -> v.getColorCount() != null)
                .max(Comparator.comparingInt(YunjinVariety::getColorCount))
                .orElse(null);
        analysis.put("色彩最丰富", mostColorful != null ? mostColorful.getName() : "N/A");

        List<String> sharedMaterials = varieties.stream()
                .map(YunjinVariety::getRawMaterial)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        analysis.put("涉及原料", sharedMaterials);

        int mostHarness = varieties.stream()
                .filter(v -> v.getHarnessCount() != null)
                .mapToInt(YunjinVariety::getHarnessCount)
                .max().orElse(0);
        analysis.put("最高综框数", mostHarness + " 综");

        return analysis;
    }

    public YunjinVariety createVariety(YunjinVariety variety) {
        if (varietyRepository.findByCode(variety.getCode()).isPresent()) {
            throw new IllegalArgumentException("品种编码已存在");
        }
        return varietyRepository.save(variety);
    }

    public YunjinVariety updateVariety(Long id, YunjinVariety variety) {
        YunjinVariety existing = varietyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("品种不存在"));
        
        existing.setName(variety.getName());
        existing.setAlias(variety.getAlias());
        existing.setDescription(variety.getDescription());
        existing.setDynasty(variety.getDynasty());
        existing.setWeaveType(variety.getWeaveType());
        existing.setWarpCount(variety.getWarpCount());
        existing.setWeftCount(variety.getWeftCount());
        existing.setWarpDensityPerCm(variety.getWarpDensityPerCm());
        existing.setWeftDensityPerCm(variety.getWeftDensityPerCm());
        existing.setColorCount(variety.getColorCount());
        existing.setPatternRepeatWarp(variety.getPatternRepeatWarp());
        existing.setPatternRepeatWeft(variety.getPatternRepeatWeft());
        existing.setSheddingMechanism(variety.getSheddingMechanism());
        existing.setHarnessCount(variety.getHarnessCount());
        existing.setProductionSpeedCmPerDay(variety.getProductionSpeedCmPerDay());
        existing.setRawMaterial(variety.getRawMaterial());
        existing.setSilkWeightPerSqmGram(variety.getSilkWeightPerSqmGram());
        existing.setCharacteristics(variety.getCharacteristics());
        existing.setHistoricalUsage(variety.getHistoricalUsage());
        existing.setCulturalSignificance(variety.getCulturalSignificance());
        existing.setPaletteColors(variety.getPaletteColors());
        existing.setComplexityScore(variety.getComplexityScore());
        existing.setRarityScore(variety.getRarityScore());
        existing.setRepresentativeWorks(variety.getRepresentativeWorks());
        existing.setImageUrl(variety.getImageUrl());

        return varietyRepository.save(existing);
    }

    public void deleteVariety(Long id) {
        if (!varietyRepository.existsById(id)) {
            throw new IllegalArgumentException("品种不存在");
        }
        varietyRepository.deleteById(id);
    }

    public List<Map<String, Object>> getVarietyComparisonRadar(List<Long> varietyIds) {
        List<YunjinVariety> varieties = varietyRepository.findAllById(varietyIds);
        List<Map<String, Object>> radarData = new ArrayList<>();

        for (YunjinVariety v : varieties) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", v.getName());
            item.put("code", v.getCode());

            Map<String, Integer> dimensions = new LinkedHashMap<>();
            dimensions.put("工艺复杂度", normalizeScore(v.getComplexityScore(), 100));
            dimensions.put("色彩丰富度", normalizeScore(v.getColorCount(), 20));
            dimensions.put("织造难度", normalizeDifficulty(v));
            dimensions.put("文化价值", normalizeScore((int)(v.getRarityScore() * 20), 100));
            dimensions.put("历史悠久度", normalizeScore(estimateAgeScore(v.getDynasty()), 100));
            dimensions.put("材料珍稀度", normalizeMaterialScore(v.getRawMaterial()));

            item.put("dimensions", dimensions);
            radarData.add(item);
        }

        return radarData;
    }

    private int normalizeScore(Integer value, int max) {
        if (value == null) return 0;
        return Math.min(100, (int) (value * 100.0 / max));
    }

    private int normalizeDifficulty(YunjinVariety v) {
        int score = 0;
        if (v.getHarnessCount() != null) {
            score += Math.min(40, v.getHarnessCount() * 2);
        }
        if (v.getWarpCount() != null) {
            score += Math.min(30, v.getWarpCount() / 40);
        }
        if (v.getColorCount() != null) {
            score += Math.min(30, v.getColorCount() * 2);
        }
        return Math.min(100, score);
    }

    private int estimateAgeScore(String dynasty) {
        if (dynasty == null) return 50;
        Map<String, Integer> dynastyAge = new HashMap<>();
        dynastyAge.put("东晋", 90);
        dynastyAge.put("南北朝", 85);
        dynastyAge.put("唐", 80);
        dynastyAge.put("宋", 75);
        dynastyAge.put("元", 70);
        dynastyAge.put("明", 60);
        dynastyAge.put("清", 40);
        dynastyAge.put("民国", 20);
        dynastyAge.put("现代", 10);
        return dynastyAge.getOrDefault(dynasty, 50);
    }

    private int normalizeMaterialScore(String material) {
        if (material == null) return 50;
        if (material.contains("真丝") || material.contains("桑蚕丝")) return 80;
        if (material.contains("金线") || material.contains("孔雀羽")) return 95;
        if (material.contains("棉")) return 40;
        return 60;
    }
}
