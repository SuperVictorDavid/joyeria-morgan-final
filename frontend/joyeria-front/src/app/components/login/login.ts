import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  username = '';
  password = '';
  isLoading = false;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  onLogin() {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage = 'Por favor ingrese su usuario y contraseña.';
      this.cdr.detectChanges();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    const credentials = { user: this.username, password: this.password };

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.toastService.success(`¡Bienvenido al sistema, ${this.username}!`);
        this.cdr.detectChanges();
        // Joyeria Morgan: entra al dashboard
        this.router.navigate(['/inicio']);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error de autenticación:', err);
        const message = this.extractErrorMessage(err);
        this.errorMessage = message;
        this.toastService.error(message);
        this.cdr.detectChanges();
      },
    });
  }

  private extractErrorMessage(err: any): string {
    if (!err) {
      return 'Usuario o contraseña incorrectos';
    }

    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Verifique su conexión.';
    }

    let message = '';

    if (typeof err.error === 'string') {
      const trimmed = err.error.trim();
      if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
        try {
          const parsed = JSON.parse(trimmed);
          if (typeof parsed === 'string') {
            message = parsed;
          } else if (parsed && typeof parsed === 'object') {
            message =
              parsed.message ||
              parsed.mensaje ||
              parsed.error ||
              parsed.detail ||
              parsed.title ||
              '';
          }
        } catch {
          message = trimmed;
        }
      } else if (!trimmed.startsWith('<')) {
        // Ignorar HTML si el servidor devolvió página de error por defecto
        message = trimmed;
      }
    } else if (err.error && typeof err.error === 'object') {
      message =
        err.error.message ||
        err.error.mensaje ||
        err.error.error ||
        err.error.detail ||
        err.error.title ||
        '';
    }

    if (
      !message &&
      err.message &&
      typeof err.message === 'string' &&
      !err.message.includes('Http failure response')
    ) {
      message = err.message;
    }

    if (!message || message.toLowerCase() === 'unauthorized') {
      message = 'Usuario o contraseña incorrectos';
    }

    return message.trim();
  }
}
