package com.crypto.screener.screeningApplication.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorites")
public class FavoriteStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType = AssetType.CRYPTO;

    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    public enum AssetType { CRYPTO, STOCK }

    public FavoriteStock() {}

    public FavoriteStock(String userId, String symbol, AssetType assetType) {
        this.userId    = userId;
        this.symbol    = symbol.toUpperCase();
        this.assetType = assetType;
    }

    public Long getId()              { return id; }
    public String getUserId()        { return userId; }
    public String getSymbol()        { return symbol; }
    public AssetType getAssetType()  { return assetType; }
    public LocalDateTime getAddedAt(){ return addedAt; }
}