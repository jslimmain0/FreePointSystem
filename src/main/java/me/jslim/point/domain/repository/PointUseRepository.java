package me.jslim.point.domain.repository;

import me.jslim.point.domain.entity.PointUse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUseRepository extends JpaRepository<PointUse, String> {
    List<PointUse> findAllByPointKey(String pointKey);
}

