package me.jslim.point.domain.repository;

import me.jslim.point.domain.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, String> {
    Optional<Point> findByPointKey(String pointKey);
}

