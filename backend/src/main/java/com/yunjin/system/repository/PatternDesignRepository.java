package com.yunjin.system.repository;

import com.yunjin.system.entity.PatternDesign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatternDesignRepository extends JpaRepository<PatternDesign, Long> {

    Optional<PatternDesign> findByPatternCode(String patternCode);

    List<PatternDesign> findByCategory(String category);

    List<PatternDesign> findByDynasty(String dynasty);

    List<PatternDesign> findByWeaveStructure(String weaveStructure);

    List<PatternDesign> findByVarietyId(Long varietyId);

    @Query("SELECT p FROM PatternDesign p WHERE p.isPublic = true ORDER BY p.useCount DESC")
    List<PatternDesign> findPopularPatterns(Pageable pageable);

    @Query("SELECT p FROM PatternDesign p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND p.isPublic = true")
    Page<PatternDesign> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM PatternDesign p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:dynasty IS NULL OR p.dynasty = :dynasty) AND " +
           "(:weaveStructure IS NULL OR p.weaveStructure = :weaveStructure) AND " +
           "(:minComplexity IS NULL OR p.complexityLevel >= :minComplexity) AND " +
           "(:maxComplexity IS NULL OR p.complexityLevel <= :maxComplexity) AND " +
           "p.isPublic = true")
    Page<PatternDesign> advancedSearch(
            @Param("category") String category,
            @Param("dynasty") String dynasty,
            @Param("weaveStructure") String weaveStructure,
            @Param("minComplexity") Integer minComplexity,
            @Param("maxComplexity") Integer maxComplexity,
            Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM PatternDesign p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT p.dynasty FROM PatternDesign p WHERE p.dynasty IS NOT NULL ORDER BY p.dynasty")
    List<String> findAllDynasties();

    @Query("SELECT DISTINCT p.weaveStructure FROM PatternDesign p WHERE p.weaveStructure IS NOT NULL")
    List<String> findAllWeaveStructures();

    @Query("SELECT DISTINCT p.symmetryType FROM PatternDesign p WHERE p.symmetryType IS NOT NULL")
    List<String> findAllSymmetryTypes();
}
