package com.yunjin.system.color_analysis;

import com.yunjin.system.entity.ColorPalette;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/colors")
@CrossOrigin(origins = "*")
public class ColorAnalysisController {

    private final ColorAnalysisService colorService;

    public ColorAnalysisController(ColorAnalysisService colorService) {
        this.colorService = colorService;
    }

    @GetMapping("/palettes")
    public ResponseEntity<List<ColorPalette>> getPalettes(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(colorService.getAllPalettes(keyword));
    }

    @GetMapping("/palettes/{id}")
    public ResponseEntity<ColorPalette> getPaletteById(@PathVariable Long id) {
        return colorService.getPaletteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/palettes/code/{code}")
    public ResponseEntity<ColorPalette> getPaletteByCode(@PathVariable String code) {
        return colorService.getPaletteByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/palettes/types")
    public ResponseEntity<List<String>> getPaletteTypes() {
        return ResponseEntity.ok(colorService.getPaletteTypes());
    }

    @GetMapping("/palettes/dynasties")
    public ResponseEntity<List<String>> getDynasties() {
        return ResponseEntity.ok(colorService.getDynasties());
    }

    @GetMapping("/palettes/type/{type}")
    public ResponseEntity<List<ColorPalette>> getPalettesByType(@PathVariable String type) {
        return ResponseEntity.ok(colorService.getPalettesByType(type));
    }

    @PostMapping("/palettes")
    public ResponseEntity<?> createPalette(@RequestBody ColorPalette palette) {
        try {
            ColorPalette created = colorService.createPalette(palette);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/palettes/{id}")
    public ResponseEntity<?> updatePalette(@PathVariable Long id,
                                            @RequestBody ColorPalette palette) {
        try {
            ColorPalette updated = colorService.updatePalette(id, palette);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/palettes/{id}")
    public ResponseEntity<?> deletePalette(@PathVariable Long id) {
        try {
            colorService.deletePalette(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> comparePalettes(
            @RequestParam Long paletteId1,
            @RequestParam Long paletteId2) {
        try {
            Map<String, Object> result = colorService.comparePalettes(paletteId1, paletteId2);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/match")
    public ResponseEntity<Map<String, Object>> findTraditionalMatch(
            @RequestParam int r,
            @RequestParam int g,
            @RequestParam int b) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(colorService.findTraditionalEquivalent(r, g, b));
    }

    @GetMapping("/traditional-names")
    public ResponseEntity<List<Map<String, Object>>> getTraditionalColorNames() {
        return ResponseEntity.ok(colorService.getAllTraditionalColorNames());
    }

    @GetMapping("/digital-comparison/{traditionalPaletteId}")
    public ResponseEntity<Map<String, Object>> getDigitalPrintingComparison(
            @PathVariable Long traditionalPaletteId) {
        try {
            Map<String, Object> result = colorService.generateDigitalPrintingComparison(
                    traditionalPaletteId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
