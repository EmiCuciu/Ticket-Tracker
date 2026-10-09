export type Status = 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type Role = 'ADMIN' | 'PROJECT_MANAGER' | 'MEMBER';
export type SortableField = 'dueDate' | 'priority' | 'createdAt';
export type SortDirection = 'asc' | 'desc';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserSummaryDto {
  id: string;
  fullName?: string;
  email?: string;
}

export interface ProjectSummaryDto {
  id: string;
  name?: string;
}

export interface ProjectDto {
  id: string;
  name: string;
  description?: string;
  createdBy?: UserSummaryDto;
  createdAt?: string;
}

export interface ProjectCreateRequest {
  name: string;
  description?: string;
}

export interface TicketDto {
  id: string;
  title: string;
  description?: string;
  status: Status;
  priority: Priority;
  project?: ProjectSummaryDto;
  assignee?: UserSummaryDto;
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TicketCreateRequest {
  title: string;
  description?: string;
  status: Status;
  priority: Priority;
  projectId: string;
  assigneeId?: string | null;
  dueDate?: string | null;
}

export interface TicketPutRequest {
  title: string;
  description?: string;
  status: Status;
  priority: Priority;
  assigneeId?: string | null;
  dueDate?: string | null;
}

export interface TicketPatchRequest {
  title?: string | null;
  description?: string | null;
  status?: Status | null;
  priority?: Priority | null;
  assigneeId?: string | null;
  dueDate?: string | null;
}

export interface CommentDto {
  id: string;
  ticketId?: string;
  author?: UserSummaryDto;
  body: string;
  createdAt?: string;
}

export interface CommentCreateRequest {
  body: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  fullName?: string;
  role?: Role;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface ErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
}