import { HttpErrorResponse } from '@angular/common/http';

/**
 * Traduce un error HTTP del backend a un mensaje entendible.
 * El backend responde JSON con "message" (server.error.include-message: always),
 * por eso mostramos ese motivo en vez de un texto genérico.
 */
export function mensajeErrorApi(err: unknown, contexto: string): string {
  const response = err as HttpErrorResponse;
  const status = response?.status ?? 0;
  const body: any = response?.error;
  const detalle = typeof body === 'string' ? body : (body?.message || body?.error || '');

  switch (status) {
    case 0:
      return `No se pudo conectar con ${contexto}. Verifica que el servicio esté arriba.`;
    case 400:
      return detalle || 'Datos inválidos.';
    case 401:
      return 'Sesión expirada o token inválido: vuelve a iniciar sesión.';
    case 403:
      return 'No tienes permisos para esta acción.';
    case 404:
      return detalle || 'El registro ya no existe.';
    case 409:
      return detalle || 'Stock insuficiente para la cantidad solicitada.';
    case 503:
      return detalle || 'Servicio no disponible (Kafka/Rabbit/inventario).';
    default:
      return detalle || `Error ${status} en ${contexto}.`;
  }
}
