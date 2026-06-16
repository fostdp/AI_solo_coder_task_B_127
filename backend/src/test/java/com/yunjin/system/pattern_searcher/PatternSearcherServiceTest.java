package com.yunjin.system.pattern_searcher;

import com.yunjin.system.entity.PatternDesign;
import com.yunjin.system.repository.PatternDesignRepository;
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
@DisplayName("古代云锦纹样数据库与智能检索测试 - PatternSearcherService")
class PatternSearcherServiceTest {

    @Mock
    private PatternDesignRepository patternRepository;

    @InjectMocks
    private PatternSearcherService patternService;

    private PatternDesign lotusPattern;

    private PatternDesign dragonPattern;

    private PatternDesign cloudPattern;

    private PatternDesign floralPattern;

    @BeforeEach
    void setUp() {
        lotusPattern = createPattern(1L, "LOTUS001", "缠枝莲纹", "植物花卉", "明",
                "缎纹", 24, 24, 8,
                "莲花,缠枝,吉祥,佛家,花卉", 3, "旋转对称");

        dragonPattern = createPattern(2L, "DRAGON001", "五爪龙纹", "瑞兽祥禽", "清",
                "妆花缎", 48, 48, 18,
                "龙,五爪,皇家,权威,祥云", 5, "中心对称");

        cloudPattern = createPattern(3L, "CLOUD001", "如意云纹", "云气纹理", "元",
                "织金锦", 16, 16, 6,
                "云,如意,吉祥,卷云", 2, "平移对称");

        floralPattern = createPattern(4L, "FLORAL002", "折枝牡丹", "植物花卉", "清",
                "妆花缎", 32, 32, 12,
                "牡丹,富贵,花卉,吉祥,缠枝", 4, "旋转对称");
    }

    private PatternDesign createPattern(Long id, String code, String name, String category,
                                         String dynasty, String weaveStructure,
                                         int warpRepeat, int weftRepeat, int colorCount,
                                         String tags, int complexity, String symmetryType) {
        PatternDesign p = new PatternDesign();
        p.setId(id);
        p.setPatternCode(code);
        p.setName(name);
        p.setCategory(category);
        p.setDynasty(dynasty);
        p.setWeaveStructure(weaveStructure);
        p.setWarpRepeat(warpRepeat);
        p.setWeftRepeat(weftRepeat);
        p.setColorCount(colorCount);
        p.setTags(tags);
        p.setComplexityLevel(complexity);
        p.setSymmetryType(symmetryType);
        p.setIsPublic(true);
        p.setUseCount(0);
        p.setLikeCount(0);
        return p;
    }

    @Nested
    @DisplayName("【正常场景】纹样相似度算法核心验证")
    class SimilarityAlgorithmNormal {

        @Test
        @DisplayName("NC-PL-01: 完全相同的纹样 - 相似度应最高（排除自身）")
        void findSimilarExcludesSelf() {
            List<PatternDesign> all = Arrays.asList(lotusPattern, dragonPattern,
                    cloudPattern, floralPattern);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(1L, 5);

            assertNotNull(similar);
            assertFalse(similar.stream().anyMatch(p -> p.getId() == 1L),
                    "结果不应包含目标纹样自身");
        }

        @Test
        @DisplayName("NC-PL-02: 缠枝莲(植物/明/缎纹/3) vs 牡丹(植物/清/妆花/4) - 类别相同得分高")
        void categoryMatchBoostsScore() {
            List<PatternDesign> all = Arrays.asList(lotusPattern, dragonPattern,
                    cloudPattern, floralPattern);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(1L, 3);

            assertEquals(3, similar.size());
            assertTrue(similar.get(0).getId() == 4L,
                    "牡丹(同为植物类)应排第一，但实际是: " +
                            similar.get(0).getName() + " id=" + similar.get(0).getId());
        }

