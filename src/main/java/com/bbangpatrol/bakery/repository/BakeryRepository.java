package com.bbangpatrol.bakery.repository;

import com.bbangpatrol.bakery.entity.Bakery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BakeryRepository extends JpaRepository<Bakery, Long> {
}
