package com.crypto.screener.screeningApplication.controller;

import com.crypto.screener.screeningApplication.dto.HoldingsDto;
import com.crypto.screener.screeningApplication.dto.TransactionDto;
import com.crypto.screener.screeningApplication.service.HoldingsService;
import com.crypto.screener.screeningApplication.service.TransactionService;
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

    private final HoldingsService holdingsService;
    private final TransactionService transactionService;

    public HoldingsController(HoldingsService holdingsService,
                              TransactionService transactionService) {
        this.holdingsService    = holdingsService;
        this.transactionService = transactionService;
    }

    // GET /api/holdings
    @GetMapping
    public ResponseEntity<List<HoldingsDto.Response>> getAll(Authentication auth) {
        return ResponseEntity.ok(holdingsService.getHoldings(userId(auth)));
    }

    // GET /api/holdings/BTCUSDT
    @GetMapping("/{symbol}")
    public ResponseEntity<HoldingsDto.Response> getOne(
            Authentication auth,
            @PathVariable String symbol) {
        return ResponseEntity.ok(holdingsService.getHolding(userId(auth), symbol));
    }

    // POST /api/holdings
    @PostMapping
    public ResponseEntity<HoldingsDto.Response> add(
            Authentication auth,
            @Valid @RequestBody HoldingsDto.AddRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(holdingsService.addHolding(userId(auth), request));
    }

    // DELETE /api/holdings/BTCUSDT
    @DeleteMapping("/{symbol}")
    public ResponseEntity<Map<String, String>> remove(
            Authentication auth,
            @PathVariable String symbol) {
        holdingsService.removeHolding(userId(auth), symbol);
        return ResponseEntity.ok(Map.of("message", symbol.toUpperCase() + " removed"));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}