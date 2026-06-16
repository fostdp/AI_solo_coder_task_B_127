package com.yunjin.system.variety_comparator;

import com.yunjin.system.entity.YunjinVariety;
import com.yunjin.system.repository.YunjinVarietyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("云锦品种工艺对比测试 - VarietyComparatorService")
class VarietyComparatorServiceTest {

    @Mock
    private YunjinVarietyRepository varietyRepository;

    @InjectMocks
    private VarietyComparatorService comparatorService;

    private YunjinVariety zhuanghua;
    private YunjinVariety kujin;
    private YunjinVariety kuduan;
    private YunjinVariety mingjin;
    private YunjinVariety songjin;

    @BeforeEach
    void setUp() {
        zhuanghua = createVariety(1L, "ZHUANGHUA", "妆花", "明代", "妆花缎",
                14000, 4, 56.0, 24.0,
                24, 1200, 4.5,
                "桑蚕丝+圆金线+扁金线+孔雀羽线", 780.0,
                "通经断纬，挖花盘织", 98, 9.8,
                "明清宫廷御用龙袍", "南京云锦研究所《南京云锦史》P156-189", 1);

        kujin = createVariety(2L, "KUJIN", "库锦", "清代", "织金缎",
                12000, 2, 60.0, 28.0,
                12, 800, 10.0,
                "桑蚕丝+圆金线+扁金线", 720.0,
                "金线显花，花纹精致", 82, 8.5,
                "宫廷服饰、官服补子", "故宫博物院《清代宫廷服饰》卷2 P45-67", 1);

        kuduan = createVariety(3L, "KUDUAN", "库缎", "清代", "暗花缎",
                10000, 1, 58.0, 32.0,
                2, 160, 25.0,
                "桑蚕丝(厂丝)", 420.0,
                "一上一下或一上三下组织", 48, 5.2,
                "日常服饰、袍料", "《清代江南三织造档案史料》P345-367", 1);

        mingjin = createVariety(4L, "MING_JIN", "明锦", "明代", "妆花缎",
                11000, 3, 55.0, 24.0,
                14, 700, 5.0,
                "桑蚕丝+金线", 620.0,
                "图案豪放，色彩庄重", 85, 8.0,
                "宫廷服饰、官服", "定陵博物馆《定陵出土文物研究》P45-78", 1);

        songjin = createVariety(5L, "SONG_JIN", "宋锦", "宋代", "宋锦",
                8000, 2, 52.0, 30.0,
                8, 300, 15.0,
                "桑蚕丝", 520.0,
                "经线二重，纬线三至四色", 65, 6.5,
                "书画装裱、服饰", "苏州丝绸博物馆《苏州宋锦》P23-45", 2);
    }

    private YunjinVariety createVariety(Long id, String code, String name, String dynasty,
                                       String weaveType, Integer warpCount, Integer weftCount,
                                       Double warpDensity, Double weftDensity,
                                       Integer colorCount, Integer harnessCount,
                                       Double productionSpeed, String rawMaterial,
                                       Double silkWeight, String characteristics,
                                       Integer complexityScore, Double rarityScore,
                                       String historicalUsage, String referenceSources,
                                       Integer dataAccuracyLevel) {
        YunjinVariety v = new YunjinVariety();
        v.setId(id);
        v.setCode(code);
        v.setName(name);
        v.setDynasty(dynasty);
        v.setWeaveType(weaveType);
        v.setWarpCount(warpCount);
        v.setWeftCount(weftCount);
        v.setWarpDensityPerCm(warpDensity);
        v.setWeftDensityPerCm(weftDensity);
        v.setColorCount(colorCount);
        v.setHarnessCount(harnessCount);
        v.setProductionSpeedCmPerDay(productionSpeed);
        v.setRawMaterial(rawMaterial);
        v.setSilkWeightPerSqmGram(silkWeight);
        v.setCharacteristics(characteristics);
        v.setComplexityScore(complexityScore);
        v.setRarityScore(rarityScore);
        v.setHistoricalUsage(historicalUsage);
        v.setReferenceSources(referenceSources);
        v.setDataAccuracyLevel(dataAccuracyLevel);
        v.setPatternRepeatWarp(warpCount / 10);
        v.setPatternRepeatWeft(weftCount * 20);
        return v;
    }

