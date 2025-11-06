package me.jslim.point.domain.repository;

import me.jslim.point.domain.entity.PointWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointWalletRepository extends JpaRepository<PointWallet, String> {
    Optional<PointWallet> findByUserId(String userId);
}

