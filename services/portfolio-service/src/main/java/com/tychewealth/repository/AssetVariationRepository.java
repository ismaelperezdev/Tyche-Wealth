package com.tychewealth.repository;

import com.tychewealth.entity.AssetVariationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetVariationRepository extends JpaRepository<AssetVariationEntity, Long> {}
