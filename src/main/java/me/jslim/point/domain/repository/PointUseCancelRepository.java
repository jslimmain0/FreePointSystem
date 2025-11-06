package me.jslim.point.domain.repository;

import me.jslim.point.domain.entity.PointUseCancel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PointUseCancelRepository extends JpaRepository<PointUseCancel, String> {
    @Query(value = """
      select e.*
        from POINT_USE_CANCEL e
       inner join POINT_USE u on u.point_key = :pointKey and u.USE_KEY = e.USE_KEY
      """, nativeQuery = true)
    List<PointUseCancel> findAllByUsePointKey(String pointKey);
}

