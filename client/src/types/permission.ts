export interface AuthUser {
  id: number;
  username: string;
  fullName: string;
}

export interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  permissions: string[];
}
