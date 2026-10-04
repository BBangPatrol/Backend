package com.bbangpatrol.mission.repository;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.item.entity.ItemRank;
import com.bbangpatrol.mission.entity.MissionProgress;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface MissionCountRepository extends Repository<MissionProgress, Long> {

    @Query("SELECT COUNT(vd) FROM VisitDetail vd WHERE vd.visit.user.id = :userId")
    long countReceipts(Long userId);

    @Query("SELECT COUNT(vd) FROM VisitDetail vd " +
            "WHERE vd.visit.user.id = :userId AND vd.visit.bakery.region = :region")
    long countReceiptsByRegion(Long userId, Region region);

    @Query("SELECT COUNT(DISTINCT v.bakery.region) FROM Visit v " +
            "WHERE v.user.id = :userId " +
            "AND v.bakery.region IS NOT NULL AND v.bakery.region <> :excluded")
    long countDistinctRegions(Long userId, Region excluded);

    @Query("SELECT COUNT(DISTINCT v.bakery.id) FROM Visit v WHERE v.user.id = :userId")
    long countDistinctBakeries(Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId AND r.deletedAt IS NULL")
    long countReviews(Long userId);

    @Query("SELECT COUNT(ui) FROM UserItem ui WHERE ui.user.id = :userId")
    long countDistinctItems(Long userId);

    @Query("SELECT COUNT(ui) FROM UserItem ui WHERE ui.user.id = :userId AND ui.item.rank = :rank")
    long countDistinctItemsByRank(Long userId, ItemRank rank);
}
