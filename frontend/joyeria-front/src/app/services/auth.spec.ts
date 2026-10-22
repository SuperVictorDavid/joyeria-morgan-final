import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth';
import { provideHttpClient } from '@angular/common/http';

describe('Auth', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient()],
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
