package com.yunjin.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yunjin_variety")
public class YunjinVariety {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String alias;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String dynasty;

    @Column(length = 50)
    private String weaveType;

    @Column
    private Integer warpCount;

    @Column
    private Integer weftCount;

    @Column
    private Double warpDensityPerCm;

    @Column
    private Double weftDensityPerCm;

    @Column
    private Integer colorCount;

    @Column(length = 50)
    private String patternRepeatUnit;

    @Column
    private Integer patternRepeatWarp;

    @Column
    private Integer patternRepeatWeft;

    @Column(length = 50)
    private String sheddingMechanism;

    @Column
    private Integer harnessCount;

    @Column
    private Double productionSpeedCmPerDay;

    @Column(length = 50)
    private String rawMaterial;

    @Column
    private Double silkWeightPerSqmGram;

    @Column(length = 2000)
    private String characteristics;

    @Column(length = 2000)
    private String historicalUsage;

    @Column(length = 2000)
    private String culturalSignificance;

    @Column(columnDefinition = "TEXT")
    private String paletteColors;

    @Column
    private Integer complexityScore;

    @Column
    private Double rarityScore;

    @Column(length = 500)
    private String representativeWorks;

    @Column(length = 500)
    private String imageUrl;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDynasty() {
        return dynasty;
    }

    public void setDynasty(String dynasty) {
        this.dynasty = dynasty;
    }

    public String getWeaveType() {
        return weaveType;
    }

    public void setWeaveType(String weaveType) {
        this.weaveType = weaveType;
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

    public Double getWarpDensityPerCm() {
        return warpDensityPerCm;
    }

    public void setWarpDensityPerCm(Double warpDensityPerCm) {
        this.warpDensityPerCm = warpDensityPerCm;
    }

    public Double getWeftDensityPerCm() {
        return weftDensityPerCm;
    }

    public void setWeftDensityPerCm(Double weftDensityPerCm) {
        this.weftDensityPerCm = weftDensityPerCm;
    }

    public Integer getColorCount() {
        return colorCount;
    }

    public void setColorCount(Integer colorCount) {
        this.colorCount = colorCount;
    }

    public String getPatternRepeatUnit() {
        return patternRepeatUnit;
    }

    public void setPatternRepeatUnit(String patternRepeatUnit) {
        this.patternRepeatUnit = patternRepeatUnit;
    }

    public Integer getPatternRepeatWarp() {
        return patternRepeatWarp;
    }

    public void setPatternRepeatWarp(Integer patternRepeatWarp) {
        this.patternRepeatWarp = patternRepeatWarp;
    }

    public Integer getPatternRepeatWeft() {
        return patternRepeatWeft;
    }

    public void setPatternRepeatWeft(Integer patternRepeatWeft) {
        this.patternRepeatWeft = patternRepeatWeft;
    }

    public String getSheddingMechanism() {
        return sheddingMechanism;
    }

    public void setSheddingMechanism(String sheddingMechanism) {
        this.sheddingMechanism = sheddingMechanism;
    }

    public Integer getHarnessCount() {
        return harnessCount;
    }

    public void setHarnessCount(Integer harnessCount) {
        this.harnessCount = harnessCount;
    }

    public Double getProductionSpeedCmPerDay() {
        return productionSpeedCmPerDay;
    }

    public void setProductionSpeedCmPerDay(Double productionSpeedCmPerDay) {
        this.productionSpeedCmPerDay = productionSpeedCmPerDay;
    }

    public String getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(String rawMaterial) {
        this.rawMaterial = rawMaterial;
    }

    public Double getSilkWeightPerSqmGram() {
        return silkWeightPerSqmGram;
    }

    public void setSilkWeightPerSqmGram(Double silkWeightPerSqmGram) {
        this.silkWeightPerSqmGram = silkWeightPerSqmGram;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getHistoricalUsage() {
        return historicalUsage;
    }

    public void setHistoricalUsage(String historicalUsage) {
        this.historicalUsage = historicalUsage;
    }

    public String getCulturalSignificance() {
        return culturalSignificance;
    }

    public void setCulturalSignificance(String culturalSignificance) {
        this.culturalSignificance = culturalSignificance;
    }

    public String getPaletteColors() {
        return paletteColors;
    }

    public void setPaletteColors(String paletteColors) {
        this.paletteColors = paletteColors;
    }

    public Integer getComplexityScore() {
        return complexityScore;
    }

    public void setComplexityScore(Integer complexityScore) {
        this.complexityScore = complexityScore;
    }

    public Double getRarityScore() {
        return rarityScore;
    }

    public void setRarityScore(Double rarityScore) {
        this.rarityScore = rarityScore;
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
