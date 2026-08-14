package com.bbangpatrol.item.repository;

import com.bbangpatrol.item.entity.Item;
import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    Long countByUser(User user);

    List<UserItem> findTop8ByUserOrderByAcquiredAtDescIdDesc(User user);

    Boolean existsByUserIdAndItemId(Long userId, Long itemId);

    @Query("SELECT ui.item FROM UserItem ui WHERE ui.user.id = :userId")
    List<Item> findItemsByUserId(@Param("userId") Long userId);
}
