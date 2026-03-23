import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

const TOKEN_KEY = 'lse_token';
const USER_KEY  = 'lse_user';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly API = '/api/auth';

  private _currentUser = signal<AuthResponse | null>(this.loadUser());

  readonly currentUser  = this._currentUser.asReadonly();
  readonly isLoggedIn   = computed(() => !!this._currentUser());
  readonly userInitials = computed(() => {
    const u = this._currentUser();
    if (!u) return '';
    return u.fullName
      ? u.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
      : u.email[0].toUpperCase();
  });

  constructor(private http: HttpClient, private router: Router) {}

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register`, request).pipe(
      tap(res => this.storeSession(res))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, request).pipe(
      tap(res => this.storeSession(res))
    );
  }

  logout(): void {
    this.http.post(`${this.API}/logout`, {}).subscribe();
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res));
    this._currentUser.set(res);
  }

  private loadUser(): AuthResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}