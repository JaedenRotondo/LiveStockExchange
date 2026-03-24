export interface Holding {
  id: number;
  symbol: string;
  totalQty: number;
  avgPrice: number;
  assetType: 'CRYPTO' | 'STOCK';
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Transaction {
  id: number;
  holdingId: number;
  type: 'BUY' | 'SELL';
  quantity: number;
  price: number;
  note: string | null;
  date: string;
  createdAt: string;
}

export interface AddTransactionRequest {
  symbol: string;
  type: 'BUY' | 'SELL';
  quantity: number;
  price: number;
  note?: string;
  date: string;
}
