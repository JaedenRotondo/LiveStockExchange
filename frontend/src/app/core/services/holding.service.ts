import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';
import { Holding, Transaction, AddTransactionRequest } from '../models/holding.model';

@Injectable({ providedIn: 'root' })
export class HoldingService {
  private readonly API = '/api/holdings';
  private readonly http = inject(HttpClient);
  private _holdings = signal<Holding[]>([]);

  readonly holdings = this._holdings.asReadonly();

  load(): Observable<Holding[]> {
    return this.http.get<Holding[]>(this.API).pipe(
      tap((list) => this._holdings.set(list))
    );
  }

  addTransaction(request: AddTransactionRequest): Observable<Holding[]> {
    return this.http.post<Transaction>(`${this.API}/transactions`, request).pipe(
      switchMap(() => this.load())
    );
  }

  deleteTransaction(id: number): Observable<Holding[]> {
    return this.http.delete<void>(`${this.API}/transactions/${id}`).pipe(
      switchMap(() => this.load())
    );
  }
}
