export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn?: number;
}

const ACCESS_KEY = 'tt.accessToken';
const REFRESH_KEY = 'tt.refreshToken';
const EXPIRY_KEY = 'tt.accessExpiresAt';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

export function setTokens(tokens: AuthTokens): void {
  localStorage.setItem(ACCESS_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  if (tokens.expiresIn != null) {
    localStorage.setItem(EXPIRY_KEY, String(Date.now() + tokens.expiresIn * 1000));
  } else {
    localStorage.removeItem(EXPIRY_KEY);
  }
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(EXPIRY_KEY);
}