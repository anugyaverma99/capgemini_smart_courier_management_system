export interface SignupRequest {
  fullName: string;
  email: string;
  password: string;
  phone: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
  userId: number;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  active: boolean;
}