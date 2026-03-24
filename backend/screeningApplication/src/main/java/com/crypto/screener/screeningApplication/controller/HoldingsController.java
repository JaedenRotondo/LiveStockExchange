package com.crypto.screener.screeningApplication.controller;

import com.crypto.screener.screeningApplication.dto.HoldingsDto;
import com.crypto.screener.screeningApplication.service.HoldingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holdings")
public class HoldingsController {

    private final HoldingsService service;

    public HoldingsController(HoldingsService service) {
        this.service = service;
    }

    // GET /api/holdings
    @GetMapping
    public ResponseEntity<List<HoldingsDto.Response>> getAll(Authentication auth) {
        return ResponseEntity.ok(service.getHoldings(userId(auth)));
    }

    // GET /api/holdings/BTCUSDT
    @GetMapping("/{symbol}")
    public ResponseEntity<HoldingsDto.Response> getOne(
            Authentication auth,
            @PathVariable String symbol) {
        return ResponseEntity.ok(service.getHolding(userId(auth), symbol));
    }

    // POST /api/holdings
    @PostMapping
    public ResponseEntity<HoldingsDto.Response> add(
            Authentication auth,
            @Valid @RequestBody HoldingsDto.AddRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.addHolding(userId(auth), request));
    }

    // DELETE /api/holdings/BTCUSDT
    @DeleteMapping("/{symbol}")
    public ResponseEntity<Map<String, String>> remove(
            Authentication auth,
            @PathVariable String symbol) {
        service.removeHolding(userId(auth), symbol);
        return ResponseEntity.ok(Map.of("message", symbol.toUpperCase() + " removed"));
    }

    // TODO: POST /api/holdings/transactions — addTransaction
    // TODO: DELETE /api/holdings/transactions/{id} — deleteTransaction
    // TODO: GET /api/holdings/{holdingId}/transactions — getTransactions

    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}
