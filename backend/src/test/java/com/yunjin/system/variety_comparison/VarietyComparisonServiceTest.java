package com.yunjin.system.variety_comparison;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("云锦品种工艺对比测试 - VarietyComparisonService")
class VarietyComparisonServiceTest {

    @Mock
    private YunjinVarietyRepository varietyRepository;

    @InjectMocks
    private VarietyComparisonService varietyService;

    private YunjinVariety zhuanghua;

    private YunjinVariety kujin;

    private YunjinVariety kuduan;

    @BeforeEach
    void setUp() {
        zhuanghua = createVariety(1L, "ZHUANGHUA", "妆花", "清", "妆花缎",
                15000, 5, 80, 70, 24, 1200, 20, 95, "妆花是云锦中最复杂的品种，采用通经断纬",
                "真丝,金线,孔雀羽", "皇家礼服面料", "皇家御用，代表最高工艺", 98, 5.0, 95);

        kujin = createVariety(2L, "KUJIN", "库锦", "清", "织金锦", 8000, 3, 60,
                50, 12, 600, 10, 80,
                "片金织入织金技术，金线显花",
                "真丝,金线", "宫廷赏赐用锦缎",
                "官方库存储备之锦，象征富贵", 85, 4.2, 82);

        kuduan = createVariety(3L, "KUDUAN", "库缎", "明", "缎纹组织",
                10000, 2, 70, 55, 8, 800, 5, 70,
                "缎地起花，光亮细腻",
                "真丝", "宫廷服装面料",
                "日常服用绸缎，质地厚实", 70, 3.5, 72);
    }

    private YunjinVariety createVariety(Long id, String code, String name, String dynasty,
                                    String weaveType, int warpCount, int weftCount,
                                    double warpDensity, double weftDensity,
                                    int colorCount, int harnessCount,
                                    double speed, String desc,
                                    String material, String usage,
                                    String cultural, int complexity,
                                    double rarity, int prodSpeed) {
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
        v.setProductionSpeedCmPerDay(prodSpeed);
        v.setDescription(desc);
        v.setRawMaterial(material);
        v.setHistoricalUsage(usage);
        v.setCulturalSignificance(cultural);
        v.setComplexityScore(complexity);
        v.setRarityScore(rarity);
        return v;
    }

    @Nested
    @DisplayName("【正常场景】品种对比核心功能")
    class NormalScenarios {

        @Test
        @DisplayName("NC-VC-01: 成功对比妆花、库锦、库缎三品种对比-验证工艺参数矩阵正确")
        void compareThreeClassicVarieties() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(zhuanghua, kujin, kuduan));

            Map<String, Object> result = varietyService.compareVarieties(
                    Arrays.asList(1L, 2L, 3L));

            assertNotNull(result);
            assertTrue(result.containsKey("varieties"));
            assertTrue(result.containsKey("comparisonMatrix"));
            assertTrue(result.containsKey("analysis"));

            List<YunjinVariety> varieties = (List<YunjinVariety>) result.get("varieties");
            assertEquals(3, varieties.size());

            Map<String, List<Object>> matrix =
                    (Map<String, List<Object>>) result.get("comparisonMatrix");

            List<Object> warpCounts = matrix.get("经纱数");
            assertEquals(15000, warpCounts.get(0));
            assertEquals(8000, warpCounts.get(1));
            assertEquals(10000, warpCounts.get(2));

            List<Object> colorCounts = matrix.get("用色数");
            assertEquals(24, colorCounts.get(0));
            assertEquals(12, colorCounts.get(1));
            assertEquals(8, colorCounts.get(2));

            Map<String, Object> analysis = (Map<String, Object>) result.get("analysis");
            assertEquals("妆花", analysis.get("工艺最复杂"));
            assertEquals("妆花", analysis.get("色彩最丰富"));

