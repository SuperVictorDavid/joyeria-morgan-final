import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Joya {
  id?: number;
  name: string;
  descripcion?: string;
  tipoProducto?: string;
  tipoMaterial?: string;
  quilates?: number;
  peso?: number;
  price: number;
  stock: number;
  estado?: string;
}

@Injectable({ providedIn: 'root' })
export class JoyaService {
  private http = inject(HttpClient);
  private base = `${environment.inventarioUrl}/products`;

  listar(): Observable<Joya[]> {
    return this.http.get<Joya[]>(this.base);
  }
  obtener(id: number): Observable<Joya> {
    return this.http.get<Joya>(`${this.base}/${id}`);
  }
  crear(joya: Joya): Observable<Joya> {
    return this.http.post<Joya>(this.base, joya);
  }
  actualizar(id: number, joya: Joya): Observable<Joya> {
    return this.http.put<Joya>(`${this.base}/${id}`, joya);
  }
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
