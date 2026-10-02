import { HttpInterceptor, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../auth/auth-service";

export const authInterceptor: HttpInterceptorFn = (req, next) =>{

  // inject AuthService to get token
  const authService = inject(AuthService);
  const token = authService.getToken();

  // if token exist, clone request and add Authorization to header
  if(token){
    const authReq = req.clone({
      setHeaders:{
        Authorization: `Bearer ${token}`
      }
    });
    // pass updated request
    return next(authReq);
  }
  // else pass request
  return next(req)
}
