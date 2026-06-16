package com.yunjin.system.repository;

import com.yunjin.system.entity.YunjinVariety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YunjinVarietyRepository extends JpaRepository<YunjinVariety, Long> {

    Optional<YunjinVariety> findByCode(String code);

    List<YunjinVariety> findByDynasty(String dynasty);

    List<YunjinVariety> findByWeaveType(String weaveType);

    @Query("SELECT v FROM YunjinVariety v WHERE " +
           "LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<YunjinVariety> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT v FROM YunjinVariety v WHERE v.complexityScore >= :minComplexity")
    List<YunjinVariety> findByMinComplexity(@Param("minComplexity") Integer minComplexity);

    @Query("SELECT DISTINCT v.dynasty FROM YunjinVariety v WHERE v.dynasty IS NOT NULL ORDER BY v.dynasty")
    List<String> findAllDynasties();

    @Query("SELECT DISTINCT v.weaveType FROM YunjinVariety v WHERE v.weaveType IS NOT NULL")
    List<String> findAllWeaveTypes();
}
