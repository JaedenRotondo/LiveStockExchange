package com.crypto.screener.screeningApplication.repository;

import com.crypto.screener.screeningApplication.model.FavoriteStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, Long> {

    List<FavoriteStock> findByUserId(String userId);

    Optional<FavoriteStock> findByUserIdAndSymbol(String userId, String symbol);

    boolean existsByUserIdAndSymbol(String userId, String symbol);

    void deleteByUserIdAndSymbol(String userId, String symbol);
}