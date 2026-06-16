package com.yunjin.system.color_analyzer;

import com.yunjin.system.entity.ColorPalette;
import com.yunjin.system.repository.ColorPaletteRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ColorAnalyzerService {

    private final ColorPaletteRepository paletteRepository;

    public ColorAnalyzerService(ColorPaletteRepository paletteRepository) {
        this.paletteRepository = paletteRepository;
    }

    public List<ColorPalette> getAllPalettes(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return paletteRepository.searchByKeyword(keyword.trim());
        }
        return paletteRepository.findAll();
    }

    public Optional<ColorPalette> getPaletteById(Long id) {
        return paletteRepository.findById(id);
    }

    public Optional<ColorPalette> getPaletteByCode(String code) {
        return paletteRepository.findByCode(code);
    }

    public List<ColorPalette> getPalettesByType(String paletteType) {
        return paletteRepository.findByPaletteType(paletteType);
    }

    public List<String> getPaletteTypes() {
        return paletteRepository.findAllPaletteTypes();
    }

    public List<String> getDynasties() {
        return paletteRepository.findAllDynasties();
    }

    public ColorPalette createPalette(ColorPalette palette) {
        if (palette.getCode() != null &&
            paletteRepository.findByCode(palette.getCode()).isPresent()) {
            throw new IllegalArgumentException("色卡编码已存在");
        }
        updateColorCount(palette);
        return paletteRepository.save(palette);
    }

    public ColorPalette updatePalette(Long id, ColorPalette palette) {
        ColorPalette existing = paletteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("色卡不存在"));

        existing.setName(palette.getName());
        existing.setPaletteType(palette.getPaletteType());
        existing.setSource(palette.getSource());
        existing.setDynasty(palette.getDynasty());
        existing.setDescription(palette.getDescription());
        existing.setColors(palette.getColors());
        existing.setColorSpace(palette.getColorSpace());
        existing.setVarietyId(palette.getVarietyId());
        existing.setReferenceImageUrl(palette.getReferenceImageUrl());
        existing.setCulturalNotes(palette.getCulturalNotes());

        updateColorCount(existing);
        return paletteRepository.save(existing);
    }

    private void updateColorCount(ColorPalette palette) {
        if (palette.getColors() != null) {
            int count = parseColors(palette.getColors()).size();
            palette.setColorCount(count);
        }
    }

    public void deletePalette(Long id) {
        if (!paletteRepository.existsById(id)) {
            throw new IllegalArgumentException("色卡不存在");
        }
        paletteRepository.deleteById(id);
    }

    public Map<String, Object> comparePalettes(Long paletteId1, Long paletteId2) {
        ColorPalette p1 = paletteRepository.findById(paletteId1)
                .orElseThrow(() -> new IllegalArgumentException("色卡1不存在"));
        ColorPalette p2 = paletteRepository.findById(paletteId2)
                .orElseThrow(() -> new IllegalArgumentException("色卡2不存在"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("palette1", Map.of("id", p1.getId(), "name", p1.getName(),
                "type", p1.getPaletteType(), "colorCount", p1.getColorCount()));
        result.put("palette2", Map.of("id", p2.getId(), "name", p2.getName(),
                "type", p2.getPaletteType(), "colorCount", p2.getColorCount()));

        List<Map<String, Object>> colors1 = parseColors(p1.getColors());
        List<Map<String, Object>> colors2 = parseColors(p2.getColors());

        int commonCount = 0;
        List<Map<String, Object>> similarColors = new ArrayList<>();

        for (Map<String, Object> c1 : colors1) {
            double minDist = Double.MAX_VALUE;
            Map<String, Object> closest = null;
            for (Map<String, Object> c2 : colors2) {
                double dist = colorDistance(
                        (int[]) c1.get("rgb"),
                        (int[]) c2.get("rgb"));
                if (dist < minDist) {
                    minDist = dist;
                    closest = c2;
                }
            }
            if (minDist < 20) {
                commonCount++;
            }
            if (closest != null && minDist < 50) {
                Map<String, Object> pair = new LinkedHashMap<>();
                pair.put("color1", c1);
                pair.put("color2", closest);
                pair.put("distance", Math.round(minDist * 100.0) / 100.0);
                pair.put("similarity", Math.round((1 - minDist / 441.0) * 10000.0) / 100.0);
                similarColors.add(pair);
            }
        }

        result.put("commonColorCount", commonCount);
        result.put("similarColors", similarColors.stream()
                .sorted((a, b) -> Double.compare((double) b.get("similarity"),
                        (double) a.get("similarity")))
                .limit(15)
                .collect(Collectors.toList()));

        double[] gamut1 = calculateGamut(colors1);
        double[] gamut2 = calculateGamut(colors2);
        result.put("gamut1", gamut1);
        result.put("gamut2", gamut2);

        Map<String, Object> stats = generateColorStats(colors1, colors2);
        result.put("stats", stats);

        return result;
    }

    private Map<String, Object> generateColorStats(List<Map<String, Object>> colors1,
                                                    List<Map<String, Object>> colors2) {
        Map<String, Object> stats = new LinkedHashMap<>();

        double avgBrightness1 = colors1.stream()
                .mapToDouble(c -> getBrightness((int[]) c.get("rgb")))
                .average().orElse(0);
        double avgBrightness2 = colors2.stream()
                .mapToDouble(c -> getBrightness((int[]) c.get("rgb")))
                .average().orElse(0);
        stats.put("avgBrightness1", Math.round(avgBrightness1 * 100.0) / 100.0);
        stats.put("avgBrightness2", Math.round(avgBrightness2 * 100.0) / 100.0);
        stats.put("brightnessDiff", Math.round((avgBrightness1 - avgBrightness2) * 100.0) / 100.0);

        double avgSaturation1 = colors1.stream()
                .mapToDouble(c -> getSaturation((int[]) c.get("rgb")))
                .average().orElse(0);
        double avgSaturation2 = colors2.stream()
                .mapToDouble(c -> getSaturation((int[]) c.get("rgb")))
                .average().orElse(0);
        stats.put("avgSaturation1", Math.round(avgSaturation1 * 100.0) / 100.0);
        stats.put("avgSaturation2", Math.round(avgSaturation2 * 100.0) / 100.0);

        int warm1 = (int) colors1.stream().filter(c -> isWarmColor((int[]) c.get("rgb"))).count();
        int warm2 = (int) colors2.stream().filter(c -> isWarmColor((int[]) c.get("rgb"))).count();
        stats.put("warmColorRatio1", Math.round(warm1 * 10000.0 / colors1.size()) / 100.0 + "%");
        stats.put("warmColorRatio2", Math.round(warm2 * 10000.0 / colors2.size()) / 100.0 + "%");

        return stats;
    }

    private double[] calculateGamut(List<Map<String, Object>> colors) {
        double minL = 100, maxL = 0;
        double minA = 128, maxA = -128;
        double minB = 128, maxB = -128;

        for (Map<String, Object> c : colors) {
            double[] lab = rgbToLab((int[]) c.get("rgb"));
            minL = Math.min(minL, lab[0]);
            maxL = Math.max(maxL, lab[0]);
            minA = Math.min(minA, lab[1]);
            maxA = Math.max(maxA, lab[1]);
            minB = Math.min(minB, lab[2]);
            maxB = Math.max(maxB, lab[2]);
        }

        return new double[]{minL, maxL, minA, maxA, minB, maxB};
    }

    public Map<String, Object> findTraditionalEquivalent(int r, int g, int b) {
        List<ColorPalette> traditional = paletteRepository.findByPaletteType("traditional");

        Map<String, Object> bestMatch = null;
        double bestDist = Double.MAX_VALUE;
        ColorPalette bestPalette = null;

        for (ColorPalette palette : traditional) {
            List<Map<String, Object>> colors = parseColors(palette.getColors());
            for (Map<String, Object> color : colors) {
                double dist = colorDistance(new int[]{r, g, b}, (int[]) color.get("rgb"));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestMatch = color;
                    bestPalette = palette;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputRgb", new int[]{r, g, b});
        result.put("inputHex", rgbToHex(r, g, b));

        if (bestMatch != null) {
            result.put("matchedColor", bestMatch);
            result.put("matchedPalette", Map.of("id", bestPalette.getId(),
                    "name", bestPalette.getName(), "dynasty", bestPalette.getDynasty()));
            result.put("distance", Math.round(bestDist * 100.0) / 100.0);
            result.put("similarity", Math.round((1 - bestDist / 441.0) * 10000.0) / 100.0);
        } else {
            result.put("matchedColor", null);
        }

        return result;
    }

    public List<Map<String, Object>> getAllTraditionalColorNames() {
        List<ColorPalette> traditional = paletteRepository.findByPaletteType("traditional");
        List<Map<String, Object>> allColors = new ArrayList<>();

        for (ColorPalette palette : traditional) {
            List<Map<String, Object>> colors = parseColors(palette.getColors());
            for (Map<String, Object> color : colors) {
                color.put("palette", palette.getName());
                color.put("dynasty", palette.getDynasty());
                allColors.add(color);
            }
        }

        return allColors;
    }

    private List<Map<String, Object>> parseColors(String colorsStr) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (colorsStr == null || colorsStr.trim().isEmpty()) {
            return result;
        }

        String[] lines = colorsStr.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String[] parts = trimmed.split("[,，\\t]+");
            if (parts.length >= 3) {
                try {
                    Map<String, Object> color = new LinkedHashMap<>();
                    String name = parts[0].trim();
                    String hex = parts[1].trim();
                    if (!hex.startsWith("#")) {
                        hex = "#" + hex;
                    }
                    int[] rgb = hexToRgb(hex);

                    color.put("name", name);
                    color.put("hex", hex);
                    color.put("rgb", rgb);
                    color.put("lab", rgbToLab(rgb));

                    if (parts.length >= 4) {
                        color.put("category", parts[3].trim());
                    }
                    if (parts.length >= 5) {
                        color.put("description", parts[4].trim());
                    }

                    result.add(color);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private static final double[] D65_WHITE_POINT = {95.047, 100.000, 108.883};

    private static final double EPSILON = 216.0 / 24389.0;
    private static final double KAPPA = 24389.0 / 27.0;

    private static final double[][] SRGB_TO_XYZ_MATRIX = {
            {0.4124564, 0.3575761, 0.1804375},
            {0.2126729, 0.7151522, 0.0721750},
            {0.0193339, 0.1191920, 0.9503041}
    };

    private static final double GAMMA_THRESHOLD = 0.04045;
    private static final double GAMMA_SLOPE = 1.0 / 12.92;
    private static final double GAMMA_OFFSET = 0.055;
    private static final double GAMMA_EXPONENT = 2.4;

    private double colorDistance(int[] rgb1, int[] rgb2) {
        double[] lab1 = rgbToLab(rgb1);
        double[] lab2 = rgbToLab(rgb2);
        double dL = lab1[0] - lab2[0];
        double dA = lab1[1] - lab2[1];
        double dB = lab1[2] - lab2[2];
        return Math.sqrt(dL * dL + dA * dA + dB * dB);
    }

    private double[] rgbToLab(int[] rgb) {
        double rLinear = correctGamma(rgb[0] / 255.0);
        double gLinear = correctGamma(rgb[1] / 255.0);
        double bLinear = correctGamma(rgb[2] / 255.0);

        double x = rLinear * SRGB_TO_XYZ_MATRIX[0][0] +
                  gLinear * SRGB_TO_XYZ_MATRIX[0][1] +
                  bLinear * SRGB_TO_XYZ_MATRIX[0][2];
        double y = rLinear * SRGB_TO_XYZ_MATRIX[1][0] +
                  gLinear * SRGB_TO_XYZ_MATRIX[1][1] +
                  bLinear * SRGB_TO_XYZ_MATRIX[1][2];
        double z = rLinear * SRGB_TO_XYZ_MATRIX[2][0] +
                  gLinear * SRGB_TO_XYZ_MATRIX[2][1] +
                  bLinear * SRGB_TO_XYZ_MATRIX[2][2];

        double xn = x / D65_WHITE_POINT[0] * 100.0;
        double yn = y / D65_WHITE_POINT[1] * 100.0;
        double zn = z / D65_WHITE_POINT[2] * 100.0;

        double fx = applyLabTransform(xn / 100.0);
        double fy = applyLabTransform(yn / 100.0);
        double fz = applyLabTransform(zn / 100.0);

        double L = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double b = 200.0 * (fy - fz);

        L = Math.max(0.0, Math.min(100.0, L));
        a = Math.max(-128.0, Math.min(128.0, a));
        b = Math.max(-128.0, Math.min(128.0, b));

        return new double[]{Math.round(L * 10000.0) / 10000.0,
                           Math.round(a * 10000.0) / 10000.0,
                           Math.round(b * 10000.0) / 10000.0};
    }

    private double correctGamma(double value) {
        if (value <= GAMMA_THRESHOLD) {
            return value * GAMMA_SLOPE;
        } else {
            return Math.pow((value + GAMMA_OFFSET) / (1.0 + GAMMA_OFFSET), GAMMA_EXPONENT);
        }
    }

    private double applyLabTransform(double t) {
        if (t > EPSILON) {
            return Math.cbrt(t);
        } else {
            return (KAPPA * t + 16.0) / 116.0;
        }
    }

    public double[] labToRgb(double[] lab) {
        double fy = (lab[0] + 16.0) / 116.0;
        double fx = lab[1] / 500.0 + fy;
        double fz = fy - lab[2] / 200.0;

        double xn = inverseLabTransform(fx) * D65_WHITE_POINT[0];
        double yn = inverseLabTransform(fy) * D65_WHITE_POINT[1];
        double zn = inverseLabTransform(fz) * D65_WHITE_POINT[2];

        double x = xn / 100.0;
        double y = yn / 100.0;
        double z = zn / 100.0;

        double rLinear =  3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
        double gLinear = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
        double bLinear =  0.0556434 * x - 0.2040259 * y + 1.0572252 * z;

        int r = (int) Math.round(255.0 * inverseGamma(rLinear));
        int g = (int) Math.round(255.0 * inverseGamma(gLinear));
        int b = (int) Math.round(255.0 * inverseGamma(bLinear));

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new double[]{r, g, b};
    }

    private double inverseGamma(double value) {
        if (value <= 0.0031308) {
            return 12.92 * value;
        } else {
            return (1.0 + GAMMA_OFFSET) * Math.pow(value, 1.0 / GAMMA_EXPONENT) - GAMMA_OFFSET;
        }
    }

    private double inverseLabTransform(double t) {
        double t3 = t * t * t;
        if (t3 > EPSILON) {
            return t3;
        } else {
            return (116.0 * t - 16.0) / KAPPA;
        }
    }

    private int[] hexToRgb(String hex) {
        String h = hex.replace("#", "");
        if (h.length() == 3) {
            h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        }
        return new int[]{
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    private String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private double getBrightness(int[] rgb) {
        return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
    }

    private double getSaturation(int[] rgb) {
        double max = Math.max(Math.max(rgb[0], rgb[1]), rgb[2]);
        double min = Math.min(Math.min(rgb[0], rgb[1]), rgb[2]);
        if (max == 0) return 0;
        return (max - min) / max * 100;
    }

    private boolean isWarmColor(int[] rgb) {
        double hue = getHue(rgb);
        return hue >= 0 && hue < 60 || hue >= 300;
    }

    private double getHue(int[] rgb) {
        double r = rgb[0] / 255.0;
        double g = rgb[1] / 255.0;
        double b = rgb[2] / 255.0;

        double max = Math.max(Math.max(r, g), b);
        double min = Math.min(Math.min(r, g), b);
        double delta = max - min;

        if (delta == 0) return 0;

        double hue = 0;
        if (max == r) {
            hue = 60 * (((g - b) / delta) % 6);
        } else if (max == g) {
            hue = 60 * ((b - r) / delta + 2);
        } else {
            hue = 60 * ((r - g) / delta + 4);
        }

        if (hue < 0) hue += 360;
        return hue;
    }

    public Map<String, Object> generateDigitalPrintingComparison(Long traditionalPaletteId) {
        ColorPalette traditional = paletteRepository.findById(traditionalPaletteId)
                .orElseThrow(() -> new IllegalArgumentException("传统色卡不存在"));

        List<Map<String, Object>> tradColors = parseColors(traditional.getColors());

        List<ColorPalette> digitalPalettes = paletteRepository.findByPaletteType("digital_printing");
        ColorPalette digital = digitalPalettes.isEmpty() ? null : digitalPalettes.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("traditionalPalette", Map.of(
                "id", traditional.getId(),
                "name", traditional.getName(),
                "colorCount", traditional.getColorCount()));

        if (digital != null) {
            result.put("digitalPalette", Map.of(
                    "id", digital.getId(),
                    "name", digital.getName(),
                    "colorCount", digital.getColorCount()));

            List<Map<String, Object>> digitalColors = parseColors(digital.getColors());
            result.put("digitalColorCount", digitalColors.size());
            result.put("digitalGamutCoverage", calculateGamutCoverage(tradColors, digitalColors));
        } else {
            result.put("digitalPalette", null);
            result.put("digitalColorCount", "1677万 (24-bit RGB)");
            result.put("digitalGamutCoverage", "理论覆盖全部sRGB色域");
        }

        result.put("traditionalColorCount", tradColors.size());

        List<Map<String, Object>> analysisPoints = new ArrayList<>();
        analysisPoints.add(Map.of(
                "title", "色数对比",
                "traditional", tradColors.size() + " 色",
                "digital", "16,777,216 色",
                "note", "古代云锦受天然染料和工艺限制，色数有限但极具文化内涵"));
        analysisPoints.add(Map.of(
                "title", "色彩来源",
                "traditional", "天然染料（植物/矿物/动物）",
                "digital", "CMYK/数码喷墨/热升华",
                "note", "传统色彩取材自然，具有独特的温润质感"));
        analysisPoints.add(Map.of(
                "title", "色牢度",
                "traditional", "中等（天然染料易褪色）",
                "digital", "高（合成染料耐光耐洗）",
                "note", "古丝绸文物的色彩老化是重要研究课题"));
        analysisPoints.add(Map.of(
                "title", "色彩层次",
                "traditional", "通过多层套色实现晕染",
                "digital", "连续渐变，256级灰阶",
                "note", "云锦的'妆花'工艺可实现丰富的色彩过渡"));

        result.put("comparisonPoints", analysisPoints);

        List<Map<String, Object>> sampleMappings = new ArrayList<>();
        for (int i = 0; i < Math.min(8, tradColors.size()); i++) {
            Map<String, Object> tc = tradColors.get(i);
            int[] rgb = (int[]) tc.get("rgb");
            Map<String, Object> mapping = new LinkedHashMap<>();
            mapping.put("name", tc.get("name"));
            mapping.put("hex", tc.get("hex"));
            mapping.put("rgb", rgb);
            mapping.put("digitalEquivalent", tc.get("hex"));
            mapping.put("reproductionScore", calculateReproductionScore(rgb));
            sampleMappings.add(mapping);
        }
        result.put("sampleColorMappings", sampleMappings);

        return result;
    }

    private double calculateGamutCoverage(List<Map<String, Object>> tradColors,
                                           List<Map<String, Object>> digitalColors) {
        double[] tradGamut = calculateGamut(tradColors);
        double[] digGamut = calculateGamut(digitalColors);

        double tradVolume = (tradGamut[1] - tradGamut[0]) *
                            (tradGamut[3] - tradGamut[2]) *
                            (tradGamut[5] - tradGamut[4]);
        double digVolume = (digGamut[1] - digGamut[0]) *
                           (digGamut[3] - digGamut[2]) *
                           (digGamut[5] - digGamut[4]);

        return Math.round((tradVolume / digVolume) * 10000.0) / 100.0;
    }

    private int calculateReproductionScore(int[] rgb) {
        double saturation = getSaturation(rgb);
        double brightness = getBrightness(rgb);

        int score = 80;
        if (saturation > 60) score -= 15;
        if (brightness < 30 || brightness > 95) score -= 10;

        return Math.max(50, Math.min(100, score));
    }
}
