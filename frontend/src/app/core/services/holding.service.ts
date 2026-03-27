import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Holding } from '../models/holding.model';

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
}
