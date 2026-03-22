package com.crypto.screener.screeningApplication.service;

import com.crypto.screener.screeningApplication.dto.FavoriteDto;
import com.crypto.screener.screeningApplication.model.FavoriteStock;
import com.crypto.screener.screeningApplication.repository.FavoriteStockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteStockService {

    private final FavoriteStockRepository repository;

    public FavoriteStockService(FavoriteStockRepository repository) {
        this.repository = repository;
    }

    // ── Get all favourites for a user ────────────────────────────
    public List<FavoriteDto.Response> getFavorites(String userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(f -> new FavoriteDto.Response(
                        f.getId(), f.getSymbol(), f.getAssetType(), f.getAddedAt()))
                .collect(Collectors.toList());
    }

    // ── Add a favourite ──────────────────────────────────────────
    @Transactional
    public FavoriteDto.Response addFavorite(String userId, FavoriteDto.AddRequest request) {
        String symbol = request.getSymbol().toUpperCase();

        if (repository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, symbol + " is already in your favourites");
        }

        FavoriteStock saved = repository.save(
                new FavoriteStock(userId, symbol, request.getAssetType()));

        return new FavoriteDto.Response(
                saved.getId(), saved.getSymbol(), saved.getAssetType(), saved.getAddedAt());
    }

    // ── Remove a favourite ───────────────────────────────────────
    @Transactional
    public void removeFavorite(String userId, String symbol) {
        String upper = symbol.toUpperCase();
        if (!repository.existsByUserIdAndSymbol(userId, upper)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, upper + " not found in your favourites");
        }
        repository.deleteByUserIdAndSymbol(userId, upper);
    }

    // ── Check if a symbol is already favourited ──────────────────
    public boolean isFavorite(String userId, String symbol) {
        return repository.existsByUserIdAndSymbol(userId, symbol.toUpperCase());
    }
}