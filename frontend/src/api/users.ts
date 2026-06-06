import { api } from './client';
import type { UserInfo, UserRole } from './types';

export interface CreateUserPayload {
  username: string;
  password: string;
  displayName?: string;
  role: UserRole;
}

export const listUsers = () => api.get<UserInfo[]>('/api/users').then((r) => r.data);

export const createUser = (payload: CreateUserPayload) =>
  api.post<UserInfo>('/api/users', payload).then((r) => r.data);
