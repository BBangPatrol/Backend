package com.bbangpatrol.item.repository;

import com.bbangpatrol.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
