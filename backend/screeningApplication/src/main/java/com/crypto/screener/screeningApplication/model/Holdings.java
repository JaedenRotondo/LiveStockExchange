package com.crypto.screener.screeningApplication.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "symbol"})
})
public class Holdings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "total_qty", nullable = false, precision = 18, scale = 8)
    private BigDecimal totalQty = BigDecimal.ZERO;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal avgPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType = AssetType.CRYPTO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum AssetType { CRYPTO, STOCK }

    public Holdings() {}

    public Holdings(String userId, String symbol, AssetType assetType) {
        this.userId = userId;
        this.symbol = symbol.toUpperCase();
        this.assetType = assetType;
    }

    // Getters
    public Long getId()                { return id; }
    public String getUserId()          { return userId; }
    public String getSymbol()          { return symbol; }
    public BigDecimal getTotalQty()    { return totalQty; }
    public BigDecimal getAvgPrice()    { return avgPrice; }
    public AssetType getAssetType()    { return assetType; }
    public String getNotes()           { return notes; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }

    // Setters
    public void setTotalQty(BigDecimal totalQty)  { this.totalQty = totalQty; }
    public void setAvgPrice(BigDecimal avgPrice)   { this.avgPrice = avgPrice; }
    public void setNotes(String notes)             { this.notes = notes; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
