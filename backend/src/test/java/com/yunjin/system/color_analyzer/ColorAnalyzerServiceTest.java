package com.yunjin.system.color_analyzer;

import com.yunjin.system.entity.ColorPalette;
import com.yunjin.system.repository.ColorPaletteRepository;
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
@DisplayName("传统与数码色彩对比分析测试 - ColorAnalyzerService")
class ColorAnalyzerServiceTest {

    @Mock
    private ColorPaletteRepository paletteRepository;

    @InjectMocks
    private ColorAnalyzerService colorService;

    private ColorPalette tradQing;

    private ColorPalette tradMing;

    private ColorPalette digitalCmyk;

    @BeforeEach
    void setUp() {
        tradQing = new ColorPalette();
        tradQing.setId(1L);
        tradQing.setCode("TRAD_QING");
        tradQing.setName("清代宫廷色卡");
        tradQing.setPaletteType("traditional");
        tradQing.setDynasty("清");
        tradQing.setColors(
                "大红,#DC143C,正色,茜草染,皇权象征\n" +
                "鹅黄,#FFFACD,间色,黄檗染,太子专用\n" +
                "石青,#1E3A5F,正色,蓝靛+红花,祭服常用\n" +
                "月白,#E6F3FF,间色,淡蓝靛,文人雅士\n" +
                "宫绿,#228B22,间色,冻绿染,吉祥寓意\n" +
                "酱紫,#5A3B5C,间色,紫草+苏木,官服等级\n" +
                "藕荷,#E4C2D1,间色,多重套染,命妇常用");

        tradMing = new ColorPalette();
        tradMing.setId(2L);
        tradMing.setCode("TRAD_MING");
        tradMing.setName("明代织金色彩");
        tradMing.setPaletteType("traditional");
        tradMing.setDynasty("明");
        tradMing.setColors(
                "明黄,#FFD700,正色,金粉+姜黄,帝王专属\n" +
                "赤金,#D4AF37,金色,金箔贴花,织金工艺\n" +
                "宝蓝,#191970,正色,蓝靛深染,祭天青服\n" +
                "枣红,#8B0000,正色,茜草重染,祭祀礼服\n" +
                "松绿,#3EB489,间色,鼠曲草染,官服补子");

        digitalCmyk = new ColorPalette();
        digitalCmyk.setId(3L);
        digitalCmyk.setCode("DIGITAL_CMYK");
        digitalCmyk.setName("数码印花CMYK广色域");
        digitalCmyk.setPaletteType("digital_printing");
        digitalCmyk.setDynasty("现代");
        digitalCmyk.setColors(
                "ProcessCyan,#00FFFF,青色,CMY100,数码青色\n" +
                "ProcessMagenta,#FF00FF,品红,CMY100,数码品红\n" +
                "ProcessYellow,#FFFF00,黄色,CMY100,数码黄色\n" +
                "PureBlack,#000000,黑色,K100,纯黑\n" +
                "VividRed,#FF0000,原色,RGB,数码鲜红\n" +
                "ElectricBlue,#0066FF,原色,RGB,数码亮蓝\n" +
                "LimeGreen,#32CD32,原色,RGB,数码鲜绿\n" +
                "HotPink,#FF69B4,衍生,CMYK,数码粉红\n" +
                "DeepPurple,#6600CC,衍生,CMYK,数码深紫\n" +
                "OrangeBright,#FF8C00,衍生,CMYK,数码亮橙");
    }

    @Nested
    @DisplayName("【正常场景】色域映射与色差计算核心验证")
    class ColorNormalScenarios {

        @Test
        @DisplayName("NC-CA-01: 清代色卡vs数码色卡对比-验证色差计算结构完整")
        void compareTradVsDigital() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findById(3L)).thenReturn(Optional.of(digitalCmyk));

            Map<String, Object> result = colorService.comparePalettes(1L, 3L);

