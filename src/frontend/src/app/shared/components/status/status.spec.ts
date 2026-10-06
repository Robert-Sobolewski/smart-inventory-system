import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Status } from './status';

describe('Status', () => {
  let mockAuthService: any;

  beforeEach(async () => {

    mockAuthService = {
      isAuthenticated: signal(true),
      getUsername: ()=> "manager"
    }
    await TestBed.configureTestingModule({
      imports: [Status],
      providers: [provideRouter([]),
    {provide: AuthService, useValue:mockAuthService}
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
});
