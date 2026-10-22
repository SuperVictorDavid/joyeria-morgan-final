import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Venta {
  saleId?: number;
  numeroBoleta?: string;
  productId: number;
  quantity: number;
  customerId: number;
  vendedor?: string;
  total?: number;
  status?: string;
  fechaVenta?: string;
}

@Injectable({ providedIn: 'root' })
export class VentaService {
  private http = inject(HttpClient);
  private base = `${environment.ventasUrl}/sales`;

  listar(): Observable<Venta[]> {
    return this.http.get<Venta[]>(this.base);
  }
  obtener(id: number): Observable<Venta> {
    return this.http.get<Venta>(`${this.base}/${id}`);
  }
  detalle(id: number): Observable<any> {
    return this.http.get<any>(`${this.base}/${id}/details`);
  }
  crear(v: Venta): Observable<Venta> {
    return this.http.post<Venta>(this.base, v);
  }
  crearRabbit(v: Venta): Observable<Venta> {
    return this.http.post<Venta>(`${this.base}/rabbit-reserve`, v);
  }
  actualizar(id: number, v: Venta): Observable<Venta> {
    return this.http.put<Venta>(`${this.base}/${id}`, v);
  }
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
