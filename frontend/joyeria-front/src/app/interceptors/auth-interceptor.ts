import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const token: string | null = authService.getToken();

  // Si la petición es hacia el endpoint de login, dejar pasar sin token
  if (req.url.includes('/usuarios/login') || req.url.includes('/auth/login')) {
    return next(req);
  }

  // Si existe un token pero ya venció antes de enviar la petición
  if (token && authService.isTokenExpired(token)) {
    authService.handleSessionExpired();
    return throwError(() => new Error('Token expirado'));
  }

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 Unauthorized o 403 Forbidden (común cuando el token expira o es inválido en backend)
      if (error.status === 401 || error.status === 403) {
        authService.handleSessionExpired();
      }
      return throwError(() => error);
    }),
  );
};
