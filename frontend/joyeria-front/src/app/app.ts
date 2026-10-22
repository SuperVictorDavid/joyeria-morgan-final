import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { ToastService, ToastMessage } from './services/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  title = 'joyeria-front';
  toasts: ToastMessage[] = [];

  constructor(
    public toastService: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.toastService.toasts$.subscribe((toasts) => {
      this.toasts = toasts;
      this.cdr.detectChanges();
    });
  }

  removeToast(id: number) {
    this.toastService.remove(id);
  }
}
