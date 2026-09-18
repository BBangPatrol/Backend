package com.bbangpatrol.bakery.repository;

import com.bbangpatrol.bakery.entity.BakeryBusinessNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BakeryBusinessNumberRepository extends JpaRepository<BakeryBusinessNumber, Long> {

    List<BakeryBusinessNumber> findAllByBakeryId(Long bakeryId);
}
