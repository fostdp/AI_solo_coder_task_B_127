package com.yunjin.system.repository;

import com.yunjin.system.entity.ColorPalette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorPaletteRepository extends JpaRepository<ColorPalette, Long> {

    Optional<ColorPalette> findByCode(String code);

    List<ColorPalette> findByPaletteType(String paletteType);

    List<ColorPalette> findByDynasty(String dynasty);

    List<ColorPalette> findByVarietyId(Long varietyId);

    @Query("SELECT c FROM ColorPalette c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND c.isPublic = true")
    List<ColorPalette> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT c.paletteType FROM ColorPalette c WHERE c.paletteType IS NOT NULL")
    List<String> findAllPaletteTypes();

    @Query("SELECT DISTINCT c.dynasty FROM ColorPalette c WHERE c.dynasty IS NOT NULL ORDER BY c.dynasty")
    List<String> findAllDynasties();

    @Query("SELECT DISTINCT c.source FROM ColorPalette c WHERE c.source IS NOT NULL")
    List<String> findAllSources();
}
