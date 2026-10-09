import { api } from './client';

export async function runTicketDigest(): Promise<void> {
  await api.post('/api/admin/jobs/ticket-digest/run');
}