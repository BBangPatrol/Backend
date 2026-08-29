package com.bbangpatrol.bookmark.repository;

import com.bbangpatrol.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndBakeryId(Long userId, Long bakeryId);

    boolean existsByUserIdAndBakeryId(Long userId, Long bakeryId);

    @Query("""
            select b.bakery.id
            from Bookmark b
            where b.user.id = :userId
            and b.bakery.id in :bakeryIds
            """)
    List<Long> findBakeryIdsByUserIdAndBakeryIds(
            @Param("userId") Long userId,
            @Param("bakeryIds") List<Long> bakeryIds
    );
}
