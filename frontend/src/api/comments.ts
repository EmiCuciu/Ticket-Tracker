import type { CommentCreateRequest, CommentDto, Page } from '../types';
import { api } from './client';

export async function listComments(ticketId: string, page = 0, size = 50): Promise<Page<CommentDto>> {
  const { data } = await api.get<Page<CommentDto>>(`/api/tickets/${ticketId}/comments`, {
    params: { page, size },
  });
  return data;
}

export async function createComment(ticketId: string, request: CommentCreateRequest): Promise<CommentDto> {
  const { data } = await api.post<CommentDto>(`/api/tickets/${ticketId}/comments`, request);
  return data;
}