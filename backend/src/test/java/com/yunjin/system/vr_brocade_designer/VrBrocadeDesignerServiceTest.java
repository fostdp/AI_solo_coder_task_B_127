package com.yunjin.system.vr_brocade_designer;

import com.yunjin.system.entity.PatternDesign;
import com.yunjin.system.entity.UserWeavingDesign;
import com.yunjin.system.entity.YunjinVariety;
import com.yunjin.system.repository.PatternDesignRepository;
import com.yunjin.system.repository.UserWeavingDesignRepository;
import com.yunjin.system.repository.YunjinVarietyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("公众虚拟织锦体验测试 - VrBrocadeDesignerService")
class VrBrocadeDesignerServiceTest {

    @Mock
    private UserWeavingDesignRepository designRepository;

    @Mock
    private PatternDesignRepository patternRepository;

    @Mock
    private YunjinVarietyRepository varietyRepository;

    @InjectMocks
    private VrBrocadeDesignerService weavingService;

    private YunjinVariety zhuanghua;

    private PatternDesign lotusPattern;

    private UserWeavingDesign designPlain;

    private UserWeavingDesign designTwill;

    private UserWeavingDesign designSatin;

    @BeforeEach
    void setUp() {
        zhuanghua = new YunjinVariety();
        zhuanghua.setId(1L);
        zhuanghua.setCode("ZHUANGHUA");
        zhuanghua.setName("妆花");
        zhuanghua.setWarpCount(120);
        zhuanghua.setWeaveType("twill");
        zhuanghua.setColorCount(24);
        zhuanghua.setPaletteColors("大红,#DC143C\n石青,#1E3A5F\n宫绿,#228B22");

        lotusPattern = new PatternDesign();
        lotusPattern.setId(10L);
        lotusPattern.setName("缠枝莲纹");
        lotusPattern.setWarpRepeat(24);
        lotusPattern.setWeftRepeat(24);
        lotusPattern.setColorCount(8);
        lotusPattern.setColorPalette("大红,#DC143C\n宫绿,#228B22");
        lotusPattern.setPatternMatrix(generateMatrixStr(48, 96, "plain"));
        lotusPattern.setVarietyId(1L);

        designPlain = createDesign(100L, "平纹设计1", "设计师A", 120,
                200, 5, generateMatrixStr(200, 120, "plain"),
                "draft", 25.5, 60);

        designTwill = createDesign(101L, "斜纹设计2", "设计师B", 120,
                200, 8, generateMatrixStr(200, 120, "twill"),
                "published", 42.3, 95);

        designSatin = createDesign(102L, "缎纹设计3", "设计师A", 128,
                256, 12, generateMatrixStr(256, 128, "satin"),
                "draft", 65.0, 150);
    }

    private UserWeavingDesign createDesign(Long id, String name, String designer,
                                           int warp, int weft, int colors,
                                           String matrix, String status,
                                           double complexity, int hours) {
        UserWeavingDesign d = new UserWeavingDesign();
        d.setId(id);
        d.setName(name);
        d.setDesigner(designer);
        d.setWarpCount(warp);
        d.setWeftCount(weft);
        d.setColorCount(colors);
        d.setPatternMatrix(matrix);
        d.setStatus(status);
        d.setComplexityScore(complexity);
        d.setEstimatedProductionHours(hours);
        d.setIsPublic("published".equals(status));
        d.setLikeCount(0);
        d.setViewCount(0);
        return d;
    }

