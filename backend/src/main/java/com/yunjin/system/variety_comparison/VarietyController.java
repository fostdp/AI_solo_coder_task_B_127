package com.yunjin.system.variety_comparison;

import com.yunjin.system.entity.YunjinVariety;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/varieties")
@CrossOrigin(origins = "*")
public class VarietyController {

    private final VarietyComparisonService varietyService;

    public VarietyController(VarietyComparisonService varietyService) {
        this.varietyService = varietyService;
    }

    @GetMapping
    public ResponseEntity<List<YunjinVariety>> getAllVarieties(
            @RequestParam(required = false) String keyword) {
        List<YunjinVariety> varieties = varietyService.searchVarieties(keyword);
        return ResponseEntity.ok(varieties);
    }

    @GetMapping("/{id}")
    public ResponseEntity<YunjinVariety> getVarietyById(@PathVariable Long id) {
        return varietyService.getVarietyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<YunjinVariety> getVarietyByCode(@PathVariable String code) {
        return varietyService.getVarietyByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dynasties")
    public ResponseEntity<List<String>> getAllDynasties() {
        return ResponseEntity.ok(varietyService.getAllDynasties());
    }

    @GetMapping("/weave-types")
    public ResponseEntity<List<String>> getAllWeaveTypes() {
        return ResponseEntity.ok(varietyService.getAllWeaveTypes());
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareVarieties(
            @RequestBody List<Long> varietyIds) {
        try {
            Map<String, Object> result = varietyService.compareVarieties(varietyIds);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/compare/radar")
    public ResponseEntity<List<Map<String, Object>>> getRadarData(
            @RequestBody List<Long> varietyIds) {
        try {
            List<Map<String, Object>> radar = varietyService.getVarietyComparisonRadar(varietyIds);
            return ResponseEntity.ok(radar);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createVariety(@RequestBody YunjinVariety variety) {
        try {
            YunjinVariety created = varietyService.createVariety(variety);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVariety(@PathVariable Long id,
                                            @RequestBody YunjinVariety variety) {
        try {
            YunjinVariety updated = varietyService.updateVariety(id, variety);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVariety(@PathVariable Long id) {
        try {
            varietyService.deleteVariety(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
