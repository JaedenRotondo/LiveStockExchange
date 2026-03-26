package com.crypto.screener.screeningApplication.service;

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
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final HoldingsRepository holdingsRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              HoldingsRepository holdingsRepository) {
        this.transactionRepository = transactionRepository;
        this.holdingsRepository    = holdingsRepository;
    }

    // ── POST /api/holdings/transactions ───────────────────────────
    @Transactional
    public TransactionDto.Response addTransaction(String userId,
                                                  TransactionDto.AddRequest request) {
        String symbol = request.getSymbol().toUpperCase();

        // Find or create the holding for this user + symbol
        Holdings holding = holdingsRepository
                .findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> {
                    Holdings h = new Holdings(userId, symbol, Holdings.AssetType.CRYPTO);
                    return holdingsRepository.save(h);
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

        return toResponse(saved);
    }

    // ── DELETE /api/holdings/transactions/{id} ────────────────────
    @Transactional
    public void deleteTransaction(String userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found"));

        Holdings holding = tx.getHolding();

        // Security: ensure the holding belongs to the requesting user
        if (!holding.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this transaction");
        }

        transactionRepository.delete(tx);
        recalculateHolding(holding);
    }

    // ── GET /api/transactions/{holdingId} ─────────────────
    public List<TransactionDto.Response> getTransactions(String userId, Long holdingId) {
        Holdings holding = holdingsRepository.findById(holdingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Holding not found"));

        if (!holding.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this holding");
        }

        return transactionRepository.findByHoldingIdOrderByDateAsc(holdingId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Recalculate totalQty & avgPrice (FIFO weighted average) ───
    private void recalculateHolding(Holdings holding) {
        List<Transaction> txs = transactionRepository
                .findByHoldingIdOrderByDateAsc(holding.getId());

        if (txs.isEmpty()) {
            holdingsRepository.delete(holding);
            return;
        }

        BigDecimal totalQty   = BigDecimal.ZERO;
        BigDecimal totalCost  = BigDecimal.ZERO;

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
            holdingsRepository.delete(holding);
            return;
        }

        BigDecimal avgPrice = totalCost.divide(totalQty, 8, RoundingMode.HALF_UP);

        holding.setTotalQty(totalQty);
        holding.setAvgPrice(avgPrice);
        holding.setUpdatedAt(LocalDateTime.now());
        holdingsRepository.save(holding);
    }

    // ── Mapper ─────────────────────────────────────────────────────
    private TransactionDto.Response toResponse(Transaction tx) {
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