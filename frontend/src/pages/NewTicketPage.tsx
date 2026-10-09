import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { createTicket } from '../api/tickets';
import { listProjects } from '../api/projects';
import TicketForm, { type TicketFormValues } from '../components/TicketForm';
import { toIso } from '../utils/format';
import { errorMessage } from '../api/client';

export default function NewTicketPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: () => listProjects(0, 200),
  });

  const mutation = useMutation({
    mutationFn: async (values: TicketFormValues) => {
      return createTicket({
        title: values.title,
        description: values.description || undefined,
        status: values.status,
        priority: values.priority,
        projectId: values.projectId,
        assigneeId: values.assigneeId ? values.assigneeId : null,
        dueDate: toIso(values.dueDate),
      });
    },
    onSuccess: (ticket) => {
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      navigate(`/tickets/${ticket.id}`);
    },
  });

  const handleSubmit = async (values: TicketFormValues) => {
    await mutation.mutateAsync(values);
  };

  if (projectsQuery.isLoading) {
    return <p>Loading projects…</p>;
  }

  if (projectsQuery.isError || !projectsQuery.data) {
    return <div className="error-banner">Failed to load projects.</div>;
  }

  return (
    <div>
      <h1>Create ticket</h1>
      <TicketForm
        projects={projectsQuery.data.content}
        submitLabel="Create"
        onSubmit={handleSubmit}
        onCancel={() => navigate('/tickets')}
      />
      {mutation.isError && <div className="error-banner">{errorMessage(mutation.error)}</div>}
    </div>
  );
}