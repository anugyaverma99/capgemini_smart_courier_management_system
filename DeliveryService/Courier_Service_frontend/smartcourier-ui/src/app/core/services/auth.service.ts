import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
  userId: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = `${environment.apiUrl}/auth`;

  public userSignal = signal<AuthResponse | null>(this.loadSession());
  public isLoggedInSignal = computed(() => !!this.userSignal());
  public isAdminSignal = computed(() => this.userSignal()?.role === 'ADMIN');
  public isCustomerSignal = computed(() => this.userSignal()?.role === 'CUSTOMER');

  constructor(private http: HttpClient) {}

  private loadSession(): AuthResponse | null {
    if (typeof localStorage === 'undefined') return null;
    const token = localStorage.getItem('token');
    if (!token) return null;
    return {
      token,
      email: localStorage.getItem('email') || '',
      fullName: localStorage.getItem('fullName') || '',
      role: localStorage.getItem('role') || '',
      userId: Number(localStorage.getItem('userId')) || 0
    };
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, { email, password }).pipe(
      tap(res => this.saveSession(res))
    );
  }

  signup(fullName: string, email: string, password: string, phone: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/signup`, { fullName, email, password, phone }).pipe(
      tap(res => this.saveSession(res))
    );
  }

  saveSession(res: AuthResponse): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('token', res.token);
      localStorage.setItem('email', res.email);
      localStorage.setItem('fullName', res.fullName);
      localStorage.setItem('role', res.role);
      localStorage.setItem('userId', res.userId.toString());
    }
    this.userSignal.set(res);
  }

  getToken(): string | null { return this.userSignal()?.token || null; }
  getRole(): string | null { return this.userSignal()?.role || null; }
  getEmail(): string | null { return this.userSignal()?.email || null; }
  getFullName(): string | null { return this.userSignal()?.fullName || null; }
  getUserId(): string | null { return this.userSignal()?.userId?.toString() || null; }
  isLoggedIn(): boolean { return this.isLoggedInSignal(); }
  isAdmin(): boolean { return this.isAdminSignal(); }
  isCustomer(): boolean { return this.isCustomerSignal(); }
  
  logout(): void { 
    if (typeof localStorage !== 'undefined') localStorage.clear(); 
    this.userSignal.set(null);
  }
}
