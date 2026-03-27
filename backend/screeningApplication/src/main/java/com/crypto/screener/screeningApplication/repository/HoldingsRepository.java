package com.crypto.screener.screeningApplication.repository;

import com.crypto.screener.screeningApplication.model.Holdings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingsRepository extends JpaRepository<Holdings, Long> {

    List<Holdings> findByUserId(String userId);

    Optional<Holdings> findByUserIdAndSymbol(String userId, String symbol);

    boolean existsByUserIdAndSymbol(String userId, String symbol);

    void deleteByUserIdAndSymbol(String userId, String symbol);
}
