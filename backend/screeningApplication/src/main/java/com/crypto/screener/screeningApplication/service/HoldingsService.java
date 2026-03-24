package com.crypto.screener.screeningApplication.service;

import com.crypto.screener.screeningApplication.dto.HoldingsDto;
import com.crypto.screener.screeningApplication.model.Holdings;
import com.crypto.screener.screeningApplication.repository.HoldingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HoldingsService {

    private final HoldingsRepository repository;

    public HoldingsService(HoldingsRepository repository) {
        this.repository = repository;
    }

    // ── Get all holdings for a user ────────────────────────────────
    public List<HoldingsDto.Response> getHoldings(String userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get single holding ─────────────────────────────────────────
    public HoldingsDto.Response getHolding(String userId, String symbol) {
        Holdings holding = repository.findByUserIdAndSymbol(userId, symbol.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, symbol + " not found in your holdings"));
        return toResponse(holding);
    }

    // ── Create a holding ───────────────────────────────────────────
    @Transactional
    public HoldingsDto.Response addHolding(String userId, HoldingsDto.AddRequest request) {
        String symbol = request.getSymbol().toUpperCase();

        if (repository.existsByUserIdAndSymbol(userId, symbol)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, symbol + " is already in your holdings");
        }

        Holdings holding = new Holdings(userId, symbol, request.getAssetType());
        holding.setNotes(request.getNotes());
        Holdings saved = repository.save(holding);
        return toResponse(saved);
    }

    // ── Delete a holding ───────────────────────────────────────────
    @Transactional
    public void removeHolding(String userId, String symbol) {
        String upper = symbol.toUpperCase();
        if (!repository.existsByUserIdAndSymbol(userId, upper)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, upper + " not found in your holdings");
        }
        repository.deleteByUserIdAndSymbol(userId, upper);
    }

    // TODO: addTransaction(userId, transactionRequest)
    //       - find or create Holding for user+symbol
    //       - save Transaction linked to the Holding
    //       - call recalculateHolding() to update totalQty & avgPrice

    // TODO: deleteTransaction(userId, transactionId)
    //       - remove the transaction
    //       - call recalculateHolding() on the parent holding
    //       - if holding has no transactions left, delete the holding

    // TODO: getTransactions(holdingId)
    //       - return all transactions for a given holding

    // TODO: recalculateHolding(holding)
    //       - query all transactions for the holding
    //       - walk through in FIFO order:
    //           BUY  → add to totalQty, compute weighted avg price
    //           SELL → subtract from totalQty
    //       - if totalQty == 0, delete the holding
    //       - otherwise, save updated totalQty & avgPrice

    // ── Map entity to response DTO ─────────────────────────────────
    private HoldingsDto.Response toResponse(Holdings h) {
        return new HoldingsDto.Response(
                h.getId(), h.getSymbol(), h.getTotalQty(), h.getAvgPrice(),
                h.getAssetType(), h.getNotes(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