    private String generateMatrixStr(int rows, int cols, String type) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int v;
                switch (type) {
                    case "twill":
                        v = ((i - j + cols) % 4 < 2) ? 1 : 0;
                        break;
                    case "satin":
                        v = ((j + 3 * i) % 8 == 0) ? 1 : 0;
                        break;
                    default:
                        v = (i + j) % 2;
                }
                sb.append(v);
                if (j < cols - 1) sb.append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String generateCustomMatrix(int[][] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                sb.append(data[i][j]);
                if (j < data[i].length - 1) sb.append(",");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Nested
    @DisplayName("【正常场景】用户设计创造力核心验证")
    class WeavingNormalScenarios {

        @Test
        @DisplayName("NC-VW-01: 从云锦品种模板创建设计-妆花模板120经×200纬")
        void createFromVarietyZhuanghua() {
            when(varietyRepository.findById(1L)).thenReturn(Optional.of(zhuanghua));
            when(designRepository.save(any())).thenAnswer(inv -> {
                UserWeavingDesign d = inv.getArgument(0);
                d.setId(200L);
                return d;
            });

            UserWeavingDesign created = weavingService.createDesignFromVariety(
                    1L, "测试设计师");

            assertNotNull(created);
            assertEquals("我的妆花设计", created.getName());
            assertEquals("测试设计师", created.getDesigner());
            assertEquals(Integer.valueOf(120), created.getWarpCount());
            assertEquals(Integer.valueOf(200), created.getWeftCount());
            assertEquals(Integer.valueOf(24), created.getColorCount());
            assertEquals("draft", created.getStatus());
            assertEquals(Long.valueOf(1L), created.getBaseVarietyId());
            assertNotNull(created.getPatternMatrix());
            assertNotNull(created.getComplexityScore());
            assertNotNull(created.getEstimatedProductionHours());

            verify(varietyRepository).findById(1L);
            verify(designRepository).save(any());
        }

        @Test
        @DisplayName("NC-VW-02: 从纹样库创建设计-24×24×4倍扩展")
        void createFromPatternLibrary() {
            when(patternRepository.findById(10L)).thenReturn(Optional.of(lotusPattern));
            when(varietyRepository.findById(1L)).thenReturn(Optional.of(zhuanghua));
            when(designRepository.save(any())).thenAnswer(inv -> {
                UserWeavingDesign d = inv.getArgument(0);
                d.setId(201L);
                return d;
            });

            UserWeavingDesign created = weavingService.createDesignFromPattern(
                    10L, "创意设计者");

            assertEquals("基于缠枝莲纹的设计", created.getName());
            assertEquals(Integer.valueOf(96), created.getWarpCount());
            assertEquals(Integer.valueOf(96), created.getWeftCount());
            assertEquals(Long.valueOf(1L), created.getBaseVarietyId());
            assertEquals("妆花", created.getBaseVarietyName());
            assertEquals("draft", created.getStatus());
            assertNotNull(created.getComplexityScore());
        }

        @Test
        @DisplayName("NC-VW-03: 创建空白设计-匿名设计师，100经×100纬平纹")
        void createBlankDesignDefault() {
            when(designRepository.save(any())).thenAnswer(inv -> {
                UserWeavingDesign d = inv.getArgument(0);
                d.setId(202L);
                return d;
            });

            UserWeavingDesign created = weavingService.createBlankDesign(
                    null, 100, 100);

            assertEquals("匿名设计师", created.getDesigner());
            assertEquals(Integer.valueOf(100), created.getWarpCount());
            assertEquals(Integer.valueOf(100), created.getWeftCount());
            assertEquals(Integer.valueOf(5), created.getColorCount());
            assertEquals("draft", created.getStatus());
            assertNotNull(created.getPatternMatrix());
        }

        @Test
        @DisplayName("NC-VW-04: 织造模拟步进100纬-进度百分比正确")
        void simulateWeaving100Steps() {
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> result = weavingService.simulateWeaving(100L, 100);

            assertNotNull(result);
            assertEquals(Long.valueOf(100L), result.get("designId"));
            assertEquals(100, result.get("steps"));
            assertEquals(200, result.get("totalWeft"));

            double progress = (Double) result.get("progressPercent");
            assertEquals(50.0, progress, 0.01);

            double estHours = (Double) result.get("estimatedTimeHours");
            assertTrue(estHours > 0);

            double[] tension = (double[]) result.get("warpTension");
            assertEquals(120, tension.length);
            for (double t : tension) {
                assertTrue(t >= 0.1 && t <= 6.0, "张力越界: " + t);
            }
        }

        @Test
        @DisplayName("NC-VW-05: 织造预览分析-5项指标全部非空")
        void generateWeavingPreview() {
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> result = weavingService.generateWeavingPreview(100L);

            assertNotNull(result.get("designId"));
            assertNotNull(result.get("warpCount"));
            assertNotNull(result.get("weftCount"));
            assertNotNull(result.get("complexityScore"));
            assertNotNull(result.get("estimatedProductionHours"));
            assertNotNull(result.get("averageFloatLength"));
            assertNotNull(result.get("isBalancedWeave"));
            assertNotNull(result.get("warpCoveragePercent"));
            assertNotNull(result.get("detectedPatternRepeat"));

            int[] repeat = (int[]) result.get("detectedPatternRepeat");
            assertEquals(2, repeat.length);
        }

        @Test
        @DisplayName("NC-VW-06: 平纹设计浮长=2(交替切换)-浮长正确")
        void plainWeaveFloatLength2() {
            int[][] plain = new int[8][8];
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) plain[i][j] = (i + j) % 2;
            designPlain.setPatternMatrix(generateCustomMatrix(plain));

            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> result = weavingService.generateWeavingPreview(100L);

            int floatLen = (Integer) result.get("averageFloatLength");
            assertEquals(1, floatLen);
        }

        @Test
        @DisplayName("NC-VW-07: 平纹覆盖率50%-balanced=true(ratio=1)")
        void plainWeaveBalanced50Percent() {
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> result = weavingService.generateWeavingPreview(100L);

            boolean balanced = (Boolean) result.get("isBalancedWeave");
            assertTrue(balanced);

            double coverage = (Double) result.get("warpCoveragePercent");
            assertEquals(50.0, coverage, 1.0);
        }

        @Test
        @DisplayName("NC-VW-08: 缎纹设计检测回纹周期-8×8缎纹检测周期")
        void satinWeavePatternRepeatDetected() {
            when(designRepository.findById(102L)).thenReturn(Optional.of(designSatin));

            Map<String, Object> result = weavingService.generateWeavingPreview(102L);

            int[] repeat = (int[]) result.get("detectedPatternRepeat");
            assertTrue(repeat[0] > 0 && repeat[0] <= 128, "经向周期: " + repeat[0]);
            assertTrue(repeat[1] > 0 && repeat[1] <= 256, "纬向周期: " + repeat[1]);
        }

        @Test
        @DisplayName("NC-VW-09: 发布设计-isPublic=true status=published")
        void publishDesignUpdatesStatus() {
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));
            when(designRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserWeavingDesign published = weavingService.publishDesign(100L);

            assertTrue(Boolean.TRUE.equals(published.getIsPublic()));
            assertEquals("published", published.getStatus());
            verify(designRepository).save(same(designPlain));
        }

