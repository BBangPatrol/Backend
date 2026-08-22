package com.bbangpatrol.bakery.repository;

import com.bbangpatrol.bakery.entity.Bakery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BakeryRepository extends JpaRepository<Bakery, Long> {


    @Query("""
            select b.businessNumber
            from Bakery b
            where b.id = :storeId
            """)
    Optional<String> findBusinessNumberByStoreId(
            @Param("storeId") Long storeId
    );
}
