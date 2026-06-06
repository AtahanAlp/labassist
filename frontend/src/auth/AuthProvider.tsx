import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { setUnauthorizedHandler, tokenStore } from '../api/client';
import { login as apiLogin } from '../api/auth';
import type { UserInfo } from '../api/types';
import { AuthContext } from './useAuth';

const USER_KEY = 'labassist_user';

function loadStoredUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as UserInfo) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(loadStoredUser);

  const clearSession = useCallback(() => {
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // When any request gets a 401, drop the session so routing falls back to login.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      localStorage.removeItem(USER_KEY);
      setUser(null);
    });
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const response = await apiLogin(username, password);
    tokenStore.set(response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    setUser(response.user);
  }, []);

  const value = useMemo(() => ({ user, login, logout: clearSession }), [user, login, clearSession]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
