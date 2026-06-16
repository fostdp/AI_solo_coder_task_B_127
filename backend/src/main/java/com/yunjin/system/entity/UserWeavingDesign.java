package com.yunjin.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_weaving_design")
public class UserWeavingDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String designer;

    @Column
    private Long baseVarietyId;

    @Column(length = 100)
    private String baseVarietyName;

    @Column
    private Integer warpCount;

    @Column
    private Integer weftCount;

    @Column
    private Integer colorCount;

    @Column(columnDefinition = "TEXT")
    private String patternMatrix;

    @Column(columnDefinition = "TEXT")
    private String colorPalette;

    @Column(columnDefinition = "TEXT")
    private String warpTensionArray;

    @Column(columnDefinition = "TEXT")
    private String shedOpeningArray;

    @Column(columnDefinition = "TEXT")
    private String designLayers;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column
    private Long patternId;

    @Column
    private Boolean isPublic;

    @Column
    private Integer likeCount;

    @Column
    private Integer viewCount;

    @Column
    private Double complexityScore;

    @Column
    private Integer estimatedProductionHours;

    @Column(length = 1000)
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 50)
    private String status;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublic == null) isPublic = false;
        if (likeCount == null) likeCount = 0;
        if (viewCount == null) viewCount = 0;
        if (status == null) status = "draft";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDesigner() {
        return designer;
    }

    public void setDesigner(String designer) {
        this.designer = designer;
    }

    public Long getBaseVarietyId() {
        return baseVarietyId;
    }

    public void setBaseVarietyId(Long baseVarietyId) {
        this.baseVarietyId = baseVarietyId;
    }

    public String getBaseVarietyName() {
        return baseVarietyName;
    }

    public void setBaseVarietyName(String baseVarietyName) {
        this.baseVarietyName = baseVarietyName;
    }

    public Integer getWarpCount() {
        return warpCount;
    }

    public void setWarpCount(Integer warpCount) {
        this.warpCount = warpCount;
    }

    public Integer getWeftCount() {
        return weftCount;
    }

    public void setWeftCount(Integer weftCount) {
        this.weftCount = weftCount;
    }

    public Integer getColorCount() {
        return colorCount;
    }

    public void setColorCount(Integer colorCount) {
        this.colorCount = colorCount;
    }

    public String getPatternMatrix() {
        return patternMatrix;
    }

    public void setPatternMatrix(String patternMatrix) {
        this.patternMatrix = patternMatrix;
    }

    public String getColorPalette() {
        return colorPalette;
    }

    public void setColorPalette(String colorPalette) {
        this.colorPalette = colorPalette;
    }

    public String getWarpTensionArray() {
        return warpTensionArray;
    }

    public void setWarpTensionArray(String warpTensionArray) {
        this.warpTensionArray = warpTensionArray;
    }

    public String getShedOpeningArray() {
        return shedOpeningArray;
    }

    public void setShedOpeningArray(String shedOpeningArray) {
        this.shedOpeningArray = shedOpeningArray;
    }

    public String getDesignLayers() {
        return designLayers;
    }

    public void setDesignLayers(String designLayers) {
        this.designLayers = designLayers;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getPatternId() {
        return patternId;
    }

    public void setPatternId(Long patternId) {
        this.patternId = patternId;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Double getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(Double complexityScore) {
        this.complexityScore = complexityScore;
    }

    public Integer getEstimatedProductionHours() {
        return estimatedProductionHours;
    }

    public void setEstimatedProductionHours(Integer estimatedProductionHours) {
        this.estimatedProductionHours = estimatedProductionHours;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
