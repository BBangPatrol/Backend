package com.bbangpatrol.item.repository;

import com.bbangpatrol.item.entity.UserItem;
import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    Long countByUser(User user);

    List<UserItem> findTop8ByUserOrderByAcquiredAtDescIdDesc(User user);
}
