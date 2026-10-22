export const environment = {
  production: false,
  // Directo (dev actual):
  apiUrl: 'http://localhost:8083',
  authUrl: 'http://localhost:8083',
  inventarioUrl: 'http://localhost:8081',
  ventasUrl: 'http://localhost:8082',
  clientesUrl: 'http://localhost:8084',
  reportesUrl: 'http://localhost:8085',
  // Via Gateway (para demo T1 Spring Cloud, cambia a esto si quieres pasar por 8762):
  // apiUrl: 'http://localhost:8762',
  // authUrl: 'http://localhost:8762',
  // inventarioUrl: 'http://localhost:8762',
  // ventasUrl: 'http://localhost:8762',
  gatewayUrl: 'http://localhost:8762',
};