    @Nested
    @DisplayName("【正常场景】品种对比核心功能")
    class NormalScenarios {

        @Test
        @DisplayName("NC-VC-01: 三品种对比矩阵数据完整")
        void testThreeVarietyComparison() {
            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Arrays.asList(zhuanghua, kujin, kuduan));

            Map<String, Object> result = comparatorService.compareVarieties(
                    Arrays.asList(1L, 2L, 3L));

            assertNotNull(result);
            assertTrue(result.containsKey("varieties"));
            assertTrue(result.containsKey("comparisonMatrix"));
            assertTrue(result.containsKey("analysis"));

            @SuppressWarnings("unchecked")
            List<YunjinVariety> varieties = (List<YunjinVariety>) result.get("varieties");
            assertEquals(3, varieties.size());

            @SuppressWarnings("unchecked")
            Map<String, List<Object>> matrix = (Map<String, List<Object>>) result.get("comparisonMatrix");
            assertTrue(matrix.containsKey("经纱数"));
            assertTrue(matrix.containsKey("用色数"));
            assertTrue(matrix.containsKey("综框数"));
            assertEquals(3, matrix.get("经纱数").size());
        }

        @Test
        @DisplayName("NC-VC-02: 雷达图六维评分均在0-100区间")
        void testRadarChartDimensions() {
            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Collections.singletonList(zhuanghua));