            verify(varietyRepository).findAllById(anyList()));
        }

        @Test
        @DisplayName("NC-VC-02: 雷达图-验证六维评分覆盖全部维度非空且在0-100之间")
        void radarChartSixDimensions() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(zhuanghua, kujin, kuduan));

            List<Map<String, Object>> radar =
                    varietyService.getVarietyComparisonRadar(Arrays.asList(1L, 2L, 3L));

            assertEquals(3, radar.size());

            String[] expectedDims = {"工艺复杂度", "色彩丰富度", "织造难度",
                    "文化价值", "历史悠久度", "材料珍稀度"};

            for (Map<String, Object> item : radar) {
                assertTrue(item.containsKey("name"));
                assertTrue(item.containsKey("code"));
                Map<String, Integer> dims = (Map<String, Integer>) item.get("dimensions");

                for (String dim : expectedDims) {
                    assertTrue(dims.containsKey(dim), "缺失维度: " + dim);
                    int score = dims.get(dim);
                    assertTrue(score >= 0 && score <= 100,
                            String.format("%s 分数 %d 超出0-100范围", dim, score));
                }

                Map<String, Integer> zhuanghuaDims = null;
                if ("妆花".equals(item.get("name"))) {
                    zhuanghuaDims = dims;
                }
                if (zhuanghuaDims != null) {
                    assertTrue(zhuanghuaDims.get("工艺复杂度") >= 90);
                    assertTrue(zhuanghuaDims.get("色彩丰富度") >= 90);
                }
            }
        }

        @Test
        @DisplayName("NC-VC-03: 织造难度归一化算法-验证综页、经密、色数加权正确")
        void difficultyNormalization() {
            YunjinVariety complex = createVariety(4L, "CMPX", "超复杂", "唐", "缂丝",
                    20000, 6, 100, 90, 30, 2000, 2, 99,
                    "缂丝工艺", "真丝", "书画装裱", "通经断纬极致", 99, 4.9, 5);

            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(complex, kuduan));

            List<Map<String, Object>> radar =
                    varietyService.getVarietyComparisonRadar(
                            Arrays.asList(4L, 3L));

            Map<String, Integer> complexDims = null;
            Map<String, Integer> kuduanDims = null;

            for (Map<String, Object> item : radar) {
                if ("超复杂".equals(item.get("name"))) complexDims =
                        (Map<String, Integer>) item.get("dimensions");
                if ("库缎".equals(item.get("name"))) kuduanDims =
                        (Map<String, Integer>) item.get("dimensions");
            }

            assertNotNull(complexDims);
    assertNotNull(kuduanDims);

            int complexDiff = complexDims.get("织造难度");
            int kuduanDiff = kuduanDims.get("织造难度");

            assertTrue(complexDiff > kuduanDiff,
                    String.format("超复杂(%d)应难于库缎(%d)", complexDiff, kuduanDiff));
            assertTrue(complexDiff <= 100, "织造难度上限应为100");
            assertTrue(kuduanDiff >= 0, "织造难度下限应为0");
        }

        @Test
        @DisplayName("NC-VC-04: 朝代历史评分-唐>明>清排序验证")
        void dynastyAgeScoreOrdering() {
            YunjinVariety tang = createVariety(5L, "TANG", "唐锦", "唐", "唐联珠纹",
                6000, 2, 50, 45, 10, 400, 8, "唐代品种", "真丝", "唐代锦",
                "大唐风格", 75, 4.0, 30);

            YunjinVariety ming = createVariety(6L, "MING", "明锦", "明", "改机",
                9000, 3, 65, 55, 15, 600, 10, "明代", "真丝", "明锦", "明风",
                80, 3.8, 40);

            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(tang, ming, zhuanghua));

            List<Map<String, Object>> radar =
                    varietyService.getVarietyComparisonRadar(
                            Arrays.asList(5L, 6L, 1L));

            int tangAge = 0, mingAge = 0, qingAge = 0;
            for (Map<String, Object> item : radar) {
                Map<String, Integer> d =
                        (Map<String, Integer>) item.get("dimensions");
                if ("唐锦".equals(item.get("name"))) tangAge = d.get("历史悠久度");
                if ("明锦".equals(item.get("name"))) mingAge = d.get("历史悠久度");
                if ("妆花".equals(item.get("name"))) qingAge = d.get("历史悠久度");
            }

            assertTrue(tangAge > mingAge && mingAge > qingAge,
                    String.format("唐(%d) > 明(%d) > 清(%d)", tangAge, mingAge, qingAge));
        }

        @Test
        @DisplayName("NC-VC-05: 材料珍稀度-金线孔雀羽>真丝>棉 验证")
        void materialRarityScoring() {
            YunjinVariety gold = createVariety(7L, "GLD", "金锦", "元", "织金",
                7000, 3, 55, 48, 12, 500, 7, "金", "真丝,金线,孔雀羽", "皇家",
                "珍稀", 85, 4.5, 25);

            YunjinVariety cotton = createVariety(8L, "COT", "棉锦", "现代", "棉织",
                5000, 2, 45, 40, 8, 300, 15, "普", "棉", "民",
                "通", 50, 2.0, 100);

            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(gold, kuduan, cotton));

            List<Map<String, Object>> radar = varietyService.getVarietyComparisonRadar(
                    Arrays.asList(7L, 3L, 8L));

            int goldScore = 0, silkScore = 0, cottonScore = 0;
            for (Map<String, Object> item : radar) {
                Map<String, Integer> d =
                        (Map<String, Integer>) item.get("dimensions");
                if ("金锦".equals(item.get("name"))) goldScore = d.get("材料珍稀度");
                if ("库缎".equals(item.get("name"))) silkScore = d.get("材料珍稀度");
                if ("棉锦".equals(item.get("name"))) cottonScore = d.get("材料珍稀度");
            }

            assertTrue(goldScore > silkScore && silkScore > cottonScore,
                    String.format("金线(%d) > 真丝(%d) > 棉(%d)",
                            goldScore, silkScore, cottonScore));
        }
    }

    @Nested
    @DisplayName("【边界场景】品种对比边界值")
    class BoundaryScenarios {

        @Test
        @DisplayName("BD-VC-01: 刚好2个品种对比(最小边界")
        void exactlyTwoVarieties() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(zhuanghua, kujin));

            Map<String, Object> result =
                    varietyService.compareVarieties(Arrays.asList(1L, 2L));

            assertNotNull(result);
            List<?> varieties = (List<?>) result.get("varieties");
            assertEquals(2, varieties.size());

            Map<String, List<Object>> matrix =
                    (Map<String, List<Object>>) result.get("comparisonMatrix");
            for (List<Object> row : matrix.values()) {
                assertEquals(2, row.size());
            }
        }

        @Test
        @DisplayName("BD-VC-02: 刚好5个品种对比(最大边界)")
        void exactlyFiveVarieties() {
            YunjinVariety v4 = createVariety(4L, "V4", "品种4", "宋", "宋锦",
                    7000, 2, 60, 50, 10, 500, 8, "d", "真丝", "宋代品种",
                    "宋风", 78, 3.9, 35);
            YunjinVariety v5 = createVariety(5L, "V5", "品种5", "元", "元织金锦",
                    7500, 3, 58, 52, 14, 550, 6, "元", "真丝,金线", "元用",
                    "元风", 82, 4.1, 28);

            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(
                            zhuanghua, kujin, kuduan, v4, v5));

            Map<String, Object> result = varietyService.compareVarieties(
                    Arrays.asList(1L, 2L, 3L, 4L, 5L));

            assertNotNull(result);
            assertEquals(5, ((List<?>) result.get("varieties")).size());
        }

        @Test
        @DisplayName("BD-VC-03: 空值null字段-验证NPE保护")
        void nullFieldValues() {
            YunjinVariety empty = new YunjinVariety();
            empty.setId(99L);
            empty.setCode("EMPTY");
            empty.setName("空品种");

            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(empty, kuduan));

            assertDoesNotThrow(() -> {
                Map<String, Object> result =
                        varietyService.compareVarieties(Arrays.asList(99L, 3L));
                assertNotNull(result);

                List<Map<String, Object>> radar =
                        varietyService.getVarietyComparisonRadar(
                                Arrays.asList(99L, 3L));
                assertEquals(2, radar.size());
            });
        }

        @Test
        @DisplayName("BD-VC-04: 边界分数计算-复杂度100分满分")
        void maxComplexityScore100() {
            YunjinVariety perfect = createVariety(9L, "PERF", "完美级", "东晋", "缂丝极品",
                    30000, 10, 120, 100, 50, 3000, 1, "最顶级",
                    "真丝,金线,孔雀羽,羊绒", "御用品",
                    "举世无双", 100, 5.0, 1);

            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Arrays.asList(perfect));

            List<Map<String, Object>> radar =
                    varietyService.getVarietyComparisonRadar(
                            Arrays.asList(9L));

            Map<String, Integer> dims = (Map<String, Integer>)
                    radar.get(0).get("dimensions");

            assertEquals(100, (int) dims.get("工艺复杂度"));
            assertEquals(100, (int) dims.get("色彩丰富度"));
            assertTrue(dims.get("织造难度") <= 100);
        }
    }

    @Nested
    @DisplayName("【异常场景】品种对比异常处理")
    class ExceptionScenarios {

        @Test
        @DisplayName("EX-VC-01: 仅1个品种-抛出非法参数异常")
        void singleVarietyFails() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> varietyService.compareVarieties(Collections.singletonList(1L)));

            assertTrue(ex.getMessage().contains("至少选择2个"));
            verify(varietyRepository, never()).findAllById(anyList()));
        }

        @Test
        @DisplayName("EX-VC-02: null列表-抛出非法参数异常")
        void nullListFails() {
            assertThrows(IllegalArgumentException.class,
                    () -> varietyService.compareVarieties(null));
        }

        @Test
        @DisplayName("EX-VC-03: 空列表-抛出非法参数异常")
        void emptyListFails() {
            assertThrows(IllegalArgumentException.class,
                    () -> varietyService.compareVarieties(Collections.emptyList()));
        }

        @Test
        @DisplayName("EX-VC-04: 超过5个品种-抛出非法参数异常")
        void overFiveVarietiesFails() {
            List<Long> sixIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> varietyService.compareVarieties(sixIds));

            assertTrue(ex.getMessage().contains("最多支持同时对比5个"));
            verify(varietyRepository, never()).findAllById(anyList()));
        }

        @Test
        @DisplayName("EX-VC-05: 部分品种ID不存在-抛出异常提示部分不存在")
        void someIdsNotFound() {
            when(varietyRepository.findAllById(anyList()))
                    .thenReturn(Collections.singletonList(zhuanghua)));

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> varietyService.compareVarieties(
                            Arrays.asList(1L, 999L)));

            assertTrue(ex.getMessage().contains("部分品种ID不存在"));
        }

        @Test
        @DisplayName("EX-VC-06: 创建品种编码重复-创建时抛异常")
        void duplicateCodeFails() {
            when(varietyRepository.findByCode("ZHUANGHUA"))
                    .thenReturn(Optional.of(zhuanghua));

            YunjinVariety newV = new YunjinVariety();
            newV.setCode("ZHUANGHUA");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> varietyService.createVariety(newV));

            assertTrue(ex.getMessage().contains("品种编码已存在"));
            verify(varietyRepository, never()).save(any()));
        }

        @Test
        @DisplayName("EX-VC-07: 更新不存在品种-抛异常")
        void updateNonExistentFails() {
            when(varietyRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> varietyService.updateVariety(999L, new YunjinVariety()));
        }

        @Test
        @DisplayName("EX-VC-08: 删除不存在品种-抛异常")
        void deleteNonExistentFails() {
            when(varietyRepository.existsById(999L))
                    .thenReturn(false));

            assertThrows(IllegalArgumentException.class,
                    () -> varietyService.deleteVariety(999L));
        }
    }
}
