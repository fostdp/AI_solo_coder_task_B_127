package com.yunjin.system.repository;

import com.yunjin.system.entity.UserWeavingDesign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWeavingDesignRepository extends JpaRepository<UserWeavingDesign, Long> {

    List<UserWeavingDesign> findByDesigner(String designer);

    List<UserWeavingDesign> findByBaseVarietyId(Long baseVarietyId);

    Page<UserWeavingDesign> findByIsPublicTrue(Pageable pageable);

    @Query("SELECT d FROM UserWeavingDesign d WHERE d.isPublic = true AND " +
           "(LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<UserWeavingDesign> searchPublicDesigns(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM UserWeavingDesign d WHERE d.isPublic = true " +
           "ORDER BY d.likeCount DESC")
    List<UserWeavingDesign> findPopularDesigns(Pageable pageable);

    @Query("SELECT d FROM UserWeavingDesign d WHERE d.isPublic = true " +
           "ORDER BY d.createdAt DESC")
    List<UserWeavingDesign> findRecentDesigns(Pageable pageable);

    long countByDesigner(String designer);
}
