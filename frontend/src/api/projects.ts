import type { Page, ProjectCreateRequest, ProjectDto } from '../types';
import { api } from './client';

export async function listProjects(page = 0, size = 50): Promise<Page<ProjectDto>> {
  const { data } = await api.get<Page<ProjectDto>>('/api/projects', {
    params: { page, size },
  });
  return data;
}

export async function getProject(id: string): Promise<ProjectDto> {
  const { data } = await api.get<ProjectDto>(`/api/projects/${id}`);
  return data;
}

export async function createProject(request: ProjectCreateRequest): Promise<ProjectDto> {
  const { data } = await api.post<ProjectDto>('/api/projects', request);
  return data;
}

export async function deleteProject(id: string): Promise<void> {
  await api.delete(`/api/projects/${id}`);
}