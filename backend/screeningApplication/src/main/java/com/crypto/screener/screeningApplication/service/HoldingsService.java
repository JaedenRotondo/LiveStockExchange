package com.crypto.screener.screeningApplication.service;

import com.crypto.screener.screeningApplication.dto.HoldingsDto;
import com.crypto.screener.screeningApplication.dto.TransactionDto;
import com.crypto.screener.screeningApplication.model.Holdings;
import com.crypto.screener.screeningApplication.model.Transaction;
import com.crypto.screener.screeningApplication.model.Transaction.TransactionType;
import com.crypto.screener.screeningApplication.repository.HoldingsRepository;
import com.crypto.screener.screeningApplication.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HoldingsService {

    private final HoldingsRepository repository;
    private final TransactionRepository transactionRepository;

    public HoldingsService(HoldingsRepository repository,
                           TransactionRepository transactionRepository) {
        this.repository            = repository;
        this.transactionRepository = transactionRepository;
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

    // ── Add a transaction and recalculate the parent holding ───────
    @Transactional
    public TransactionDto.Response addTransaction(String userId,
                                                  TransactionDto.AddRequest request) {
        String symbol = request.getSymbol().toUpperCase();

        Holdings holding = repository
                .findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> {
                    Holdings h = new Holdings(userId, symbol, Holdings.AssetType.CRYPTO);
                    return repository.save(h);
                });

        Transaction tx = new Transaction(
                holding,
                request.getType(),
                request.getQuantity(),
                request.getPrice(),
                request.getNote(),
                request.getDate()
        );
        Transaction saved = transactionRepository.save(tx);

        recalculateHolding(holding);

        return toTransactionResponse(saved);
    }

    // ── Delete a transaction and recalculate the parent holding ────
    @Transactional
    public void deleteTransaction(String userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found"));

        Holdings holding = tx.getHolding();

        if (!holding.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this transaction");
        }

        transactionRepository.delete(tx);
        recalculateHolding(holding);
    }

    // ── Get all transactions for a holding ─────────────────────────
    public List<TransactionDto.Response> getTransactions(String userId, Long holdingId) {
        Holdings holding = repository.findById(holdingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Holding not found"));

        if (!holding.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this holding");
        }

        return transactionRepository.findByHoldingIdOrderByDateAsc(holdingId)
                .stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    // ── Recalculate totalQty & avgPrice (weighted average cost) ────
    public void recalculateHolding(Holdings holding) {
        List<Transaction> txs = transactionRepository
                .findByHoldingIdOrderByDateAsc(holding.getId());

        if (txs.isEmpty()) {
            repository.delete(holding);
            return;
        }

        BigDecimal totalQty  = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Transaction tx : txs) {
            if (tx.getType() == TransactionType.BUY) {
                totalCost = totalCost.add(tx.getQuantity().multiply(tx.getPrice()));
                totalQty  = totalQty.add(tx.getQuantity());
            } else { // SELL
                totalQty = totalQty.subtract(tx.getQuantity());
                if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                    // Reduce cost proportionally
                    BigDecimal soldRatio = tx.getQuantity()
                            .divide(totalQty.add(tx.getQuantity()), 18, RoundingMode.HALF_UP);
                    totalCost = totalCost.subtract(totalCost.multiply(soldRatio));
                } else {
                    totalCost = BigDecimal.ZERO;
                }
            }
        }

        if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
            repository.delete(holding);
            return;
        }

        BigDecimal avgPrice = totalCost.divide(totalQty, 8, RoundingMode.HALF_UP);

        holding.setTotalQty(totalQty);
        holding.setAvgPrice(avgPrice);
        holding.setUpdatedAt(LocalDateTime.now());
        repository.save(holding);
    }

    // ── Mappers ────────────────────────────────────────────────────
    private HoldingsDto.Response toResponse(Holdings h) {
        return new HoldingsDto.Response(
                h.getId(), h.getSymbol(), h.getTotalQty(), h.getAvgPrice(),
                h.getAssetType(), h.getNotes(), h.getCreatedAt(), h.getUpdatedAt());
    }

    private TransactionDto.Response toTransactionResponse(Transaction tx) {
        return new TransactionDto.Response(
                tx.getId(),
                tx.getHolding().getId(),
                tx.getType(),
                tx.getQuantity(),
                tx.getPrice(),
                tx.getNote(),
                tx.getDate(),
                tx.getCreatedAt()
        );
    }
}