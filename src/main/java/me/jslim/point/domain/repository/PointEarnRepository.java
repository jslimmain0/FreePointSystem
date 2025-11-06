package me.jslim.point.domain.repository;

import me.jslim.point.domain.entity.PointEarn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PointEarnRepository extends JpaRepository<PointEarn, String> {
    Optional<PointEarn> findByPointKey(String pointKey);
    @Query(value = """
      select *
        from point_earn e
       where e.wallet_key = :walletKey
         and e.earn_status = 'AVAILABLE'
         and e.expire_date >= :today
         and (
              :lastManual is null
              or e.is_manual < :lastManual
              or (e.is_manual = :lastManual and e.expire_date > :lastExpire)
              or (e.is_manual = :lastManual and e.expire_date = :lastExpire and e.earn_key > :lastEarnKey)
           )
       order by e.is_manual desc, e.expire_date asc, e.earn_key asc
       limit :limit
      """, nativeQuery = true)
    List<PointEarn> findAvailableEarns(
            @Param("walletKey") String walletKey,
            @Param("today") LocalDate today,
            @Param("lastManual") Boolean lastManual,
            @Param("lastExpire") LocalDate lastExpire,
            @Param("lastEarnKey") String lastEarnKey,
            @Param("limit") int limit);

    @Query(value = """
      select *
        from point_earn e
       where e.expire_date < :today
    """, nativeQuery = true)
    List<PointEarn> findExpiredEarns(@Param("today") LocalDate today);
}

