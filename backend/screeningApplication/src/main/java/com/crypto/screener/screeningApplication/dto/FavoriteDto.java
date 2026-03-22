package com.crypto.screener.screeningApplication.dto;

import com.screener.model.FavoriteStock.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class FavoriteDto {

    public static class AddRequest {
        @NotBlank(message = "Symbol is required")
        private String symbol;

        @NotNull(message = "Asset type is required")
        private AssetType assetType;

        public String getSymbol() { return symbol; }
        public AssetType getAssetType() { return assetType; }
        public void setSymbol(String s) { this.symbol = s; }
        public void setAssetType(AssetType a) { this.assetType = a; }
    }

    public static class Response {
        private Long id;
        private String symbol;
        private AssetType assetType;
        private LocalDateTime addedAt;

        public Response(Long id, String symbol, AssetType assetType, LocalDateTime addedAt) {
            this.id = id; this.symbol = symbol;
            this.assetType = assetType; this.addedAt = addedAt;
        }
        public Long getId() { return id; }
        public String getSymbol() { return symbol; }
        public AssetType getAssetType() { return assetType; }
        public LocalDateTime getAddedAt() { return addedAt; }
    }
}