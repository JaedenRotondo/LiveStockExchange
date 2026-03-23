export type AssetType = 'CRYPTO' | 'STOCK';

export interface FavoriteStock {
  id: number;
  symbol: string;
  assetType: AssetType;
  addedAt: string;
}

export interface AddFavoriteRequest {
  symbol: string;
  assetType: AssetType;
}