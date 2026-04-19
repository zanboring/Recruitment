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

export interface UserVO {
  id: number;
  username: string;
  role: string;
  email: string;
  token: string;
}

export function loginApi(data: LoginPayload): Promise<UserVO> {
  return http.post('/auth/login', data);
}

export function registerApi(data: RegisterPayload): Promise<void> {
  return http.post('/auth/register', data);
}

