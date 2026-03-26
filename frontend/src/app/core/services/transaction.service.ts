import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction, AddTransactionRequest } from '../models/holding.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly API = '/api/transactions';
  private readonly http = inject(HttpClient);

  add(request: AddTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.API, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  getByHolding(holdingId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`/api/holdings/${holdingId}/transactions`);
  }
}
