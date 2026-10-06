import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Status } from './status';

describe('Status', () => {
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(async () => {
    // vi.fn() tworzy SPY — zapisuje każde wywołanie, więc możemy na nie assertować.
    // Zwykła strzałka (logout: () => {...}) niczego NIE zapisuje — dlatego
    // .toHaveBeenCalled() rzucał "is not a spy or a call to a spy".
    mockAuthService = {
      isAuthenticated: signal(true),
      getUsername: () => 'manager',
      logout: vi.fn(),
    };
    mockRouter = {
      navigate: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [Status],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(Status);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should show username and logout button when user is authenticated',()=>{
    const fixture = TestBed.createComponent(Status)
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const h3 = compiled.querySelector('h3');
    const button = compiled.querySelector('button[type="button"]');
    expect(h3).toBeTruthy();
    expect(button).toBeTruthy();

    expect(h3?.textContent).toContain("manager")
    expect(button?.textContent).toContain("Logout");
  })

  it('should call authService.logout() and router.navigate("/login")', () => {
    const fixture = TestBed.createComponent(Status);
    const component = fixture.componentInstance;
    component.onLogout();

    // Oba są teraz spy'ami, więc możemy assertować na ich wywołaniach.
    expect(mockAuthService.logout).toHaveBeenCalledTimes(1);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
  });
});
