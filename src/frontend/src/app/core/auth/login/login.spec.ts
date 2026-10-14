import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../auth-service';
import { Login } from './login';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(async () => {
    // login() w serwisie zwraca Observable — mock musi to odzwierciedlać,
    // bo komponent wywołuje .subscribe() na wyniku
    mockAuthService = {
      login: vi.fn().mockReturnValue(of({ token: 'ey...AFTD', username: 'worker' })),
      saveToken: vi.fn(),
    };
    mockRouter = {
      navigate: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create login component', () => {
    expect(component).toBeTruthy();
  });

  it('should not call login service if form is invalid', () => {
    // setup
    const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
    // because .authError is protected convert to any to extract this function
    const componentAny = component as any;
    // act
    submitBtn.triggerEventHandler('click');
    // assert
    expect(mockAuthService.login).not.toHaveBeenCalled();
    expect(componentAny.authError()).toBe(null);
  });

  it('should call login service with login success', () => {
    // setup — mock zwraca Observable, żeby .subscribe() w komponencie zadziałało
    const mockRes = { token: 'ey....AFTD', username: 'worker' };
    mockAuthService.login.mockReturnValue(of(mockRes));
    const componentAny = component as any;
    //fill form
    const usernameInput = fixture.debugElement.query(By.css('[data-testid="username-input"]'));
    usernameInput.nativeElement.value = 'worker';
    usernameInput.nativeElement.dispatchEvent(new Event('input'));

    const passwordInput = fixture.debugElement.query(By.css('[data-testid="username-password"]'));
    passwordInput.nativeElement.value = 'Worker123!';
    passwordInput.nativeElement.dispatchEvent(new Event('input'));

    // act — onSubmit($event) jest przypięte do <form> (submit), nie do buttona,
    // więc triggerujemy submit na formularzu; event musi mieć preventDefault()
    const formEl = fixture.debugElement.query(By.css('form'));
    formEl.triggerEventHandler('submit', { preventDefault: () => {} });

    // assert — komponent woła saveToken(res.token, res.username) — DWA argumenty
    expect(mockAuthService.login).toHaveBeenCalledWith({
      username: 'worker',
      password: 'Worker123!',
    });
    expect(mockAuthService.saveToken).toHaveBeenCalledWith(mockRes.token, mockRes.username);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/products']);
    expect(componentAny.authError()).toBeNull();
  });

  it('should call login service with login error', async () => {
    // setup - return observable for subscribe
    mockAuthService.login.mockReturnValue(
      throwError(() => new Error('Invalid username or password')),
    );
    // authError is protected in service
    // fill form
    const usernameInput = fixture.debugElement.query(By.css('[data-testid="username-input"]'));
    usernameInput.nativeElement.value = 'user';
    usernameInput.nativeElement.dispatchEvent(new Event('input'));

    const passwordInput = fixture.debugElement.query(By.css('[data-testid="username-password"]'));
    passwordInput.nativeElement.value = 'User123!';
    passwordInput.nativeElement.dispatchEvent(new Event('input'));

    // act - form submit event
    const formElement = fixture.debugElement.query(By.css('form'));
    formElement.triggerEventHandler('submit', { preventDefault: () => {} });
    fixture.detectChanges();
    // assert
    const alertElement = fixture.nativeElement.querySelector('.auth-error');

    expect(mockAuthService.login).toHaveBeenCalledTimes(1);
    expect(alertElement).toBeTruthy();
    expect(alertElement.textContent).toContain('Invalid username or password');
  });

  it('should be trigger validation guard loginForm.invalid ', () => {
    // setup
    // fill form
    const usernameInput = fixture.debugElement.query(By.css('[data-testid="username-input"]'));
    usernameInput.nativeElement.value = '';
    usernameInput.nativeElement.dispatchEvent(new Event('input'));

    const passwordInput = fixture.debugElement.query(By.css('[data-testid="username-password"]'));
    passwordInput.nativeElement.value = '';
    passwordInput.nativeElement.dispatchEvent(new Event('input'));

    // act
    const formElement = fixture.debugElement.query(By.css('form'));
    formElement.triggerEventHandler('submit', { preventDefault: () => {} });
    fixture.detectChanges();
    // assert
    expect(mockAuthService.login).not.toHaveBeenCalled();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should be render submit button', () => {
    const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
    submitBtn.triggerEventHandler('click');
    expect(submitBtn).toBeTruthy();
  });
});
