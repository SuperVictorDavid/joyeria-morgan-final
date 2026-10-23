// Ambiente de PRODUCCION (ng build).
// El frontend se sirve por nginx y este proxya /api hacia el API Gateway,
// por eso el bundle no lleva hosts ni puertos de los microservicios.
export const environment = {
  production: true,
  apiUrl: '/api',
  authUrl: '/api',
  inventarioUrl: '/api',
  ventasUrl: '/api',
  clientesUrl: '/api',
  reportesUrl: '/api',
  gatewayUrl: '/api',
};