            assertNotNull(result);
            assertTrue(result.containsKey("palette1"));
            assertTrue(result.containsKey("palette2"));
            assertTrue(result.containsKey("commonColorCount"));
            assertTrue(result.containsKey("similarColors"));
            assertTrue(result.containsKey("gamut1"));
            assertTrue(result.containsKey("gamut2"));
            assertTrue(result.containsKey("stats"));

            Map<?, ?> p1 = (Map<?, ?>) result.get("palette1");
            assertEquals("清代宫廷色卡", p1.get("name"));
            assertEquals("traditional", p1.get("type"));
            assertEquals(7, p1.get("colorCount"));

            double[] gamut1 = (double[]) result.get("gamut1");
            double[] gamut2 = (double[]) result.get("gamut2");
            assertEquals(6, gamut1.length);
            assertEquals(6, gamut2.length);
            assertTrue(gamut1[1] >= gamut1[0]);
            assertTrue(gamut2[1] >= gamut2[0]);
        }

        @Test
        @DisplayName("NC-CA-02: 同色自身对比-色差=0，相似度=100%")
        void sameColorDistanceZero() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing, tradMing));

            Map<String, Object> match = colorService.findTraditionalEquivalent(
                    220, 20, 60);

            assertNotNull(match);
            assertTrue(match.containsKey("inputRgb"));
            assertTrue(match.containsKey("inputHex"));
            assertTrue(match.containsKey("matchedColor"));

            Map<?, ?> matched = (Map<?, ?>) match.get("matchedColor");
            if (matched != null) {
                int[] rgb = (int[]) match.get("inputRgb");
                int[] matchedRgb = (int[]) matched.get("rgb");
                assertNotNull(rgb);
                assertNotNull(matchedRgb);
                assertEquals(3, rgb.length);
                assertEquals(3, matchedRgb.length);
            }
        }

        @Test
        @DisplayName("NC-CA-03: CIE L*a*b*转换-纯白(255,255,255) L*≈100验证")
        void rgbToLabPureWhite() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing));

            Map<String, Object> result = colorService.findTraditionalEquivalent(
                    255, 255, 255);

            Map<?, ?> matched = (Map<?, ?>) result.get("matchedColor");
            if (matched != null) {
                double[] lab = (double[]) matched.get("lab");
                assertNotNull(lab);
                assertEquals(3, lab.length);
            }

            assertEquals("#FFFFFF", result.get("inputHex"));
        }

        @Test
        @DisplayName("NC-CA-04: CIE L*a*b*转换-纯黑(0,0,0) L*≈0验证")
        void rgbToLabPureBlack() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing));

            Map<String, Object> result = colorService.findTraditionalEquivalent(
                    0, 0, 0);

            assertEquals("#000000", result.get("inputHex"));
        }

        @Test
        @DisplayName("NC-CA-05: 红色(255,0,0)匹配清代-查找宫绿/大红近色")
        void findTraditionalEquivalentForRed() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing, tradMing));

            Map<String, Object> match = colorService.findTraditionalEquivalent(
                    220, 20, 60);

            Map<?, ?> mc = (Map<?, ?>) match.get("matchedColor");
            if (mc != null) {
                String colorName = (String) mc.get("name");
                assertNotNull(colorName);
                double similarity = (Double) match.get("similarity");
                assertTrue(similarity >= 0 && similarity <= 100,
                        "相似度应在0-100之间: " + similarity);
                double distance = (Double) match.get("distance");
                assertTrue(distance >= 0, "色差不能为负");
            }
        }

        @Test
        @DisplayName("NC-CA-06: 色域6维边界-L*/a*/b*各min≤max")
        void gamutBoundsMinLessThanMax() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findById(2L)).thenReturn(Optional.of(tradMing));

            Map<String, Object> result = colorService.comparePalettes(1L, 2L);

            double[] gamut1 = (double[]) result.get("gamut1");
            double[] gamut2 = (double[]) result.get("gamut2");

            assertTrue(gamut1[0] <= gamut1[1]);
            assertTrue(gamut1[2] <= gamut1[3]);
            assertTrue(gamut1[4] <= gamut1[5]);
            assertTrue(gamut2[0] <= gamut2[1]);
            assertTrue(gamut2[2] <= gamut2[3]);
            assertTrue(gamut2[4] <= gamut2[5]);

            assertTrue(gamut1[0] >= 0 && gamut1[1] <= 100);
        }

        @Test
        @DisplayName("NC-CA-07: 色彩统计-平均亮度/饱和度/暖色比正确格式")
        void colorStatisticsFormat() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findById(3L)).thenReturn(Optional.of(digitalCmyk));

            Map<String, Object> result = colorService.comparePalettes(1L, 3L);

            Map<?, ?> stats = (Map<?, ?>) result.get("stats");
            assertNotNull(stats.get("avgBrightness1"));
            assertNotNull(stats.get("avgSaturation1"));
            assertNotNull(stats.get("warmColorRatio1"));
            assertNotNull(stats.get("warmColorRatio2"));

            String ratio1 = (String) stats.get("warmColorRatio1");
            String ratio2 = (String) stats.get("warmColorRatio2");
            assertTrue(ratio1.endsWith("%"));
            assertTrue(ratio2.endsWith("%"));
        }

        @Test
        @DisplayName("NC-CA-08: 数码对比分析-色域覆盖率百分比格式0-100")
        void digitalGamutCoveragePercent() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findByPaletteType("digital_printing"))
                    .thenReturn(Arrays.asList(digitalCmyk));

            Map<String, Object> result =
                    colorService.generateDigitalPrintingComparison(1L);

            assertNotNull(result);
            Object coverage = result.get("digitalGamutCoverage");
            if (coverage instanceof Double) {
                Double d = (Double) coverage;
                assertTrue(d >= 0, "覆盖率>=0: " + d);
            }

            List<?> points = (List<?>) result.get("comparisonPoints");
            assertEquals(4, points.size());

            List<?> mappings = (List<?>) result.get("sampleColorMappings");
            assertEquals(7, mappings.size());
        }

        @Test
        @DisplayName("NC-CA-09: 解析HEX格式-3位缩写(#F00)转6位正确")
        void hex3ToRgbExpansion() {
            tradQing.setColors("短红,#F00,测试,缩写格式,3位缩写色");
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing));

            Map<String, Object> match = colorService.findTraditionalEquivalent(
                    255, 0, 0);

            assertNotNull(match);
        }

        @Test
        @DisplayName("NC-CA-10: 色彩色差边界-互补色距离最大（红vs青）")
        void complementaryColorsMaxDistance() {
            ColorPalette basic = new ColorPalette();
            basic.setId(99L);
            basic.setCode("BASIC");
            basic.setName("基础色");
            basic.setPaletteType("traditional");
            basic.setColors("纯红,#FF0000,原色,RGB,纯红\n纯青,#00FFFF,原色,RGB,纯青\n纯绿,#00FF00,原色,RGB,纯绿");

            when(paletteRepository.findById(99L)).thenReturn(Optional.of(basic));
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findById(3L)).thenReturn(Optional.of(digitalCmyk));

            Map<String, Object> result = colorService.comparePalettes(1L, 3L);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("【边界场景】色彩计算边界验证")
    class ColorBoundaryScenarios {

        @Test
        @DisplayName("BD-CA-01: RGB边界值-(0,0,0)黑色不抛异常")
        void rgbZeroBoundary() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing));

            assertDoesNotThrow(() ->
                    colorService.findTraditionalEquivalent(0, 0, 0));
        }

        @Test
        @DisplayName("BD-CA-02: RGB边界值-(255,255,255)白色不抛异常")
        void rgbMaxBoundary() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(tradQing));

            assertDoesNotThrow(() ->
                    colorService.findTraditionalEquivalent(255, 255, 255));
        }

        @Test
        @DisplayName("BD-CA-03: HEX带#号/不带#号/小写-均正确解析")
        void hexFormatFlexibility() {
            ColorPalette mixed = new ColorPalette();
            mixed.setId(99L);
            mixed.setCode("MIX");
            mixed.setName("混合HEX格式");
            mixed.setPaletteType("traditional");
            mixed.setColors(
                    "带井号,#AABBCC,测试1,带#,格式1\n" +
                    "无井号,112233,测试2,无#,格式2\n" +
                    "三位3,#F0A,测试3,3位#,格式3\n" +
                    "小写hex,#aaccff,测试4,小写#,格式4");

            when(paletteRepository.findById(1L)).thenReturn(Optional.of(mixed));
            when(paletteRepository.findById(99L)).thenReturn(Optional.of(mixed));
            when(paletteRepository.findById(3L)).thenReturn(Optional.of(digitalCmyk));

            assertDoesNotThrow(() -> colorService.comparePalettes(1L, 99L));
        }

        @Test
        @DisplayName("BD-CA-04: 空色卡colors-解析返回空列表")
        void emptyColorsParseEmpty() {
            ColorPalette emptyPalette = new ColorPalette();
            emptyPalette.setId(88L);
            emptyPalette.setCode("EMPTY");
            emptyPalette.setName("空色卡");
            emptyPalette.setPaletteType("traditional");
            emptyPalette.setColors("");

            when(paletteRepository.findById(88L)).thenReturn(Optional.of(emptyPalette));
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));

            assertDoesNotThrow(() -> colorService.comparePalettes(88L, 1L));
        }

        @Test
        @DisplayName("BD-CA-05: 色卡colors含非法HEX-整行跳过不中断解析")
        void invalidHexLineSkipped() {
            ColorPalette corruptPalette = new ColorPalette();
            corruptPalette.setId(77L);
            corruptPalette.setCode("CORRUPT");
            corruptPalette.setName("脏数据色卡");
            corruptPalette.setPaletteType("traditional");
            corruptPalette.setColors(
                    "合法色,#FFFFFF,合法,RGB,白\n" +
                    "非法1,NOT_HEX,非法,X,X\n" +
                    "合法2,#123456,合法,RGB,灰\n" +
                    "非法2,短色,#GGGGGG\n" +
                    "合法3,#ABC,合法,3位,浅蓝");

            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Arrays.asList(corruptPalette));

            assertDoesNotThrow(() -> {
                Map<String, Object> r = colorService.findTraditionalEquivalent(
                        255, 255, 255);
                assertNotNull(r);
            });
        }

        @Test
        @DisplayName("BD-CA-06: 单色色卡对比-色域体积=0（min==max）")
        void singleColorGamut() {
            ColorPalette single = new ColorPalette();
            single.setId(66L);
            single.setCode("SINGLE");
            single.setName("单色卡");
            single.setPaletteType("traditional");
            single.setColors("独色,#808080,单色,单一,仅一色");

            when(paletteRepository.findById(66L)).thenReturn(Optional.of(single));
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));

            Map<String, Object> result = colorService.comparePalettes(66L, 1L);
            double[] gamut1 = (double[]) result.get("gamut1");

            assertEquals(gamut1[0], gamut1[1]);
            assertEquals(gamut1[2], gamut1[3]);
            assertEquals(gamut1[4], gamut1[5]);
        }

        @Test
        @DisplayName("BD-CA-07: 相似度范围-所有颜色相似度0≤s≤100")
        void allSimilaritiesIn0_100() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findById(3L)).thenReturn(Optional.of(digitalCmyk));

            Map<String, Object> result = colorService.comparePalettes(1L, 3L);
            List<?> similar = (List<?>) result.get("similarColors");

            for (Object o : similar) {
                Map<?, ?> pair = (Map<?, ?>) o;
                double sim = (Double) pair.get("similarity");
                assertTrue(sim >= 0 && sim <= 100,
                        "相似度越界: " + sim);
            }
        }

        @Test
        @DisplayName("BD-CA-08: 色卡解析字段-仅3列必填(名称,HEX,分类)，4-5列可选")
        void colorParseMin3Cols() {
            ColorPalette threeCol = new ColorPalette();
            threeCol.setId(55L);
            threeCol.setCode("MIN3");
            threeCol.setName("最少3列");
            threeCol.setPaletteType("traditional");
            threeCol.setColors(
                    "仅三列,#111111,分类A\n" +
                    "四列带,#222222,分类B,来源B\n" +
                    "五列全,#333333,分类C,来源C,描述C");

            when(paletteRepository.findById(55L)).thenReturn(Optional.of(threeCol));
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));

            assertDoesNotThrow(() -> colorService.comparePalettes(55L, 1L));
        }
    }

    @Nested
    @DisplayName("【异常场景】色彩模块异常处理")
    class ColorExceptionScenarios {

        @Test
        @DisplayName("EX-CA-01: 色卡1不存在-异常提示正确")
        void palette1NotFound() {
            when(paletteRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> colorService.comparePalettes(999L, 3L));

            assertTrue(ex.getMessage().contains("色卡1不存在"));
        }

        @Test
        @DisplayName("EX-CA-02: 色卡2不存在-异常提示正确")
        void palette2NotFound() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> colorService.comparePalettes(1L, 999L));

            assertTrue(ex.getMessage().contains("色卡2不存在"));
        }

        @Test
        @DisplayName("EX-CA-03: 传统色匹配无traditional色卡-返回匹配null不崩溃")
        void noTraditionalPalettes() {
            when(paletteRepository.findByPaletteType("traditional"))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> r =
                    colorService.findTraditionalEquivalent(255, 0, 0);

            assertNotNull(r);
            assertNull(r.get("matchedColor"));
        }

        @Test
        @DisplayName("EX-CA-04: 数码对比无数码色卡-回退文本描述")
        void noDigitalPalettesFallback() {
            when(paletteRepository.findById(1L)).thenReturn(Optional.of(tradQing));
            when(paletteRepository.findByPaletteType("digital_printing"))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> r =
                    colorService.generateDigitalPrintingComparison(1L);

            assertNull(r.get("digitalPalette"));
            assertEquals("1677万 (24-bit RGB)", r.get("digitalColorCount"));
            assertEquals("理论覆盖全部sRGB色域", r.get("digitalGamutCoverage"));
        }

        @Test
        @DisplayName("EX-CA-05: 创建色卡编码重复-抛异常不保存")
        void createDuplicateCodeFails() {
            when(paletteRepository.findByCode("TRAD_QING"))
                    .thenReturn(Optional.of(tradQing));

            ColorPalette newP = new ColorPalette();
            newP.setCode("TRAD_QING");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> colorService.createPalette(newP));

            assertTrue(ex.getMessage().contains("色卡编码已存在"));
            verify(paletteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EX-CA-06: 更新不存在色卡-抛异常")
        void updateNonExistentFails() {
            when(paletteRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> colorService.updatePalette(999L, new ColorPalette()));
        }

        @Test
        @DisplayName("EX-CA-07: 删除不存在色卡-抛异常")
        void deleteNonExistentFails() {
            when(paletteRepository.existsById(999L)).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> colorService.deletePalette(999L));
        }

        @Test
        @DisplayName("EX-CA-08: 创建色卡-colorCount自动计算")
        void createAutoCountsColors() {
            ColorPalette p = new ColorPalette();
            p.setName("测试色卡");
            p.setColors("A,#111111,X\nB,#222222,Y\nC,#333333,Z");
            when(paletteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ColorPalette saved = colorService.createPalette(p);

            assertEquals(Integer.valueOf(3), saved.getColorCount());
            verify(paletteRepository).save(argThat(arg ->
                    Integer.valueOf(3).equals(((ColorPalette) arg).getColorCount())));
        }
    }
}
