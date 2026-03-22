package com.crypto.screener.screeningApplication.controller;

import com.crypto.screener.screeningApplication.dto.FavoriteDto;
import com.crypto.screener.screeningApplication.service.FavoriteStockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteStockService service;

    public FavoriteController(FavoriteStockService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FavoriteDto.Response>> getAll(Authentication auth) {
        return ResponseEntity.ok(service.getFavorites(userId(auth)));
    }

    @PostMapping
    public ResponseEntity<FavoriteDto.Response> add(
            Authentication auth,
            @Valid @RequestBody FavoriteDto.AddRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addFavorite(userId(auth), request));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Map<String, String>> remove(
            Authentication auth,
            @PathVariable String symbol) {
        service.removeFavorite(userId(auth), symbol);
        return ResponseEntity.ok(Map.of("message", symbol.toUpperCase() + " removed"));
    }

    @GetMapping("/{symbol}/check")
    public ResponseEntity<Map<String, Boolean>> check(
            Authentication auth,
            @PathVariable String symbol) {
        return ResponseEntity.ok(Map.of("isFavorite", service.isFavorite(userId(auth), symbol)));
    }

    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}