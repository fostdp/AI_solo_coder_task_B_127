package com.yunjin.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pattern_design")
public class PatternDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String alias;

    @Column(length = 50)
    private String patternCode;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subCategory;

    @Column(length = 100)
    private String dynasty;

    @Column(length = 100)
    private String origin;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String culturalMeaning;

    @Column(length = 100)
    private String weaveStructure;

    @Column
    private Integer warpRepeat;

    @Column
    private Integer weftRepeat;

    @Column
    private Integer colorCount;

    @Column(length = 500)
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String patternMatrix;

    @Column(columnDefinition = "TEXT")
    private String colorPalette;

    @Column
    private Integer complexityLevel;

    @Column
    private Integer symmetryScore;

    @Column(length = 50)
    private String symmetryType;

    @Column(length = 500)
    private String representativeWorks;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column
    private Long varietyId;

    @Column(length = 100)
    private String creator;

    @Column
    private Boolean isPublic;

    @Column
    private Integer useCount;

    @Column
    private Integer likeCount;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublic == null) isPublic = true;
        if (useCount == null) useCount = 0;
        if (likeCount == null) likeCount = 0;
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getPatternCode() {
        return patternCode;
    }

    public void setPatternCode(String patternCode) {
        this.patternCode = patternCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getDynasty() {
        return dynasty;
    }

    public void setDynasty(String dynasty) {
        this.dynasty = dynasty;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCulturalMeaning() {
        return culturalMeaning;
    }

    public void setCulturalMeaning(String culturalMeaning) {
        this.culturalMeaning = culturalMeaning;
    }

    public String getWeaveStructure() {
        return weaveStructure;
    }

    public void setWeaveStructure(String weaveStructure) {
        this.weaveStructure = weaveStructure;
    }

    public Integer getWarpRepeat() {
        return warpRepeat;
    }

    public void setWarpRepeat(Integer warpRepeat) {
        this.warpRepeat = warpRepeat;
    }

    public Integer getWeftRepeat() {
        return weftRepeat;
    }

    public void setWeftRepeat(Integer weftRepeat) {
        this.weftRepeat = weftRepeat;
    }

    public Integer getColorCount() {
        return colorCount;
    }

    public void setColorCount(Integer colorCount) {
        this.colorCount = colorCount;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public Integer getComplexityLevel() {
        return complexityLevel;
    }

    public void setComplexityLevel(Integer complexityLevel) {
        this.complexityLevel = complexityLevel;
    }

    public Integer getSymmetryScore() {
        return symmetryScore;
    }

    public void setSymmetryScore(Integer symmetryScore) {
        this.symmetryScore = symmetryScore;
    }

    public String getSymmetryType() {
        return symmetryType;
    }

    public void setSymmetryType(String symmetryType) {
        this.symmetryType = symmetryType;
    }

    public String getRepresentativeWorks() {
        return representativeWorks;
    }

    public void setRepresentativeWorks(String representativeWorks) {
        this.representativeWorks = representativeWorks;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getVarietyId() {
        return varietyId;
    }

    public void setVarietyId(Long varietyId) {
        this.varietyId = varietyId;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
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
