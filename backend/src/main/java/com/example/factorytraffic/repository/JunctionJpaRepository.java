package com.example.factorytraffic.repository;

import com.example.factorytraffic.entity.JunctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JunctionJpaRepository extends JpaRepository<JunctionEntity, String> {
}