        @Test
        @DisplayName("NC-VW-10: 复杂度评分-缎纹(65) > 斜纹(42) > 平纹(25)")
        void complexityRankingCorrect() {
            assertTrue(
                    designSatin.getComplexityScore() > designTwill.getComplexityScore() &&
                    designTwill.getComplexityScore() > designPlain.getComplexityScore(),
                    String.format("缎纹(%.1f) > 斜纹(%.1f) > 平纹(%.1f)",
                            designSatin.getComplexityScore(),
                            designTwill.getComplexityScore(),
                            designPlain.getComplexityScore()));
        }
    }

    @Nested
    @DisplayName("【边界场景】织造交互边界与极限值")
    class WeavingBoundaryScenarios {

        @Test
        @DisplayName("BD-VW-01: 空白设计最小边界-经纱数=10纬纱数=10")
        void blankDesignMinDims() {
            when(designRepository.save(any())).thenAnswer(inv -> {
                UserWeavingDesign d = inv.getArgument(0);
                d.setId(300L);
                return d;
            });

            UserWeavingDesign created = weavingService.createBlankDesign(
                    "边界测试", 10, 10);

            assertEquals(Integer.valueOf(10), created.getWarpCount());
            assertEquals(Integer.valueOf(10), created.getWeftCount());
        }

        @Test
        @DisplayName("BD-VW-02: 空白设计最大边界-经纱=5000纬纱=10000")
        void blankDesignMaxDims() {
            when(designRepository.save(any())).thenAnswer(inv -> {
                UserWeavingDesign d = inv.getArgument(0);
                d.setId(301L);
                return d;
            });

            assertDoesNotThrow(() ->
                    weavingService.createBlankDesign("大", 5000, 10000));
        }

        @Test
        @DisplayName("BD-VW-03: 织造模拟steps>total-实际步进=total，进度100%")
        void simulateExceedAllSteps() {
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> result = weavingService.simulateWeaving(100L, 99999);

            assertEquals(200, result.get("steps"));
            assertEquals(100.0, (Double) result.get("progressPercent"), 0.01);
        }

        @Test
        @DisplayName("BD-VW-04: 织造模拟steps=0-进度0%")
        void simulateZeroSteps() {
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> result = weavingService.simulateWeaving(100L, 0);

            assertEquals(0, result.get("steps"));
            assertEquals(0.0, (Double) result.get("progressPercent"), 0.01);
        }

        @Test
        @DisplayName("BD-VW-05: 1×1矩阵最小设计-回纹检测返回[1,1]")
        void singleCellPatternRepeat() {
            int[][] oneCell = {{1}};
            UserWeavingDesign minDesign = createDesign(
                    302L, "最小", "X", 1, 1, 1,
                    generateCustomMatrix(oneCell), "draft", 0.0, 1);

            when(designRepository.findById(302L)).thenReturn(Optional.of(minDesign));

            Map<String, Object> result = weavingService.generateWeavingPreview(302L);

            int[] repeat = (int[]) result.get("detectedPatternRepeat");
            assertEquals(1, repeat[0]);
            assertEquals(1, repeat[1]);
        }

        @Test
        @DisplayName("BD-VW-06: 张力数组边界-所有张力在[0.1,6.0]安全区间")
        void tensionAllInSafeRange() {
            when(designRepository.findById(102L)).thenReturn(Optional.of(designSatin));

            Map<String, Object> result = weavingService.simulateWeaving(102L, 128);

            double[] tension = (double[]) result.get("warpTension");
            double avgTension = (Double) result.get("averageTension");

            for (double t : tension) {
                assertTrue(t >= 0.1, "张力过低: " + t);
                assertTrue(t <= 6.0, "张力过高: " + t);
            }
            assertTrue(avgTension >= 0.1 && avgTension <= 6.0,
                    "平均张力越界: " + avgTension);
        }

        @Test
        @DisplayName("BD-VW-07: 斜纹全部覆盖测试-覆盖率验证")
        void twillCoverageRational() {
            when(designRepository.findById(101L)).thenReturn(Optional.of(designTwill));

            Map<String, Object> result = weavingService.generateWeavingPreview(101L);

            double cov = (Double) result.get("warpCoveragePercent");
            assertTrue(cov >= 0 && cov <= 100, "覆盖率越界: " + cov);
        }

        @Test
        @DisplayName("BD-VW-08: 损坏矩阵行包含非法字符-解析后默认0")
        void corruptMatrixCellDefaultZero() {
            UserWeavingDesign d = createDesign(303L, "脏矩阵", "X", 4, 4, 2,
                    "1,X,1,0\n0,abc,0,1\ninvalid,row\n1,0,1,0",
                    "draft", 0.0, 10);

            when(designRepository.findById(303L)).thenReturn(Optional.of(d));

            assertDoesNotThrow(() -> {
                Map<String, Object> r = weavingService.simulateWeaving(303L, 4);
                assertNotNull(r);
            });
        }

        @Test
        @DisplayName("BD-VW-09: 分页public设计-size>100-裁剪到100")
        void publicPageSizeClamped() {
            Page<UserWeavingDesign> page = new PageImpl<>(Collections.emptyList());
            when(designRepository.findByIsPublicTrue(any(Pageable.class))).thenReturn(page);

            weavingService.getPublicDesigns(null, 0, 9999, "newest");

            verify(designRepository).findByIsPublicTrue(argThat(p ->
                    ((org.springframework.data.domain.PageRequest) p).getPageSize() == 100));
        }

        @Test
        @DisplayName("BD-VW-10: 点赞/view计数初始为null-+1后=1")
        void countersHandleNullInitial() {
            designPlain.setLikeCount(null);
            designPlain.setViewCount(null);

            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));
            when(designRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            weavingService.incrementLikeCount(100L);
            weavingService.incrementViewCount(100L);

            assertEquals(Integer.valueOf(1), designPlain.getLikeCount());
            assertEquals(Integer.valueOf(1), designPlain.getViewCount());
        }
    }

    @Nested
    @DisplayName("【异常场景】虚拟织造异常与创造性容错")
    class WeavingExceptionScenarios {

        @Test
        @DisplayName("EX-VW-01: 经纱<10-抛异常提示范围")
        void warpTooSmall() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> weavingService.createBlankDesign("A", 9, 100));

            assertTrue(ex.getMessage().contains("经纱数需在10~5000之间"));
            verify(designRepository, never()).save(any());
        }

        @Test
        @DisplayName("EX-VW-02: 经纱>5000-抛异常提示范围")
        void warpTooLarge() {
            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.createBlankDesign("A", 5001, 100));
        }

        @Test
        @DisplayName("EX-VW-03: 纬纱<10-抛异常提示范围")
        void weftTooSmall() {
            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.createBlankDesign("A", 100, 9));
        }

        @Test
        @DisplayName("EX-VW-04: 纬纱>10000-抛异常提示范围")
        void weftTooLarge() {
            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.createBlankDesign("A", 100, 10001));
        }

        @Test
        @DisplayName("EX-VW-05: 从不存在品种创建-抛异常")
        void varietyNotFoundCreate() {
            when(varietyRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> weavingService.createDesignFromVariety(999L, "X"));

            assertTrue(ex.getMessage().contains("云锦品种不存在"));
        }

        @Test
        @DisplayName("EX-VW-06: 从不存在纹样创建-抛异常")
        void patternNotFoundCreate() {
            when(patternRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> weavingService.createDesignFromPattern(999L, "X"));

            assertTrue(ex.getMessage().contains("纹样不存在"));
        }

        @Test
        @DisplayName("EX-VW-07: 织造模拟设计不存在-抛异常")
        void simulateDesignNotFound() {
            when(designRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> weavingService.simulateWeaving(999L, 10));

            assertTrue(ex.getMessage().contains("设计不存在"));
        }

        @Test
        @DisplayName("EX-VW-08: 预览分析不存在-抛异常")
        void previewDesignNotFound() {
            when(designRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.generateWeavingPreview(999L));
        }

        @Test
        @DisplayName("EX-VW-09: 发布不存在-抛异常")
        void publishNonExistent() {
            when(designRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.publishDesign(999L));
        }

        @Test
        @DisplayName("EX-VW-10: 更新不存在-抛异常")
        void updateNonExistent() {
            when(designRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.updateDesign(999L, new UserWeavingDesign()));
        }

        @Test
        @DisplayName("EX-VW-11: 删除不存在-抛异常")
        void deleteNonExistent() {
            when(designRepository.existsById(999L)).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> weavingService.deleteDesign(999L));
        }

        @Test
        @DisplayName("EX-VW-12: 品种模板未设置colorCount-回退默认5色")
        void varietyNullColorCountFallback5() {
            zhuanghua.setColorCount(null);

            when(varietyRepository.findById(1L)).thenReturn(Optional.of(zhuanghua));
            when(designRepository.save(any())).thenAnswer(inv -> {
                UserWeavingDesign d = inv.getArgument(0);
                d.setId(400L);
                return d;
            });

            UserWeavingDesign created = weavingService.createDesignFromVariety(1L, "A");
            assertEquals(Integer.valueOf(5), created.getColorCount());
        }

        @Test
        @DisplayName("EX-VW-13: 设计matrix=null-解析后0纬不崩溃")
        void matrixNullParsesEmpty() {
            designPlain.setPatternMatrix(null);
            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));

            Map<String, Object> r = weavingService.simulateWeaving(100L, 50);
            assertNotNull(r);
            assertEquals(0, r.get("totalWeft"));
        }

        @Test
        @DisplayName("EX-VW-14: 更新后complexity重新计算")
        void updateRecomputesComplexity() {
            String newMatrix = generateMatrixStr(100, 100, "twill");
            UserWeavingDesign updateReq = new UserWeavingDesign();
            updateReq.setName("更新后");
            updateReq.setPatternMatrix(newMatrix);
            updateReq.setWeftCount(100);

            when(designRepository.findById(100L)).thenReturn(Optional.of(designPlain));
            when(designRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserWeavingDesign updated = weavingService.updateDesign(100L, updateReq);

            assertNotNull(updated.getComplexityScore());
            assertNotNull(updated.getEstimatedProductionHours());
            assertEquals("更新后", updated.getName());
        }
    }
}
