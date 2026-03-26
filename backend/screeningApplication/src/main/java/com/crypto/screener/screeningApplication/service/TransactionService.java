package com.crypto.screener.screeningApplication.service;

import com.crypto.screener.screeningApplication.dto.TransactionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final HoldingsService holdingsService;

    public TransactionService(HoldingsService holdingsService) {
        this.holdingsService = holdingsService;
    }

    // ── POST /api/holdings/transactions ───────────────────────────
    @Transactional
    public TransactionDto.Response addTransaction(String userId,
                                                  TransactionDto.AddRequest request) {
        return holdingsService.addTransaction(userId, request);
    }

    // ── DELETE /api/holdings/transactions/{id} ────────────────────
    @Transactional
    public void deleteTransaction(String userId, Long transactionId) {
        holdingsService.deleteTransaction(userId, transactionId);
    }

    // ── GET /api/transactions/{holdingId} ─────────────────────────
    public List<TransactionDto.Response> getTransactions(String userId, Long holdingId) {
        return holdingsService.getTransactions(userId, holdingId);
    }
}