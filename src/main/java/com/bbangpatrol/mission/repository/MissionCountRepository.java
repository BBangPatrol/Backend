package com.bbangpatrol.mission.repository;

import com.bbangpatrol.common.enums.Region;
import com.bbangpatrol.item.entity.ItemRank;
import com.bbangpatrol.mission.entity.MissionProgress;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface MissionCountRepository extends Repository<MissionProgress, Long> {

    @Query("SELECT COUNT(vd) FROM VisitDetail vd WHERE vd.visit.user.id = :userId")
    long countReceipts(Long userId);

    // 지도에 실린 빵집의 구가 아니라 영수증을 실제로 끊은 구로 센다.
    // 안 그러면 중구 지점 영수증이 대덕구 미션을 채운다
    @Query("SELECT COUNT(vd) FROM VisitDetail vd " +
            "WHERE vd.visit.user.id = :userId AND vd.region = :region")
    long countReceiptsByRegion(Long userId, Region region);

    @Query("SELECT COUNT(DISTINCT vd.region) FROM VisitDetail vd " +
            "WHERE vd.visit.user.id = :userId " +
            "AND vd.region IS NOT NULL AND vd.region <> :excluded")
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
