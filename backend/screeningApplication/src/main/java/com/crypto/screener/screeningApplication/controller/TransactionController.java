package com.crypto.screener.screeningApplication.controller;

import com.crypto.screener.screeningApplication.dto.TransactionDto;
import com.crypto.screener.screeningApplication.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // POST /api/transactions
    @PostMapping("/api/transactions")
    public ResponseEntity<TransactionDto.Response> addTransaction(
            Authentication auth,
            @Valid @RequestBody TransactionDto.AddRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.addTransaction(userId(auth), request));
    }

    // DELETE /api/transactions/{id}
    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<Map<String, String>> deleteTransaction(
            Authentication auth,
            @PathVariable Long id) {
        transactionService.deleteTransaction(userId(auth), id);
        return ResponseEntity.ok(Map.of("message", "Transaction " + id + " deleted"));
    }

    // GET /api/holdings/{holdingId}/transactions
    @GetMapping("/api/holdings/{holdingId}/transactions")
    public ResponseEntity<List<TransactionDto.Response>> getTransactions(
            Authentication auth,
            @PathVariable Long holdingId) {
        return ResponseEntity.ok(
                transactionService.getTransactions(userId(auth), holdingId));
    }

    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}