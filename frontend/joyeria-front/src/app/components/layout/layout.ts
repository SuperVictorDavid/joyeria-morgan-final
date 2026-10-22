import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService, UserSession } from '../../services/auth';
import { ToastService, ToastMessage } from '../../services/toast.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class LayoutComponent implements OnInit {
  currentUser: UserSession | null = null;
  toasts: ToastMessage[] = [];

  constructor(
    public authService: AuthService,
    private router: Router,
    public toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
    });

    this.toastService.toasts$.subscribe((toasts) => {
      this.toasts = toasts;
    });
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  isOperador(): boolean {
    return this.authService.isOperador();
  }

  logout() {
    this.authService.logout();
    this.toastService.info('Sesión cerrada correctamente');
    this.router.navigate(['/login']);
  }

  removeToast(id: number) {
    this.toastService.remove(id);
  }
}
