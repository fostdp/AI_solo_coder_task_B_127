package com.yunjin.system.pattern_library;

import com.yunjin.system.entity.PatternDesign;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patterns")
@CrossOrigin(origins = "*")
public class PatternLibraryController {

    private final PatternLibraryService patternService;

    public PatternLibraryController(PatternLibraryService patternService) {
        this.patternService = patternService;
    }

    @GetMapping
    public ResponseEntity<Page<PatternDesign>> searchPatterns(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(patternService.searchPatterns(keyword, page, size, sortBy));
    }

    @GetMapping("/advanced")
    public ResponseEntity<Page<PatternDesign>> advancedSearch(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String weaveStructure,
            @RequestParam(required = false) Integer minComplexity,
            @RequestParam(required = false) Integer maxComplexity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(patternService.advancedSearch(
                category, dynasty, weaveStructure,
                minComplexity, maxComplexity, page, size, sortBy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatternDesign> getPatternById(@PathVariable Long id) {
        return patternService.getPatternById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PatternDesign> getPatternByCode(@PathVariable String code) {
        return patternService.getPatternByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(patternService.getAllCategories());
    }

    @GetMapping("/dynasties")
    public ResponseEntity<List<String>> getDynasties() {
        return ResponseEntity.ok(patternService.getAllDynasties());
    }

    @GetMapping("/weave-structures")
    public ResponseEntity<List<String>> getWeaveStructures() {
        return ResponseEntity.ok(patternService.getAllWeaveStructures());
    }

    @GetMapping("/symmetry-types")
    public ResponseEntity<List<String>> getSymmetryTypes() {
        return ResponseEntity.ok(patternService.getAllSymmetryTypes());
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PatternDesign>> getPopularPatterns(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(patternService.getPopularPatterns(limit));
    }

    @GetMapping("/variety/{varietyId}")
    public ResponseEntity<List<PatternDesign>> getPatternsByVariety(
            @PathVariable Long varietyId) {
        return ResponseEntity.ok(patternService.getPatternsByVariety(varietyId));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<PatternDesign>> getSimilarPatterns(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            return ResponseEntity.ok(patternService.findSimilarPatterns(id, limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/similar-scored")
    public ResponseEntity<Map<String, Object>> getSimilarPatternsWithScores(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int limit) {
        try {
            return ResponseEntity.ok(patternService.findSimilarPatternsWithScores(id, limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/matrix")
    public ResponseEntity<int[][]> getPatternMatrix(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(patternService.getPatternMatrixAsArray(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(patternService.getPatternStatistics());
    }

    @PostMapping
    public ResponseEntity<?> createPattern(@RequestBody PatternDesign pattern) {
        try {
            PatternDesign created = patternService.createPattern(pattern);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePattern(@PathVariable Long id,
                                            @RequestBody PatternDesign pattern) {
        try {
            PatternDesign updated = patternService.updatePattern(id, pattern);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePattern(@PathVariable Long id) {
        try {
            patternService.deletePattern(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<Void> incrementUseCount(@PathVariable Long id) {
        patternService.incrementUseCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> incrementLikeCount(@PathVariable Long id) {
        patternService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }
}
