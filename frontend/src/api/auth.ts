import { api } from './client';
import type { LoginResponse, UserInfo } from './types';

export const login = (username: string, password: string) =>
  api.post<LoginResponse>('/api/auth/login', { username, password }).then((r) => r.data);

export const fetchMe = () => api.get<UserInfo>('/api/auth/me').then((r) => r.data);
