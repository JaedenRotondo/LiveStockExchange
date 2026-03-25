package com.crypto.screener.screeningApplication.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holding_id", nullable = false)
    private Holdings holding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType { BUY, SELL }

    public Transaction() {}

    public Transaction(Holdings holding, TransactionType type,
                       BigDecimal quantity, BigDecimal price,
                       String note, LocalDate date) {
        this.holding  = holding;
        this.type     = type;
        this.quantity = quantity;
        this.price    = price;
        this.note     = note;
        this.date     = date;
    }

    // Getters
    public Long getId()                  { return id; }
    public Holdings getHolding()         { return holding; }
    public TransactionType getType()     { return type; }
    public BigDecimal getQuantity()      { return quantity; }
    public BigDecimal getPrice()         { return price; }
    public String getNote()              { return note; }
    public LocalDate getDate()           { return date; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}