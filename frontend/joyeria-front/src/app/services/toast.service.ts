import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ToastMessage {
  id: number;
  text: string;
  type: 'success' | 'error' | 'warning' | 'info';
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  toasts$ = this.toastsSubject.asObservable();
  private nextId = 1;

  show(text: string, type: 'success' | 'error' | 'warning' | 'info' = 'info', duration = 3500) {
    const id = this.nextId++;
    const toast: ToastMessage = { id, text, type };
    const current = this.toastsSubject.value;
    this.toastsSubject.next([...current, toast]);

    setTimeout(() => {
      this.remove(id);
    }, duration);
  }

  success(text: string) {
    this.show(text, 'success');
  }

  error(text: string) {
    this.show(text, 'error', 5000);
  }

  warning(text: string) {
    this.show(text, 'warning');
  }

  info(text: string) {
    this.show(text, 'info');
  }

  remove(id: number) {
    const updated = this.toastsSubject.value.filter((t) => t.id !== id);
    this.toastsSubject.next(updated);
  }
}
