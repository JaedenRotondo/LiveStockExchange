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
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // POST /api/transactions
    @PostMapping
    public ResponseEntity<TransactionDto.Response> addTransaction(
            Authentication auth,
            @Valid @RequestBody TransactionDto.AddRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transactionService.addTransaction(userId(auth), request));
    }

    // DELETE /api/transactions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTransaction(
            Authentication auth,
            @PathVariable Long id) {
        transactionService.deleteTransaction(userId(auth), id);
        return ResponseEntity.ok(Map.of("message", "Transaction " + id + " deleted"));
    }

    private String userId(Authentication auth) {
        return (String) auth.getPrincipal();
    }
}