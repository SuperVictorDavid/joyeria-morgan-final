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

export interface VentaItemRequest {
  productId: number;
  quantity: number;
}

export interface VentaBatchRequest {
  customerId: number;
  vendedor?: string;
  items: VentaItemRequest[];
}

export interface VentaBatchResponse {
  numeroBoleta: string;
  customerId: number;
  vendedor?: string;
  total: number;
  fechaVenta?: string;
  items: Venta[];
}

// Una boleta puede tener varias filas (una por producto). Se agrupan por numeroBoleta.
export interface VentaGrupo {
  numeroBoleta: string;
  customerId?: number;
  vendedor?: string;
  fechaVenta?: string;
  total: number;
  items: Venta[];
}

export function agruparVentas(ventas: Venta[]): VentaGrupo[] {
  const mapa = new Map<string, VentaGrupo>();
  ventas.forEach((v, idx) => {
    const key = v.numeroBoleta || `SIN-BOLETA-${v.saleId ?? idx}`;
    let grupo = mapa.get(key);
    if (!grupo) {
      grupo = {
        numeroBoleta: key,
        customerId: v.customerId,
        vendedor: v.vendedor,
        fechaVenta: v.fechaVenta,
        total: 0,
        items: [],
      };
      mapa.set(key, grupo);
    }
    grupo.total += v.total ?? 0;
    grupo.items.push(v);
    if (!grupo.fechaVenta && v.fechaVenta) grupo.fechaVenta = v.fechaVenta;
    if (grupo.customerId == null && v.customerId != null) grupo.customerId = v.customerId;
    if (!grupo.vendedor && v.vendedor) grupo.vendedor = v.vendedor;
  });
  return [...mapa.values()];
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
  crearLote(payload: VentaBatchRequest): Observable<VentaBatchResponse> {
    return this.http.post<VentaBatchResponse>(`${this.base}/batch`, payload);
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
