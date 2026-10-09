import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteProject, getProject } from '../api/projects';
import { formatDate } from '../utils/format';
import { errorMessage } from '../api/client';

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['project', id],
    queryFn: () => getProject(id!),
    enabled: !!id,
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteProject(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate('/projects');
    },
  });

  if (!id) {
    return <div className="error-banner">Missing project ID.</div>;
  }

  if (isLoading) {
    return <p>Loading project…</p>;
  }

  if (isError || !data) {
    return (
      <div>
        <div className="error-banner">Failed to load project.</div>
        <button className="secondary" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{data.name}</h1>
          <p>{data.description ?? 'No description'}</p>
        </div>
        <div className="page-actions">
          <button
            className="danger"
            disabled={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm('Delete this project?')) {
                deleteMutation.mutate();
              }
            }}
          >
            {deleteMutation.isPending ? 'Deleting…' : 'Delete project'}
          </button>
        </div>
      </div>

      <div className="card">
        <dl className="metadata">
          <dt>Created by</dt>
          <dd>{data.createdBy?.fullName ?? '—'}</dd>
          <dt>Created at</dt>
          <dd>{formatDate(data.createdAt)}</dd>
        </dl>
      </div>

      <p>
        <Link to="/tickets">View tickets</Link> or <Link to="/projects">Back to projects</Link>
      </p>

      {deleteMutation.isError && (
        <div className="error-banner">{errorMessage(deleteMutation.error)}</div>
      )}
    </div>
  );
}