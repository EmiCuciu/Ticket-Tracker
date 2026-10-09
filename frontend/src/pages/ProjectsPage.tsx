import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { createProject, deleteProject, listProjects } from '../api/projects';
import { errorMessage } from '../api/client';
import { formatDate } from '../utils/format';

export default function ProjectsPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['projects'],
    queryFn: () => listProjects(0, 100),
  });

  const createMutation = useMutation({
    mutationFn: () => createProject({ name, description: description || undefined }),
    onSuccess: () => {
      setName('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
    onError: (e) => setError(errorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteProject(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
    onError: (e) => setError(errorMessage(e)),
  });

  const handleCreate = (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    createMutation.mutate();
  };

  const handleDelete = (id: string, nameToDelete: string) => {
    if (window.confirm(`Delete project "${nameToDelete}"?`)) {
      setError(null);
      deleteMutation.mutate(id);
    }
  };

  if (isError) {
    return (
      <div>
        <div className="error-banner">Failed to load projects.</div>
        <button className="secondary" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div>
      <h1>Projects</h1>

      <form className="card project-form" onSubmit={handleCreate}>
        <h2>Create project</h2>
        {error && <div className="error-banner">{error}</div>}
        <div className="field">
          <label htmlFor="project-name">Name</label>
          <input
            id="project-name"
            required
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="project-description">Description</label>
          <textarea
            id="project-description"
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
        <button type="submit" disabled={createMutation.isPending}>
          {createMutation.isPending ? 'Creating…' : 'Create'}
        </button>
      </form>

      {isLoading ? (
        <p>Loading projects…</p>
      ) : !data || data.content.length === 0 ? (
        <p className="empty">No projects yet.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
              <th>Created by</th>
              <th>Created at</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {data.content.map((project) => (
              <tr key={project.id}>
                <td>
                  <Link to={`/projects/${project.id}`}>{project.name}</Link>
                </td>
                <td>{project.description ?? '—'}</td>
                <td>{project.createdBy?.fullName ?? '—'}</td>
                <td>{formatDate(project.createdAt)}</td>
                <td>
                  <button
                    className="danger"
                    disabled={deleteMutation.isPending}
                    onClick={() => handleDelete(project.id, project.name)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}