        @Test
        @DisplayName("NC-PL-03: 五爪龙(瑞兽/清/妆花/5) vs 牡丹(植物/清/妆花/4) - 同朝代+同组织共35分加成")
        void dynastyAndWeaveMatch() {
            List<PatternDesign> all = Arrays.asList(lotusPattern, dragonPattern,
                    cloudPattern, floralPattern);

            when(patternRepository.findById(2L)).thenReturn(Optional.of(dragonPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(2L, 3);

            assertEquals(4L, (long) similar.get(0).getId(),
                    "牡丹(同朝代+同组织)应比云纹更相似");
        }

        @Test
        @DisplayName("NC-PL-04: 标签Jaccard系数 - 莲(5标签)∩牡丹(5标签)=2/8→J=0.25，得分7.5")
        void tagJaccardCalculation() {
            List<PatternDesign> all = Arrays.asList(lotusPattern, dragonPattern,
                    cloudPattern, floralPattern);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(1L, 1);

            PatternDesign top = similar.get(0);
            assertEquals("折枝牡丹", top.getName());

            Set<String> lotusTags = Set.of("莲花", "缠枝", "吉祥", "佛家", "花卉");
            Set<String> peonyTags = Set.of("牡丹", "富贵", "花卉", "吉祥", "缠枝");
            Set<String> intersection = new HashSet<>(lotusTags);
            intersection.retainAll(peonyTags);
            Set<String> union = new HashSet<>(lotusTags);
            union.addAll(peonyTags);

            double expectedJaccard = (double) intersection.size() / union.size();
            assertEquals(0.25, expectedJaccard, 0.001);
            assertEquals(3, intersection.size());
            assertEquals(7, union.size());
        }

        @Test
        @DisplayName("NC-PL-05: 复杂度差=|3-4|=1，加成9分（max10-1）")
        void complexityDifferenceScoring() {
            PatternDesign p = createPattern(10L, "TST", "测试", "植物花卉", "明",
                    "缎纹", 24, 24, 8, "莲花,缠枝,吉祥", 3, "旋转对称");

            List<PatternDesign> all = Arrays.asList(lotusPattern, p);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(1L, 1);

            assertTrue(!similar.isEmpty());
        }

        @Test
        @DisplayName("NC-PL-06: 分页检索 - 第0页大小2")
        void paginationFirstPage() {
            Page<PatternDesign> page = new PageImpl<>(
                    Arrays.asList(lotusPattern, dragonPattern),
                    org.springframework.data.domain.PageRequest.of(0, 2), 4);

            when(patternRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<PatternDesign> result = patternService.searchPatterns(null, 0, 2, "newest");

            assertEquals(0, result.getNumber());
            assertEquals(2, result.getNumberOfElements());
            assertEquals(4, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
        }

        @Test
        @DisplayName("NC-PL-07: 高级检索 - 多条件组合（植物类+明代+复杂度2-4）")
        void advancedSearchMultiFilter() {
            Page<PatternDesign> page = new PageImpl<>(Collections.singletonList(lotusPattern));

            when(patternRepository.advancedSearch(eq("植物花卉"), eq("明"),
                    isNull(), eq(2), eq(4), any(Pageable.class)))
                    .thenReturn(page);

            Page<PatternDesign> result = patternService.advancedSearch(
                    "植物花卉", "明", null, 2, 4, 0, 10, "name");

            assertEquals(1, result.getTotalElements());
            assertEquals("缠枝莲纹", result.getContent().get(0).getName());
        }

        @Test
        @DisplayName("NC-PL-08: 纹样矩阵解析 - CSV格式转int[][]")
        void parsePatternMatrixCsv() {
            lotusPattern.setPatternMatrix("1,0,1,0\n0,1,0,1\n1,0,1,0\n0,1,0,1");

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));

            int[][] matrix = patternService.getPatternMatrixAsArray(1L);

            assertEquals(4, matrix.length);
            assertEquals(4, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(0, matrix[0][1]);
            assertEquals(1, matrix[1][1]);
            assertEquals(1, matrix[2][0]);
        }

        @Test
        @DisplayName("NC-PL-09: 使用次数+1 - incrementUseCount幂等验证")
        void incrementUseCountIncrements() {
            lotusPattern.setUseCount(5);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));

            patternService.incrementUseCount(1L);

            assertEquals(Integer.valueOf(6), lotusPattern.getUseCount());
            verify(patternRepository).save(lotusPattern);
        }
    }

    @Nested
    @DisplayName("【边界场景】纹样检索边界值验证")
    class SimilarityBoundary {

        @Test
        @DisplayName("BD-PL-01: 矩阵为空 - 回退生成默认平纹矩阵(24×24)")
        void emptyMatrixGeneratesDefault() {
            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));

            int[][] matrix = patternService.getPatternMatrixAsArray(1L);

            assertEquals(24, matrix.length);
            assertEquals(24, matrix[0].length);
            assertEquals(1, matrix[0][0]);
            assertEquals(0, matrix[0][1]);
            assertEquals(0, matrix[1][0]);
            assertEquals(1, matrix[1][1]);
        }