            List<Map<String, Object>> radarList = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(1L));

            assertNotNull(radarList);
            assertEquals(1, radarList.size());

            Map<String, Object> item = radarList.get(0);
            assertTrue(item.containsKey("name"));
            assertTrue(item.containsKey("code"));
            assertTrue(item.containsKey("dimensions"));

            @SuppressWarnings("unchecked")
            Map<String, Integer> dimensions = (Map<String, Integer>) item.get("dimensions");
            assertEquals(6, dimensions.size());
            assertTrue(dimensions.containsKey("工艺复杂度"));
            assertTrue(dimensions.containsKey("色彩丰富度"));
            assertTrue(dimensions.containsKey("织造难度"));
            assertTrue(dimensions.containsKey("文化价值"));
            assertTrue(dimensions.containsKey("历史悠久度"));
            assertTrue(dimensions.containsKey("材料珍稀度"));

            for (Integer score : dimensions.values()) {
                assertTrue(score >= 0 && score <= 100,
                        "评分应在0-100之间，实际: " + score);
            }
        }

        @Test
        @DisplayName("NC-VC-03: 织造难度归一化计算正确")
        void testDifficultyNormalization() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Collections.singletonList(zhuanghua))
                    .thenReturn(Collections.singletonList(kuduan));

            List<Map<String, Object>> radarList1 = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(1L));
            List<Map<String, Object>> radarList2 = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(3L));

            @SuppressWarnings("unchecked")
            int diff1 = (int) ((Map<String, Object>) radarList1.get(0).get("dimensions")).get("织造难度");
            @SuppressWarnings("unchecked")
            int diff2 = (int) ((Map<String, Object>) radarList2.get(0).get("dimensions")).get("织造难度");

            assertTrue(diff1 > diff2,
                    "妆花织造难度应高于库缎");
        }

        @Test
        @DisplayName("NC-VC-04: 朝代历史评分符合年代顺序")
        void testDynastyHistoricalScore() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Collections.singletonList(songjin))
                    .thenReturn(Collections.singletonList(mingjin))
                    .thenReturn(Collections.singletonList(kujin));

            List<Map<String, Object>> songRadar = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(5L));
            List<Map<String, Object>> mingRadar = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(4L));
            List<Map<String, Object>> qingRadar = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(2L));

            @SuppressWarnings("unchecked")
            int songScore = (int) ((Map<String, Object>) songRadar.get(0).get("dimensions")).get("历史悠久度");
            @SuppressWarnings("unchecked")
            int mingScore = (int) ((Map<String, Object>) mingRadar.get(0).get("dimensions")).get("历史悠久度");
            @SuppressWarnings("unchecked")
            int qingScore = (int) ((Map<String, Object>) qingRadar.get(0).get("dimensions")).get("历史悠久度");

            assertTrue(songScore >= mingScore, "宋代历史悠久度应不低于明代: " + songScore + " vs " + mingScore);
            assertTrue(mingScore >= qingScore, "明代历史悠久度应不低于清代: " + mingScore + " vs " + qingScore);
        }

        @Test
        @DisplayName("NC-VC-05: 材料珍稀度金线高于普通蚕丝")
        void testMaterialRarity() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Collections.singletonList(zhuanghua))
                    .thenReturn(Collections.singletonList(kuduan));

            List<Map<String, Object>> radar1 = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(1L));
            List<Map<String, Object>> radar3 = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(3L));

            @SuppressWarnings("unchecked")
            int rarity1 = (int) ((Map<String, Object>) radar1.get(0).get("dimensions")).get("材料珍稀度");
            @SuppressWarnings("unchecked")
            int rarity3 = (int) ((Map<String, Object>) radar3.get(0).get("dimensions")).get("材料珍稀度");

            assertTrue(rarity1 > rarity3,
                    "妆花材料珍稀度应高于库缎（含金线vs纯蚕丝）: " + rarity1 + " vs " + rarity3);
        }

        @Test
        @DisplayName("NC-VC-06: 文献来源字段可正常获取")
        void testReferenceSources() {
            when(varietyRepository.findById(1L)).thenReturn(Optional.of(zhuanghua));

            Optional<YunjinVariety> variety = comparatorService.getVarietyById(1L);

            assertTrue(variety.isPresent());
            assertNotNull(variety.get().getReferenceSources());
            assertTrue(variety.get().getReferenceSources().contains("南京云锦研究所"));
            assertEquals(1, variety.get().getDataAccuracyLevel());
        }
    }

    @Nested
    @DisplayName("【边界场景】边界值与极限测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("BD-VC-01: 最小边界 - 刚好2个品种对比")
        void testMinimumTwoVarieties() {
            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Arrays.asList(zhuanghua, kujin));

            Map<String, Object> result = comparatorService.compareVarieties(
                    Arrays.asList(1L, 2L));

            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<YunjinVariety> varieties = (List<YunjinVariety>) result.get("varieties");
            assertEquals(2, varieties.size());
        }

        @Test
        @DisplayName("BD-VC-02: 最大边界 - 刚好5个品种对比")
        void testMaximumFiveVarieties() {
            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Arrays.asList(zhuanghua, kujin, kuduan, mingjin, songjin));

            Map<String, Object> result = comparatorService.compareVarieties(
                    Arrays.asList(1L, 2L, 3L, 4L, 5L));

            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<YunjinVariety> varieties = (List<YunjinVariety>) result.get("varieties");
            assertEquals(5, varieties.size());
        }

        @Test
        @DisplayName("BD-VC-03: null字段保护 - 空值不抛异常")
        void testNullFieldProtection() {
            YunjinVariety partial = new YunjinVariety();
            partial.setId(99L);
            partial.setCode("TEST");
            partial.setName("测试品种");

            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Collections.singletonList(partial));

            assertDoesNotThrow(() -> comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(99L)));

            List<Map<String, Object>> radar = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(99L));
            assertNotNull(radar);
            assertEquals(1, radar.size());
        }

        @Test
        @DisplayName("BD-VC-04: 复杂度满分边界不越界")
        void testFullComplexityScoreBoundary() {
            YunjinVariety maxVariety = createVariety(10L, "MAX", "极限品种", "上古", "极限组织",
                    20000, 10, 100.0, 50.0,
                    50, 2000, 0.5,
                    "金+玉+翡翠+珍珠+龙涎香", 2000.0,
                    "极限工艺", 100, 10.0,
                    "传说神器", "山海经", 1);

            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Collections.singletonList(maxVariety));

            List<Map<String, Object>> radar = comparatorService.getVarietyComparisonRadar(
                    Collections.singletonList(10L));
            @SuppressWarnings("unchecked")
            Map<String, Integer> dimensions = (Map<String, Integer>) radar.get(0).get("dimensions");

            for (Map.Entry<String, Integer> entry : dimensions.entrySet()) {
                assertTrue(entry.getValue() <= 100,
                        entry.getKey() + "评分不应超过100，实际: " + entry.getValue());
                assertTrue(entry.getValue() >= 0,
                        entry.getKey() + "评分不应低于0，实际: " + entry.getValue());
            }
        }

        @Test
        @DisplayName("BD-VC-05: 关键词搜索空关键词返回全部")
        void testEmptyKeywordSearch() {
            when(varietyRepository.findAll()).thenReturn(
                    Arrays.asList(zhuanghua, kujin, kuduan));

            List<YunjinVariety> result = comparatorService.searchVarieties("");
            assertEquals(3, result.size());

            result = comparatorService.searchVarieties(null);
            assertEquals(3, result.size());
        }
    }

    @Nested
    @DisplayName("【异常场景】异常处理与错误输入")
    class ExceptionScenarios {

        @Test
        @DisplayName("EX-VC-01: 仅1个品种抛异常")
        void testOnlyOneVariety() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.compareVarieties(Collections.singletonList(1L)));
            assertTrue(ex.getMessage().contains("至少选择2个"));
        }

        @Test
        @DisplayName("EX-VC-02: null列表抛非法参数")
        void testNullList() {
            assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.compareVarieties(null));
        }

        @Test
        @DisplayName("EX-VC-03: 空列表抛非法参数")
        void testEmptyList() {
            assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.compareVarieties(Collections.emptyList()));
        }

        @Test
        @DisplayName("EX-VC-04: 超过5个抛异常")
        void testMoreThanFive() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.compareVarieties(
                            Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L)));
            assertTrue(ex.getMessage().contains("最多支持同时对比5个品种"));
        }

        @Test
        @DisplayName("EX-VC-05: 部分ID不存在抛异常")
        void testSomeIdsNotFound() {
            when(varietyRepository.findAllById(anyList())).thenReturn(
                    Collections.singletonList(zhuanghua));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.compareVarieties(Arrays.asList(1L, 999L)));
            assertTrue(ex.getMessage().contains("部分品种ID不存在"));
        }

        @Test
        @DisplayName("EX-VC-06: 编码重复创建")
        void testDuplicateCode() {
            when(varietyRepository.findByCode("ZHUANGHUA")).thenReturn(Optional.of(zhuanghua));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.createVariety(zhuanghua));
            assertTrue(ex.getMessage().contains("品种编码已存在"));
            verify(varietyRepository, never()).save(any());
        }

        @Test
        @DisplayName("EX-VC-08: 更新不存在抛异常")
        void testUpdateNotFound() {
            YunjinVariety nonExistent = new YunjinVariety();
            nonExistent.setId(999L);
            when(varietyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.updateVariety(999L, nonExistent));
        }

        @Test
        @DisplayName("EX-VC-09: 删除不存在抛异常")
        void testDeleteNotFound() {
            when(varietyRepository.existsById(999L)).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                    comparatorService.deleteVariety(999L));
        }
    }
}
