import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JoyaService, Joya } from '../../services/joya';
import { CarritoStore } from '../../services/carrito-store';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inventario.html',
  styleUrl: './inventario.css',
})
export class InventarioComponent implements OnInit {
  joyas: Joya[] = [];
  filtro = '';
  filtroTipo = 'Todos';
  filtroMaterial = 'Todos';
  filtroEstado = 'Todos';

  isModalOpen = false;
  isEditMode = false;
  isViewOpen = false;
  verJoya: Joya | null = null;

  nueva: Joya = { name: '', price: 0, stock: 0, tipoProducto: 'Anillo', tipoMaterial: 'Oro', estado: 'Disponible' };

  tipos = ['Anillo', 'Cadena', 'Pulsera', 'Aretes', 'Dije'];
  materiales = ['Oro', 'Plata', 'Platino'];
  estados = ['Disponible', 'Vendido', 'Reservado'];

  // Paginación: 6 por página (19 joyas = 4 págs, 11 clientes = 2 págs)
  pagina = 1;
  porPagina = 6;

  constructor(private joyaService: JoyaService, private toast: ToastService, private cdr: ChangeDetectorRef, private carritoStore: CarritoStore, private router: Router, public auth: AuthService) {}

  ngOnInit(): void { this.cargar(); }

  cargar() {
    this.joyaService.listar().subscribe({
      next: (d) => { this.joyas = d ?? []; this.cdr.detectChanges(); },
      error: () => this.toast.error('Error al cargar inventario'),
    });
  }

  get filtradas(): Joya[] {
    const f = this.filtro.trim().toLowerCase();
    return this.joyas.filter((j) => {
      const matchTexto = !f ||
        (j.descripcion ?? '').toLowerCase().includes(f) ||
        (j.name ?? '').toLowerCase().includes(f) ||
        String(j.id ?? '').toLowerCase().includes(f);
      const matchTipo = this.filtroTipo === 'Todos' || (j.tipoProducto ?? '') === this.filtroTipo;
      const matchMaterial = this.filtroMaterial === 'Todos' || (j.tipoMaterial ?? '') === this.filtroMaterial;
      const matchEstado = this.filtroEstado === 'Todos' || this.estadoDe(j) === this.filtroEstado;
      return matchTexto && matchTipo && matchMaterial && matchEstado;
    });
  }

  estadoDe(j: Joya): string {
    if (j.estado && j.estado.trim()) return j.estado;
    return (j.stock ?? 0) > 0 ? 'Disponible' : 'Vendido';
  }

  get total(): number { return this.joyas.length; }
  get disponibles(): number { return this.joyas.filter((j) => (j.stock ?? 0) > 0).length; }
  get agotados(): number { return this.joyas.filter((j) => (j.stock ?? 0) === 0).length; }
  get valor(): number { return this.joyas.reduce((a, j) => a + (j.price ?? 0) * (j.stock ?? 0), 0); }

  // ===== Paginación =====
  get totalPaginas(): number { return Math.max(1, Math.ceil(this.filtradas.length / this.porPagina)); }
  get paginas(): number[] { return Array.from({ length: this.totalPaginas }, (_, i) => i + 1); }
  get paginadas(): Joya[] {
    const ini = (this.pagina - 1) * this.porPagina;
    return this.filtradas.slice(ini, ini + this.porPagina);
  }
  irA(p: number) { this.pagina = Math.min(Math.max(1, p), this.totalPaginas); }
  anterior() { this.irA(this.pagina - 1); }
  siguiente() { this.irA(this.pagina + 1); }

  limpiarFiltros() {
    this.filtro = '';
    this.filtroTipo = 'Todos';
    this.filtroMaterial = 'Todos';
    this.filtroEstado = 'Todos';
    this.pagina = 1;
  }

  abrir(j?: Joya) {
    this.isModalOpen = true;
    if (j) { this.isEditMode = true; this.nueva = { ...j }; }
    else { this.isEditMode = false; this.nueva = { name: '', price: 0, stock: 0, tipoProducto: 'Anillo', tipoMaterial: 'Oro', estado: 'Disponible' }; }
  }
  cerrar() { this.isModalOpen = false; }

  ver(j: Joya) { this.verJoya = { ...j }; this.isViewOpen = true; }
  cerrarVer() { this.isViewOpen = false; this.verJoya = null; }

  agregarAlCarrito(j: Joya) {
    if ((j.stock ?? 0) <= 0) { this.toast.warning('Sin stock disponible'); return; }
    this.carritoStore.add(j, 1);
    this.toast.success('Producto agregado al carrito');
    this.router.navigate(['/carrito']);
  }

  guardar() {
    const descripcion = (this.nueva.descripcion ?? this.nueva.name ?? '').trim();
    if (!descripcion || !(this.nueva.price > 0)) { this.toast.warning('Descripción y precio son obligatorios'); return; }
    // Mantener name sincronizado con descripcion para compatibilidad con el backend
    this.nueva.name = this.nueva.name?.trim() ? this.nueva.name : descripcion;
    this.nueva.descripcion = descripcion;
    if (this.isEditMode && this.nueva.id != null) {
      this.joyaService.actualizar(this.nueva.id, this.nueva).subscribe({ next: () => { this.toast.success('Producto actualizado'); this.cargar(); this.cerrar(); }, error: () => this.toast.error('Error al actualizar') });
    } else {
      this.joyaService.crear(this.nueva).subscribe({ next: () => { this.toast.success('Producto registrado'); this.cargar(); this.cerrar(); }, error: () => this.toast.error('Error al registrar') });
    }
  }

  eliminar(id?: number) {
    if (id == null) return;
    if (confirm('¿Eliminar producto?')) {
      this.joyaService.eliminar(id).subscribe({ next: () => { this.toast.success('Eliminado'); this.cargar(); }, error: () => this.toast.error('No se pudo eliminar') });
    }
  }
}
