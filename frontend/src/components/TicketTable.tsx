import type { TicketDto } from '../types';
import { formatDate } from '../utils/format';
import { PriorityBadge, StatusBadge } from './Badges';

interface TicketTableProps {
  tickets: TicketDto[];
  onRowClick: (ticket: TicketDto) => void;
}

export default function TicketTable({ tickets, onRowClick }: TicketTableProps) {
  if (tickets.length === 0) {
    return <p className="empty">No tickets found.</p>;
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Project</th>
          <th>Status</th>
          <th>Priority</th>
          <th>Assignee</th>
          <th>Due date</th>
          <th>Updated</th>
        </tr>
      </thead>
      <tbody>
        {tickets.map((ticket) => (
          <tr key={ticket.id} onClick={() => onRowClick(ticket)} className="clickable">
            <td>{ticket.title}</td>
            <td>{ticket.project?.name ?? '—'}</td>
            <td>
              <StatusBadge status={ticket.status} />
            </td>
            <td>
              <PriorityBadge priority={ticket.priority} />
            </td>
            <td>{ticket.assignee?.fullName ?? '—'}</td>
            <td>{formatDate(ticket.dueDate)}</td>
            <td>{formatDate(ticket.updatedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}