import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { FavoriteService } from '../../../core/services/favorite.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent {
  fullName = '';
  email    = '';
  password = '';
  loading  = signal(false);
  error    = signal('');

  constructor(
    private auth: AuthService,
    private favorites: FavoriteService,
    private router: Router
  ) {}

  submit(): void {
    if (!this.email || !this.password || !this.fullName) return;
    if (this.password.length < 8) {
      this.error.set('Password must be at least 8 characters');
      return;
    }
    this.loading.set(true);
    this.error.set('');

    this.auth.register({ email: this.email, password: this.password, fullName: this.fullName }).subscribe({
      next: () => {
        this.favorites.load().subscribe();
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Registration failed');
        this.loading.set(false);
      }
    });
  }
}