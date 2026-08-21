import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink, CommonModule],
  templateUrl: './signup.html',
  styleUrl: './signup.css'
})
export class Signup {
  fullName = signal('');
  email = signal('');
  password = signal('');
  phone = signal('');
  errorMessage = signal('');
  loading = signal(false);

  constructor(private authService: AuthService, private router: Router) {}

  onSignup() {
    if (!this.fullName() || !this.email() || !this.password() || !this.phone()) {
      this.errorMessage.set('Please fill in all fields.');
      return;
    }
    if (!/^[0-9]{10}$/.test(this.phone())) {
      this.errorMessage.set('Phone number must be exactly 10 digits.');
      return;
    }
    this.loading.set(true);
    this.errorMessage.set('');
    this.authService.signup(this.fullName(), this.email(), this.password(), this.phone()).subscribe({
      next: (res) => {
        if (res.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/customer/dashboard']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.error || 'Signup failed. Please try again.');
      }
    });
  }
}
