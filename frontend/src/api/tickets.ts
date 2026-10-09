import type {
  Page,
  Priority,
  SortableField,
  SortDirection,
  Status,
  TicketCreateRequest,
  TicketDto,
  TicketPatchRequest,
  TicketPutRequest,
} from '../types';
import { api } from './client';

export interface TicketQueryParams {
  status?: Status;
  priority?: Priority;
  assigneeId?: string;
  dueBefore?: string;
  dueAfter?: string;
  titleContains?: string;
  page?: number;
  size?: number;
  sortBy?: SortableField;
  sortDirection?: SortDirection;
}

export async function listTickets(params: TicketQueryParams): Promise<Page<TicketDto>> {
  const { data } = await api.get<Page<TicketDto>>('/api/tickets', { params });
  return data;
}

export async function getTicket(id: string): Promise<TicketDto> {
  const { data } = await api.get<TicketDto>(`/api/tickets/${id}`);
  return data;
}

export async function createTicket(request: TicketCreateRequest): Promise<TicketDto> {
  const { data } = await api.post<TicketDto>('/api/tickets', request);
  return data;
}

export async function updateTicket(id: string, request: TicketPutRequest): Promise<TicketDto> {
  const { data } = await api.put<TicketDto>(`/api/tickets/${id}`, request);
  return data;
}

export async function patchTicket(id: string, request: TicketPatchRequest): Promise<TicketDto> {
  const { data } = await api.patch<TicketDto>(`/api/tickets/${id}`, request);
  return data;
}

export async function deleteTicket(id: string): Promise<void> {
  await api.delete(`/api/tickets/${id}`);
}