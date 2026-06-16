package com.yunjin.system.virtual_weaving;

import com.yunjin.system.entity.UserWeavingDesign;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/virtual-weaving")
@CrossOrigin(origins = "*")
public class VirtualWeavingController {

    private final VirtualWeavingService weavingService;

    public VirtualWeavingController(VirtualWeavingService weavingService) {
        this.weavingService = weavingService;
    }

    @PostMapping("/create/from-variety")
    public ResponseEntity<?> createFromVariety(
            @RequestParam Long varietyId,
            @RequestParam(required = false) String designer) {
        try {
            UserWeavingDesign design = weavingService.createDesignFromVariety(varietyId, designer);
            return ResponseEntity.ok(design);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create/from-pattern")
    public ResponseEntity<?> createFromPattern(
            @RequestParam Long patternId,
            @RequestParam(required = false) String designer) {
        try {
            UserWeavingDesign design = weavingService.createDesignFromPattern(patternId, designer);
            return ResponseEntity.ok(design);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create/blank")
    public ResponseEntity<?> createBlank(
            @RequestParam(defaultValue = "120") int warpCount,
            @RequestParam(defaultValue = "200") int weftCount,
            @RequestParam(required = false) String designer) {
        try {
            UserWeavingDesign design = weavingService.createBlankDesign(designer, warpCount, weftCount);
            return ResponseEntity.ok(design);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/designs/{id}")
    public ResponseEntity<UserWeavingDesign> getDesignById(@PathVariable Long id) {
        weavingService.incrementViewCount(id);
        return weavingService.getDesignById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/designs")
    public ResponseEntity<Page<UserWeavingDesign>> getPublicDesigns(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ResponseEntity.ok(weavingService.getPublicDesigns(keyword, page, size, sortBy));
    }

    @GetMapping("/designs/popular")
    public ResponseEntity<List<UserWeavingDesign>> getPopularDesigns(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(weavingService.getPopularDesigns(limit));
    }

    @GetMapping("/designs/recent")
    public ResponseEntity<List<UserWeavingDesign>> getRecentDesigns(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(weavingService.getRecentDesigns(limit));
    }

    @GetMapping("/designs/designer/{designer}")
    public ResponseEntity<List<UserWeavingDesign>> getDesignsByDesigner(
            @PathVariable String designer) {
        return ResponseEntity.ok(weavingService.getDesignsByDesigner(designer));
    }

    @PutMapping("/designs/{id}")
    public ResponseEntity<?> updateDesign(@PathVariable Long id,
                                           @RequestBody UserWeavingDesign design) {
        try {
            UserWeavingDesign updated = weavingService.updateDesign(id, design);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/designs/{id}")
    public ResponseEntity<?> deleteDesign(@PathVariable Long id) {
        try {
            weavingService.deleteDesign(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/designs/{id}/publish")
    public ResponseEntity<?> publishDesign(@PathVariable Long id) {
        try {
            UserWeavingDesign design = weavingService.publishDesign(id);
            return ResponseEntity.ok(design);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/designs/{id}/unpublish")
    public ResponseEntity<?> unpublishDesign(@PathVariable Long id) {
        try {
            UserWeavingDesign design = weavingService.unpublishDesign(id);
            return ResponseEntity.ok(design);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/designs/{id}/like")
    public ResponseEntity<Void> likeDesign(@PathVariable Long id) {
        weavingService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulate/{designId}")
    public ResponseEntity<?> simulateWeaving(
            @PathVariable Long designId,
            @RequestParam(defaultValue = "10") int steps) {
        try {
            Map<String, Object> result = weavingService.simulateWeaving(designId, steps);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/designs/{id}/preview")
    public ResponseEntity<?> getPreview(@PathVariable Long id) {
        try {
            Map<String, Object> result = weavingService.generateWeavingPreview(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(weavingService.getDesignStatistics());
    }

    @GetMapping("/designs/{id}/webgl-texture")
    public ResponseEntity<?> getWebGLTexture(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int version) {
        try {
            Map<String, Object> result = weavingService.getWebGLTextureData(id, version);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/designs/{id}/simulate-batch")
    public ResponseEntity<?> simulateWeavingBatch(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int batchSize,
            @RequestParam(defaultValue = "100") int steps) {
        try {
            Map<String, Object> result = weavingService.simulateWeavingBatch(id, batchSize, steps);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/designs/{id}/matrix-cached")
    public ResponseEntity<int[][]> getCachedMatrix(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int version) {
        try {
            return ResponseEntity.ok(weavingService.getCachedMatrix(id, version));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/designs/{id}/cache")
    public ResponseEntity<Map<String, Object>> invalidateCache(@PathVariable Long id) {
        int removed = weavingService.invalidateCache(id);
        return ResponseEntity.ok(Map.of(
                "designId", id,
                "cacheEntriesInvalidated", removed,
                "success", true
        ));
    }

    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(weavingService.getCacheStats());
    }
}
