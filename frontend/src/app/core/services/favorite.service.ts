import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { FavoriteStock, AddFavoriteRequest } from '../models/favorite.model';

@Injectable({ providedIn: 'root' })
export class FavoriteService {

  private readonly API = '/api/favorites';
  private _favorites = signal<FavoriteStock[]>([]);

  readonly favorites   = this._favorites.asReadonly();
  readonly favoriteIds = computed(() =>
    new Set(this._favorites().map(f => f.symbol.toLowerCase()))
  );

  constructor(private http: HttpClient) {}

  load(): Observable<FavoriteStock[]> {
    return this.http.get<FavoriteStock[]>(this.API).pipe(
      tap(list => this._favorites.set(list))
    );
  }

  add(request: AddFavoriteRequest): Observable<FavoriteStock> {
    return this.http.post<FavoriteStock>(this.API, request).pipe(
      tap(added => this._favorites.update(list => [...list, added]))
    );
  }

  remove(symbol: string): Observable<unknown> {
    return this.http.delete(`${this.API}/${symbol}`).pipe(
      tap(() => this._favorites.update(list =>
        list.filter(f => f.symbol !== symbol.toUpperCase())
      ))
    );
  }

  isFavorited(symbolId: string): boolean {
    return this.favoriteIds().has(symbolId.toLowerCase());
  }

  toggle(symbolId: string): Observable<unknown> {
    const symbol = symbolId.toUpperCase();
    if (this.isFavorited(symbolId)) {
      return this.remove(symbol);
    }
    return this.add({ symbol, assetType: 'CRYPTO' });
  }
}