import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Joya } from './joya';

export interface CarritoItem {
  joya: Joya;
  cantidad: number;
}

const STORAGE_KEY = 'carrito-morga';

function loadInitial(): CarritoItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((i: any) => i && i.joya && typeof i.cantidad === 'number');
  } catch {
    return [];
  }
}

@Injectable({ providedIn: 'root' })
export class CarritoStore {
  private _items = new BehaviorSubject<CarritoItem[]>(loadInitial());
  readonly items$ = this._items.asObservable();

  get items(): CarritoItem[] {
    return this._items.value;
  }

  get count(): number {
    return this._items.value.reduce((a, i) => a + (i.cantidad ?? 0), 0);
  }

  private persist(items: CarritoItem[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // almacenamiento no disponible: se sigue en memoria
    }
    this._items.next([...items]);
  }

  add(joya: Joya, cantidad = 1): void {
    if (!joya || cantidad <= 0) return;
    const items = [...this._items.value];
    const existe = items.find((i) => i.joya.id === joya.id);
    if (existe) {
      existe.cantidad += cantidad;
    } else {
      items.push({ joya: { ...joya }, cantidad });
    }
    this.persist(items);
  }

  remove(index: number): void {
    const items = [...this._items.value];
    if (index < 0 || index >= items.length) return;
    items.splice(index, 1);
    this.persist(items);
  }

  clear(): void {
    this.persist([]);
  }

  total(): number {
    return this._items.value.reduce((a, i) => a + (i.joya.price ?? 0) * (i.cantidad ?? 0), 0);
  }
}
