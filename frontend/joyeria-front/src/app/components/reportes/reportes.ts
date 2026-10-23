import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VentaService, Venta, agruparVentas } from '../../services/venta';
import { JoyaService, Joya } from '../../services/joya';
import { ToastService } from '../../services/toast.service';

interface ReporteModal {
  titulo: string;
  subtitulo: string;
  encabezados: string[];
  filas: (string | number)[][];
  totalLabel: string;
  totalValor: string;
}

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css',
})
export class ReportesComponent implements OnInit {
  ventas: Venta[] = [];
  joyas: Joya[] = [];

  // Reporte 1: Ventas por período
  repVentasDesde = '';
  repVentasHasta = '';
  repVentasCliente = 'Todos';

  // Reporte 2: Productos vendidos
  repProdDesde = '';
  repProdHasta = '';

  // Reporte 4: Ventas por vendedor
  repVendedor = 'Todos';

  modal: ReporteModal | null = null;

  constructor(
    private ventaService: VentaService,
    private joyaService: JoyaService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.ventaService.listar().subscribe({
      next: (d) => {
        this.ventas = d ?? [];
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('Error al cargar ventas para reportes'),
    });
    this.joyaService.listar().subscribe({
      next: (d) => {
        this.joyas = d ?? [];
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('Error al cargar inventario para reportes'),
    });
  }

  get clientes(): number[] {
    const set = new Set<number>();
    this.ventas.forEach((v) => {
      if (v.customerId != null) set.add(v.customerId);
    });
    return [...set].sort((a, b) => a - b);
  }

  get vendedores(): string[] {
    const set = new Set<string>();
    this.ventas.forEach((v) => {
      if (v.vendedor) set.add(v.vendedor);
    });
    return [...set].sort();
  }

  get conteoInventario(): number {
    return this.joyas.length;
  }

  nombreJoya(productId?: number): string {
    const j = this.joyas.find((x) => x.id === productId);
    return j ? j.descripcion || j.name || `Joya ${productId}` : `Joya ${productId ?? '—'}`;
  }

  // Verifica si la fecha de la venta cae dentro del rango (YYYY-MM-DD, inclusivo)
  private enRango(fechaVenta: string | undefined, desde: string, hasta: string): boolean {
    const fv = fechaVenta ? fechaVenta.slice(0, 10) : '';
    if (desde && (!fv || fv < desde)) return false;
    if (hasta && (!fv || fv > hasta)) return false;
    return true;
  }

  private rangoLabel(desde: string, hasta: string): string {
    if (desde && hasta) return `Del ${desde} al ${hasta}`;
    if (desde) return `Desde ${desde}`;
    if (hasta) return `Hasta ${hasta}`;
    return 'Todos los registros (sin filtro de fecha)';
  }

  generarReporteVentas() {
    // Se agrupa por boleta: un checkout con varios productos cuenta como una sola venta.
    const lista = agruparVentas(
      this.ventas.filter(
        (v) =>
          (this.repVentasCliente === 'Todos' || String(v.customerId) === this.repVentasCliente) &&
          this.enRango(v.fechaVenta, this.repVentasDesde, this.repVentasHasta),
      ),
    );
    const total = lista.reduce((a, g) => a + g.total, 0);
    this.modal = {
      titulo: 'Reporte de Ventas por Período',
      subtitulo: `${this.rangoLabel(this.repVentasDesde, this.repVentasHasta)} · Cliente: ${this.repVentasCliente}`,
      encabezados: ['N° Boleta', 'Cliente', 'Vendedor', 'Total S/'],
      filas: lista.map((g) => [
        g.numeroBoleta,
        g.customerId != null ? `Cliente ${g.customerId}` : '—',
        g.vendedor || '—',
        `S/ ${g.total.toFixed(2)}`,
      ]),
      totalLabel: `Total (${lista.length} boletas)`,
      totalValor: `S/ ${total.toFixed(2)}`,
    };
    this.cdr.detectChanges();
  }

  generarReporteProductos() {
    const porProducto = new Map<number, { cantidad: number; total: number }>();
    this.ventas
      .filter((v) => this.enRango(v.fechaVenta, this.repProdDesde, this.repProdHasta))
      .forEach((v) => {
        const key = v.productId ?? -1;
        const cur = porProducto.get(key) ?? { cantidad: 0, total: 0 };
        cur.cantidad += v.quantity ?? 0;
        cur.total += v.total ?? 0;
        porProducto.set(key, cur);
      });
    const filas: (string | number)[][] = [];
    let granTotal = 0;
    porProducto.forEach((val, pid) => {
      filas.push([
        pid === -1 ? '—' : this.nombreJoya(pid),
        val.cantidad,
        `S/ ${val.total.toFixed(2)}`,
      ]);
      granTotal += val.total;
    });
    this.modal = {
      titulo: 'Reporte de Productos Vendidos',
      subtitulo: this.rangoLabel(this.repProdDesde, this.repProdHasta),
      encabezados: ['Producto', 'Cantidad Vendida', 'Monto Total S/'],
      filas,
      totalLabel: `Total (${filas.length} productos)`,
      totalValor: `S/ ${granTotal.toFixed(2)}`,
    };
    this.cdr.detectChanges();
  }

  generarReporteInventario() {
    const valor = this.joyas.reduce((a, j) => a + (j.price ?? 0) * (j.stock ?? 0), 0);
    this.modal = {
      titulo: 'Reporte de Inventario',
      subtitulo: `${this.joyas.length} productos registrados`,
      encabezados: ['ID', 'Producto', 'Stock', 'Precio S/', 'Estado'],
      filas: this.joyas.map((j) => [
        j.id ?? '—',
        j.descripcion || j.name,
        j.stock ?? 0,
        `S/ ${(j.price ?? 0).toFixed(2)}`,
        j.estado || ((j.stock ?? 0) > 0 ? 'Disponible' : 'Vendido'),
      ]),
      totalLabel: 'Valor total del inventario',
      totalValor: `S/ ${valor.toFixed(2)}`,
    };
    this.cdr.detectChanges();
  }

  generarReporteVendedor() {
    const lista = agruparVentas(
      this.ventas.filter((v) => this.repVendedor === 'Todos' || v.vendedor === this.repVendedor),
    );
    const total = lista.reduce((a, g) => a + g.total, 0);
    this.modal = {
      titulo: 'Reporte de Ventas por Vendedor',
      subtitulo: `Vendedor: ${this.repVendedor}`,
      encabezados: ['N° Boleta', 'Cliente', 'Vendedor', 'Total S/'],
      filas: lista.map((g) => [
        g.numeroBoleta,
        g.customerId != null ? `Cliente ${g.customerId}` : '—',
        g.vendedor || '—',
        `S/ ${g.total.toFixed(2)}`,
      ]),
      totalLabel: `Total (${lista.length} boletas)`,
      totalValor: `S/ ${total.toFixed(2)}`,
    };
    this.cdr.detectChanges();
  }

  cerrarModal() {
    this.modal = null;
  }

  imprimir() {
    window.print();
  }
}
