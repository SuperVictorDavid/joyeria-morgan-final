import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ToastService } from './toast.service';

export interface UserSession {
  username: string;
  rol: string;
  token?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private toastService = inject(ToastService);

  private loginUrl = `${environment.authUrl}/auth/login`;
  private currentUserSubject = new BehaviorSubject<UserSession | null>(this.getInitialUser());
  public currentUser$ = this.currentUserSubject.asObservable();
  private expirationTimer: any = null;

  constructor() {
    if (this.isAuthenticated()) {
      this.scheduleTokenExpirationTimer();
    }
  }

  /**
   * Decodifica el payload de un token JWT de forma segura
   */
  public decodeToken(token?: string | null): any {
    const t = token || this.getToken();
    if (!t) return null;
    try {
      const parts = t.split('.');
      if (parts.length !== 3) return null;
      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join(''),
      );
      return JSON.parse(jsonPayload);
    } catch (e) {
      return null;
    }
  }

  /**
   * Determina el rol a partir del token o nombre de usuario
   */
  private extractRole(token: string, username: string): string {
    const payload = this.decodeToken(token);
    let detectedRole = '';

    if (payload) {
      if (payload.rol) detectedRole = String(payload.rol);
      else if (payload.role) detectedRole = String(payload.role);
      else if (Array.isArray(payload.roles) && payload.roles.length > 0) detectedRole = String(payload.roles[0]);
      else if (Array.isArray(payload.authorities) && payload.authorities.length > 0) {
        detectedRole = payload.authorities[0].authority || payload.authorities[0];
      }
    }

    if (!detectedRole) {
      detectedRole = username.toLowerCase().includes('admin') ? 'ADMIN' : 'OPERADOR';
    }

    const cleanRole = detectedRole.toUpperCase().replace('ROLE_', '');
    return cleanRole.includes('ADMIN') ? 'ADMIN' : 'OPERADOR';
  }

  /**
   * Verifica si el token ha expirado
   */
  public isTokenExpired(token?: string | null): boolean {
    const t = token || this.getToken();
    if (!t) return true;

    const payload = this.decodeToken(t);
    if (!payload || !payload.exp) {
      return false;
    }

    const expDateMs = payload.exp * 1000;
    return Date.now() >= expDateMs;
  }

  /**
   * Programa el temporizador para cerrar sesión exactamente cuando el token expire
   */
  private scheduleTokenExpirationTimer() {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
      this.expirationTimer = null;
    }

    const token = this.getToken();
    if (!token) return;

    const payload = this.decodeToken(token);
    if (payload && payload.exp) {
      const timeoutMs = payload.exp * 1000 - Date.now();
      if (timeoutMs > 0) {
        this.expirationTimer = setTimeout(() => {
          this.handleSessionExpired();
        }, timeoutMs);
      } else {
        this.handleSessionExpired();
      }
    }
  }

  public handleSessionExpired() {
    this.logout();
    this.toastService.warning('Tu sesión ha expirado. Por favor inicia sesión nuevamente.');
    this.router.navigate(['/login']);
  }

  private getInitialUser(): UserSession | null {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    const rol = localStorage.getItem('rol');

    if (token && username && !this.isTokenExpired(token)) {
      return { username, rol: rol || 'OPERADOR', token };
    }

    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('rol');
    return null;
  }

  login(credentials: { user: string; password: string }): Observable<string> {
    const body = { username: credentials.user, password: credentials.password };

    return this.http.post<any>(this.loginUrl, body).pipe(
      map((res) => (typeof res === 'string' ? res : res.token)),
      tap((token) => {
        const rol = this.extractRole(token, credentials.user);
        localStorage.setItem('token', token);
        localStorage.setItem('username', credentials.user);
        localStorage.setItem('rol', rol);

        this.currentUserSubject.next({
          username: credentials.user,
          rol,
          token,
        });

        this.scheduleTokenExpirationTimer();
      }),
    );
  }

  logout() {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
      this.expirationTimer = null;
    }
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('rol');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;

    if (this.isTokenExpired(token)) {
      this.logout();
      return false;
    }

    return true;
  }

  getCurrentUser(): UserSession | null {
    return this.currentUserSubject.value;
  }

  isAdmin(): boolean {
    const rol = this.currentUserSubject.value?.rol?.toUpperCase() || '';
    return rol === 'ADMIN' || rol === 'ADMINISTRADOR';
  }

  isOperador(): boolean {
    return !this.isAdmin();
  }
}
