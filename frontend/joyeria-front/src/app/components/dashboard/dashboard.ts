import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { JoyaService, Joya } from '../../services/joya';
import { VentaService } from '../../services/venta';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  ventasHoy = 0;
  montoHoy = 0;
  ventasMes = 0;
  montoMes = 0;
  totalVentas = 0;
  disponibles = 0;
  totalJoyas = 0;
  recientes: any[] = [];
  usuario = '';
  rolLabel = '';

  private joyasList: Joya[] = [];

  constructor(
    private joyas: JoyaService,
    private ventas: VentaService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  // Fecha local en formato YYYY-MM-DD (evita el desfase UTC de toISOString)
  private hoyLocal(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    this.usuario = user?.username ?? 'usuario';
    this.rolLabel = this.auth.isAdmin() ? 'Administrador' : 'Vendedor';

    forkJoin({ joyas: this.joyas.listar(), ventas: this.ventas.listar() }).subscribe({
      next: ({ joyas, ventas }) => {
        this.joyasList = joyas ?? [];
        this.totalJoyas = this.joyasList.length;
        this.disponibles = this.joyasList.filter((j) => (j.stock ?? 0) > 0).length;

        const hoy = this.hoyLocal();
        const mes = hoy.slice(0, 7);
        const deHoy = ventas.filter((v) => v.fechaVenta?.slice(0, 10) === hoy);
        const delMes = ventas.filter((v) => v.fechaVenta?.slice(0, 7) === mes);

        this.ventasHoy = deHoy.length;
        this.montoHoy = deHoy.reduce((a, v) => a + (v.total ?? 0), 0);
        this.ventasMes = delMes.length;
        this.montoMes = delMes.reduce((a, v) => a + (v.total ?? 0), 0);
        this.totalVentas = ventas.length;
        this.recientes = [...ventas].reverse().slice(0, 5);
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  nombreJoya(productId?: number): string {
    const j = this.joyasList.find((x) => x.id === productId);
    return j ? j.descripcion || j.name || `Joya ${productId}` : `Joya ${productId ?? '—'}`;
  }
}
