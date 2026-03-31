import http from './http';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  password: string;
  email: string;
}

export function loginApi(data: LoginPayload) {
  return http.post('/auth/login', data);
}

export function registerApi(data: RegisterPayload) {
  return http.post('/auth/register', data);
}