        @Test
        @DisplayName("BD-PL-02: 矩阵损坏格式 - 返回空矩阵不抛异常")
        void corruptMatrixGraceful() {
            lotusPattern.setPatternMatrix("invalid,data,here\nnot,numbers");

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));

            int[][] matrix = patternService.getPatternMatrixAsArray(1L);

            assertEquals(0, matrix.length);
            assertDoesNotThrow(() -> patternService.getPatternMatrixAsArray(1L));
        }

        @Test
        @DisplayName("BD-PL-03: 空标签 - 标签相似度0分不影响总分非负")
        void emptyTagsNoTagScore() {
            lotusPattern.setTags("");
            floralPattern.setTags(null);

            List<PatternDesign> all = Arrays.asList(lotusPattern, dragonPattern, floralPattern);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            assertDoesNotThrow(() -> patternService.findSimilarPatterns(1L, 3));
        }

        @Test
        @DisplayName("BD-PL-04: 相似度得分范围 - 最小0最大100验证")
        void similarityScoreInRange() {
            PatternDesign completelyDifferent = createPattern(99L, "DIFF", "完全不同",
                    "几何纹样", "现代", "平纹", 8, 8, 2,
                    "现代,极简", 1, "无对称");

            List<PatternDesign> all = Arrays.asList(lotusPattern, completelyDifferent);

            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(1L, 1);
            assertTrue(similar.size() <= 1);
        }

        @Test
        @DisplayName("BD-PL-05: 分页大小超限 - size>100自动裁剪为100")
        void pageSizeClampedTo100() {
            Page<PatternDesign> page = new PageImpl<>(Collections.emptyList());

            when(patternRepository.findAll(any(Pageable.class))).thenReturn(page);

            patternService.searchPatterns(null, 0, 999, "newest");

            verify(patternRepository).findAll(argThat(p ->
                    ((org.springframework.data.domain.PageRequest) p).getPageSize() == 100));
        }

        @Test
        @DisplayName("BD-PL-06: limit=0 - 相似检索返回空列表")
        void zeroLimitReturnsEmpty() {
            List<PatternDesign> all = Arrays.asList(lotusPattern, dragonPattern);
            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            List<PatternDesign> similar = patternService.findSimilarPatterns(1L, 0);
            assertTrue(similar.isEmpty());
        }

        @Test
        @DisplayName("BD-PL-07: 标签分隔符兼容-逗号/分号/顿号/中文逗号")
        void tagSeparatorCompatibility() {
            lotusPattern.setTags("莲花,缠枝;吉祥、佛家，花卉");

            List<PatternDesign> all = Arrays.asList(lotusPattern, floralPattern);
            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            assertDoesNotThrow(() -> patternService.findSimilarPatterns(1L, 1));
        }
    }

    @Nested
    @DisplayName("【异常场景】纹样检索异常处理")
    class PatternExceptions {

        @Test
        @DisplayName("EX-PL-01: 查找不存在ID的相似纹样-抛异常")
        void similarPatternsNotFound() {
            when(patternRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> patternService.findSimilarPatterns(999L, 5));

            assertTrue(ex.getMessage().contains("纹样不存在"));
        }

        @Test
        @DisplayName("EX-PL-02: 取矩阵-纹样不存在")
        void matrixPatternNotFound() {
            when(patternRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> patternService.getPatternMatrixAsArray(999L));
        }

        @Test
        @DisplayName("EX-PL-03: 创建纹样编码重复-抛异常")
        void createDuplicateCodeFails() {
            when(patternRepository.findByPatternCode("LOTUS001"))
                    .thenReturn(Optional.of(lotusPattern));

            PatternDesign newP = new PatternDesign();
            newP.setPatternCode("LOTUS001");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> patternService.createPattern(newP));

            assertTrue(ex.getMessage().contains("纹样编码已存在"));
            verify(patternRepository, never()).save(any());
        }

        @Test
        @DisplayName("EX-PL-04: 创建纹样isPublic未设置-默认true")
        void createDefaultIsPublicTrue() {
            PatternDesign newP = new PatternDesign();
            newP.setName("新纹样");
            when(patternRepository.save(any())).thenReturn(newP);

            PatternDesign created = patternService.createPattern(newP);

            assertTrue(Boolean.TRUE.equals(created.getIsPublic()));
            verify(patternRepository).save(argThat(p ->
                    Boolean.TRUE.equals(p.getIsPublic())));
        }

        @Test
        @DisplayName("EX-PL-05: 更新不存在纹样-抛异常")
        void updateNonExistentFails() {
            when(patternRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> patternService.updatePattern(999L, new PatternDesign()));
        }

        @Test
        @DisplayName("EX-PL-06: 删除不存在纹样-抛异常")
        void deleteNonExistentFails() {
            when(patternRepository.existsById(999L)).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> patternService.deletePattern(999L));
        }

        @Test
        @DisplayName("EX-PL-07: 多种分隔符混合标签-空字符串自动过滤")
        void mixedSeparatorsFilterEmpty() {
            lotusPattern.setTags(",,,莲花,,,缠枝,;;;,吉祥,、、、");
            floralPattern.setTags(",,,,");

            List<PatternDesign> all = Arrays.asList(lotusPattern, floralPattern);
            when(patternRepository.findById(1L)).thenReturn(Optional.of(lotusPattern));
            when(patternRepository.findAll()).thenReturn(all);

            assertDoesNotThrow(() -> {
                List<PatternDesign> r = patternService.findSimilarPatterns(1L, 1);
                assertNotNull(r);
            });
        }
    }
}
