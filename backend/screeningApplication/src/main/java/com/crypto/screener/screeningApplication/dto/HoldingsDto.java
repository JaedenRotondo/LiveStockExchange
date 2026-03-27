package com.crypto.screener.screeningApplication.dto;

import com.crypto.screener.screeningApplication.model.Holdings.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HoldingsDto {

    // ── Inbound ──────────────────────────────────────────────────
    public static class AddRequest {

        @NotBlank(message = "Symbol is required")
        private String symbol;

        @NotNull(message = "Asset type is required")
        private AssetType assetType;

        private String notes;

        public String getSymbol()        { return symbol; }
        public AssetType getAssetType()  { return assetType; }
        public String getNotes()         { return notes; }
        public void setSymbol(String v)       { this.symbol = v; }
        public void setAssetType(AssetType v) { this.assetType = v; }
        public void setNotes(String v)        { this.notes = v; }
    }

    // ── Outbound ─────────────────────────────────────────────────
    public static class Response {

        private Long id;
        private String symbol;
        private BigDecimal totalQty;
        private BigDecimal avgPrice;
        private AssetType assetType;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Response(Long id, String symbol, BigDecimal totalQty, BigDecimal avgPrice,
                        AssetType assetType, String notes,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id        = id;
            this.symbol    = symbol;
            this.totalQty  = totalQty;
            this.avgPrice  = avgPrice;
            this.assetType = assetType;
            this.notes     = notes;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId()                { return id; }
        public String getSymbol()          { return symbol; }
        public BigDecimal getTotalQty()    { return totalQty; }
        public BigDecimal getAvgPrice()    { return avgPrice; }
        public AssetType getAssetType()    { return assetType; }
        public String getNotes()           { return notes; }
        public LocalDateTime getCreatedAt(){ return createdAt; }
        public LocalDateTime getUpdatedAt(){ return updatedAt; }
    }
}
