package com.crypto.screener.screeningApplication.dto;

import com.crypto.screener.screeningApplication.model.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDto {

    // ── Inbound ──────────────────────────────────────────────────
    public static class AddRequest {

        @NotBlank(message = "Symbol is required")
        private String symbol;

        @NotNull(message = "Transaction type is required")
        private TransactionType type;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.00000001", message = "Quantity must be positive")
        private BigDecimal quantity;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00000001", message = "Price must be positive")
        private BigDecimal price;

        private String note;

        @NotNull(message = "Date is required")
        private LocalDate date;

        public String getSymbol()            { return symbol; }
        public TransactionType getType()     { return type; }
        public BigDecimal getQuantity()      { return quantity; }
        public BigDecimal getPrice()         { return price; }
        public String getNote()              { return note; }
        public LocalDate getDate()           { return date; }

        public void setSymbol(String v)           { this.symbol = v; }
        public void setType(TransactionType v)    { this.type = v; }
        public void setQuantity(BigDecimal v)     { this.quantity = v; }
        public void setPrice(BigDecimal v)        { this.price = v; }
        public void setNote(String v)             { this.note = v; }
        public void setDate(LocalDate v)          { this.date = v; }
    }

    // ── Outbound ─────────────────────────────────────────────────
    public static class Response {

        private Long id;
        private Long holdingId;
        private TransactionType type;
        private BigDecimal quantity;
        private BigDecimal price;
        private String note;
        private LocalDate date;
        private LocalDateTime createdAt;

        public Response(Long id, Long holdingId, TransactionType type,
                        BigDecimal quantity, BigDecimal price,
                        String note, LocalDate date, LocalDateTime createdAt) {
            this.id        = id;
            this.holdingId = holdingId;
            this.type      = type;
            this.quantity  = quantity;
            this.price     = price;
            this.note      = note;
            this.date      = date;
            this.createdAt = createdAt;
        }

        public Long getId()                  { return id; }
        public Long getHoldingId()           { return holdingId; }
        public TransactionType getType()     { return type; }
        public BigDecimal getQuantity()      { return quantity; }
        public BigDecimal getPrice()         { return price; }
        public String getNote()              { return note; }
        public LocalDate getDate()           { return date; }
        public LocalDateTime getCreatedAt()  { return createdAt; }
    }
}