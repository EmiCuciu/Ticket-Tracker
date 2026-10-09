import type {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  RegisterResponse,
} from '../types';
import { api } from './client';

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/auth/login', request);
  return data;
}

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  const { data } = await api.post<RegisterResponse>('/api/auth/register', request);
  return data;
}

export async function refresh(request: RefreshTokenRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/api/auth/refresh', request);
  return data;
}

export async function logout(request: RefreshTokenRequest): Promise<void> {
  await api.post('/api/auth/logout', request);
}

export async function logoutAll(): Promise<void> {
  await api.post('/api/auth/logout-all');
}