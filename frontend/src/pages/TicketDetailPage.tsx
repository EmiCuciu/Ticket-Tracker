import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { deleteTicket, getTicket, patchTicket, updateTicket } from '../api/tickets';
import { listComments, createComment } from '../api/comments';
import { listProjects } from '../api/projects';
import { errorMessage } from '../api/client';
import { formatDate, fromIso, toIso } from '../utils/format';
import { StatusBadge, PriorityBadge } from '../components/Badges';
import type { TicketDto, TicketPatchRequest, TicketPutRequest } from '../types';
import { useEffect, useState, type FormEvent } from 'react';

const STATUSES = ['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'] as const;
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;

type StatusOption = (typeof STATUSES)[number];
type PriorityOption = (typeof PRIORITIES)[number];

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<StatusOption>('OPEN');
  const [priority, setPriority] = useState<PriorityOption>('LOW');
  const [assigneeId, setAssigneeId] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [commentBody, setCommentBody] = useState('');
  const [error, setError] = useState<string | null>(null);

  const ticketQuery = useQuery({
    queryKey: ['ticket', id],
    queryFn: () => getTicket(id!),
    enabled: !!id,
  });

  const commentsQuery = useQuery({
    queryKey: ['comments', id],
    queryFn: () => listComments(id!, 0, 100),
    enabled: !!id,
  });

  const projectsQuery = useQuery({
    queryKey: ['projects'],
    queryFn: () => listProjects(0, 200),
  });

  useEffect(() => {
    if (ticketQuery.data) {
      setTitle(ticketQuery.data.title);
      setDescription(ticketQuery.data.description ?? '');
      setStatus(ticketQuery.data.status as StatusOption);
      setPriority(ticketQuery.data.priority as PriorityOption);
      setAssigneeId(ticketQuery.data.assignee?.id ?? '');
      setDueDate(fromIso(ticketQuery.data.dueDate));
    }
  }, [ticketQuery.data]);

  const updateMutation = useMutation({
    mutationFn: (payload: { put: TicketPutRequest; patch: TicketPatchRequest }) =>
      id ? updateTicket(id, payload.put) : Promise.reject(new Error('Missing id')),
    onSuccess: (updated) => {
      queryClient.setQueryData<TicketDto | undefined>(['ticket', id], updated);
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      setEditing(false);
      setError(null);
    },
    onError: (e) => setError(errorMessage(e)),
  });

  const patchStatusMutation = useMutation({
    mutationFn: (nextStatus: StatusOption) => {
      if (!id) return Promise.reject(new Error('Missing id'));
      return patchTicket(id, { status: nextStatus });
    },
    onSuccess: (updated) => {
      queryClient.setQueryData<TicketDto | undefined>(['ticket', id], updated);
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      setStatus(updated.status as StatusOption);
      setError(null);
    },
    onError: (e) => setError(errorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => {
      if (!id) return Promise.reject(new Error('Missing id'));
      return deleteTicket(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      navigate('/tickets');
    },
    onError: (e) => setError(errorMessage(e)),
  });

  const commentMutation = useMutation({
    mutationFn: () => {
      if (!id) return Promise.reject(new Error('Missing id'));
      return createComment(id, { body: commentBody.trim() });
    },
    onSuccess: () => {
      setCommentBody('');
      queryClient.invalidateQueries({ queryKey: ['comments', id] });
      setError(null);
    },
    onError: (e) => setError(errorMessage(e)),
  });

  if (!id) {
    return <div className="error-banner">Missing ticket ID.</div>;
  }

  if (ticketQuery.isLoading) {
    return <p>Loading ticket…</p>;
  }

  if (ticketQuery.isError || !ticketQuery.data) {
    return (
      <div>
        <div className="error-banner">Failed to load ticket.</div>
        <button className="secondary" onClick={() => ticketQuery.refetch()}>
          Retry
        </button>
      </div>
    );
  }

  const ticket = ticketQuery.data;
  projectsQuery.data?.content ?? [];

  const handleSave = (event: FormEvent) => {
    event.preventDefault();
    const putPayload: TicketPutRequest = {
      title: title.trim(),
      description: description || undefined,
      status: status as any,
      priority: priority as any,
      assigneeId: assigneeId ? assigneeId : null,
      dueDate: toIso(dueDate),
    };
    updateMutation.mutate({ put: putPayload, patch: {} });
  };

  const handleAddComment = (event: FormEvent) => {
    event.preventDefault();
    if (!commentBody.trim()) return;
    commentMutation.mutate();
  };

  const nextStatus = (current: StatusOption): StatusOption | null => {
    if (current === 'OPEN') return 'IN_PROGRESS';
    if (current === 'IN_PROGRESS') return 'DONE';
    if (current === 'DONE') return 'CANCELLED';
    return null;
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{ticket.title}</h1>
          <div style={{ display: 'flex', gap: '0.6rem', alignItems: 'center' }}>
            <StatusBadge status={ticket.status as any} />
            <PriorityBadge priority={ticket.priority as any} />
            <span className="empty">Updated {formatDate(ticket.updatedAt)}</span>
          </div>
        </div>
        <div className="page-actions">
          {nextStatus(status as StatusOption) && (
            <button
              onClick={() => patchStatusMutation.mutate(nextStatus(status as StatusOption)!)}
              disabled={patchStatusMutation.isPending}
            >
              Mark as {nextStatus(status as StatusOption)}
            </button>
          )}
          <button className="secondary" onClick={() => setEditing((v) => !v)}>
            {editing ? 'Cancel edit' : 'Edit'}
          </button>
          <button
            className="danger"
            disabled={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm('Delete this ticket?')) {
                deleteMutation.mutate();
              }
            }}
          >
            Delete
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {editing ? (
        <form className="card ticket-form" onSubmit={handleSave}>
          <div className="field">
            <label>Title</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>
          <div className="field">
            <label>Description</label>
            <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="field-row">
            <div className="field">
              <label>Status</label>
              <select value={status} onChange={(e) => setStatus(e.target.value as StatusOption)}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Priority</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value as PriorityOption)}>
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Project</label>
              <select value={ticket.project?.id ?? ''} disabled>
                {(projectsQuery.data?.content ?? []).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Assignee ID</label>
              <input value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)} placeholder="uuid" />
            </div>
          </div>
          <div className="field">
            <label>Due date</label>
            <input type="datetime-local" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>
          <div className="form-actions">
            <button type="submit" disabled={updateMutation.isPending}>
              {updateMutation.isPending ? 'Saving…' : 'Save'}
            </button>
            <button type="button" className="secondary" onClick={() => setEditing(false)}>
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <div className="card">
          <dl className="metadata">
            <dt>Project</dt>
            <dd>{ticket.project?.name ?? '—'}</dd>
            <dt>Assignee</dt>
            <dd>{ticket.assignee?.fullName ?? ticket.assignee?.email ?? '—'}</dd>
            <dt>Created at</dt>
            <dd>{formatDate(ticket.createdAt)}</dd>
            <dt>Due date</dt>
            <dd>{formatDate(ticket.dueDate)}</dd>
            <dt>Description</dt>
            <dd style={{ whiteSpace: 'pre-wrap' }}>{ticket.description || '—'}</dd>
          </dl>
        </div>
      )}

      <div className="card">
        <h2>Comments</h2>
        <form className="comment-form" onSubmit={handleAddComment} style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem', marginBottom: '1rem' }}>
          <textarea
            rows={3}
            value={commentBody}
            onChange={(e) => setCommentBody(e.target.value)}
            placeholder="Add a comment…"
          />
          <div className="form-actions">
            <button type="submit" disabled={commentMutation.isPending || !commentBody.trim()}>
              {commentMutation.isPending ? 'Posting…' : 'Post comment'}
            </button>
          </div>
        </form>
        {commentsQuery.isLoading ? (
          <p>Loading comments…</p>
        ) : !commentsQuery.data || commentsQuery.data.content.length === 0 ? (
          <p className="empty">No comments yet.</p>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
            {commentsQuery.data.content.map((comment) => (
              <li key={comment.id} className="card" style={{ padding: '0.8rem 1rem', marginBottom: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: '#94a3b8' }}>
                  <span>{comment.author?.fullName ?? 'Unknown'}</span>
                  <span>{formatDate(comment.createdAt)}</span>
                </div>
                <p style={{ whiteSpace: 'pre-wrap' }}>{comment.body}</p>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
