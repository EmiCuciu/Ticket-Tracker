import { useState, type FormEvent } from 'react';
import type { Priority, ProjectDto, Status } from '../types';

export interface TicketFormValues {
  title: string;
  description: string;
  status: Status;
  priority: Priority;
  projectId: string;
  assigneeId: string;
  dueDate: string;
}

export interface TicketFormProps {
  projects: ProjectDto[];
  initial?: Partial<TicketFormValues>;
  submitLabel: string;
  onSubmit: (values: TicketFormValues) => Promise<void>;
  onCancel?: () => void;
}

const STATUSES: Status[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const EMPTY: TicketFormValues = {
  title: '',
  description: '',
  status: 'OPEN',
  priority: 'LOW',
  projectId: '',
  assigneeId: '',
  dueDate: '',
};

export default function TicketForm({
  projects,
  initial,
  submitLabel,
  onSubmit,
  onCancel,
}: TicketFormProps) {
  const [values, setValues] = useState<TicketFormValues>({ ...EMPTY, ...initial });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const set = <K extends keyof TicketFormValues>(key: K, value: TicketFormValues[K]) => {
    setValues((current) => ({ ...current, [key]: value }));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit(values);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Request failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="card ticket-form" onSubmit={handleSubmit}>
      {error && <div className="error-banner">{error}</div>}
      <div className="field">
        <label htmlFor="ticket-title">Title</label>
        <input
          id="ticket-title"
          required
          maxLength={100}
          value={values.title}
          onChange={(e) => set('title', e.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor="ticket-description">Description</label>
        <textarea
          id="ticket-description"
          rows={3}
          value={values.description}
          onChange={(e) => set('description', e.target.value)}
        />
      </div>
      <div className="field-row">
        <div className="field">
          <label htmlFor="ticket-status">Status</label>
          <select
            id="ticket-status"
            value={values.status}
            onChange={(e) => set('status', e.target.value as Status)}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="ticket-priority">Priority</label>
          <select
            id="ticket-priority"
            value={values.priority}
            onChange={(e) => set('priority', e.target.value as Priority)}
          >
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="field-row">
        <div className="field">
          <label htmlFor="ticket-project">Project</label>
          <select
            id="ticket-project"
            required
            value={values.projectId}
            onChange={(e) => set('projectId', e.target.value)}
          >
            <option value="">Select a project…</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="ticket-assignee">Assignee ID (optional)</label>
          <input
            id="ticket-assignee"
            placeholder="UUID"
            value={values.assigneeId}
            onChange={(e) => set('assigneeId', e.target.value)}
          />
        </div>
      </div>
      <div className="field">
        <label htmlFor="ticket-due-date">Due date</label>
        <input
          id="ticket-due-date"
          type="datetime-local"
          value={values.dueDate}
          onChange={(e) => set('dueDate', e.target.value)}
        />
      </div>
      <div className="form-actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}