import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { listProjects } from "../api/projects";
import { listTickets, type TicketQueryParams } from "../api/tickets";
import TicketTable from "../components/TicketTable";
import Pagination from "../components/Pagination";
import { useState } from "react";
import { errorMessage } from "../api/client";
import { toIso } from "../utils/format";
import type { Priority, Status, SortableField, SortDirection } from "../types";

const STATUSES: (Status | "")[] = [
  "",
  "OPEN",
  "IN_PROGRESS",
  "DONE",
  "CANCELLED",
];
const PRIORITIES: (Priority | "")[] = ["", "LOW", "MEDIUM", "HIGH", "URGENT"];

export default function TicketsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [status, setStatus] = useState<Status | "">("");
  const [priority, setPriority] = useState<Priority | "">("");
  const [titleContains, setTitleContains] = useState("");
  const [sortBy, setSortBy] = useState<SortableField | "">("");
  const [sortDirection, setSortDirection] = useState<SortDirection | "">("");
  const [assigneeId, setAssigneeId] = useState("");
  const [dueAfter, setDueAfter] = useState("");
  const [dueBefore, setDueBefore] = useState("");

  useQuery({
    queryKey: ["projects"],
    queryFn: () => listProjects(0, 200),
  });

  const params: TicketQueryParams = {
    page,
    size,
    status: status || undefined,
    priority: priority || undefined,
    titleContains: titleContains || undefined,
    sortBy: sortBy || undefined,
    sortDirection: sortDirection || undefined,
    assigneeId: assigneeId || undefined,
    dueAfter: dueAfter ? (toIso(dueAfter) ?? undefined) : undefined,
    dueBefore: dueBefore ? (toIso(dueBefore) ?? undefined) : undefined,
  };

  const ticketsQuery = useQuery({
    queryKey: ["tickets", params],
    queryFn: () => listTickets(params),
  });

  const resetFilters = () => {
    setPage(0);
    setStatus("");
    setPriority("");
    setTitleContains("");
    setSortBy("");
    setSortDirection("");
    setAssigneeId("");
    setDueAfter("");
    setDueBefore("");
  };

  return (
    <div>
      <div className="page-header">
        <h1>Tickets</h1>
        <div className="page-actions">
          <button onClick={() => navigate("/tickets/new")}>New ticket</button>
        </div>
      </div>

      <div className="card filter-card">
        <div className="field-row">
          <div className="field">
            <label>Search title</label>
            <input
              value={titleContains}
              onChange={(e) => {
                setPage(0);
                setTitleContains(e.target.value);
              }}
              placeholder="contains..."
            />
          </div>
          <div className="field">
            <label>Status</label>
            <select
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value as Status | "");
              }}
            >
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s || "All"}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Priority</label>
            <select
              value={priority}
              onChange={(e) => {
                setPage(0);
                setPriority(e.target.value as Priority | "");
              }}
            >
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>
                  {p || "All"}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Sort by</label>
            <select
              value={sortBy}
              onChange={(e) => {
                setPage(0);
                setSortBy(e.target.value as SortableField | "");
              }}
            >
              <option value="">Default</option>
              <option value="createdAt">Created</option>
              <option value="dueDate">Due date</option>
              <option value="priority">Priority</option>
            </select>
          </div>
          <div className="field">
            <label>Direction</label>
            <select
              value={sortDirection}
              onChange={(e) => {
                setPage(0);
                setSortDirection(e.target.value as SortDirection | "");
              }}
            >
              <option value="">Auto</option>
              <option value="asc">Asc</option>
              <option value="desc">Desc</option>
            </select>
          </div>
          <div className="field">
            <label>Size</label>
            <select
              value={size}
              onChange={(e) => {
                setPage(0);
                setSize(Number(e.target.value));
              }}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </div>
        </div>
        <div className="field-row">
          <div className="field">
            <label>Assignee ID</label>
            <input
              value={assigneeId}
              onChange={(e) => {
                setPage(0);
                setAssigneeId(e.target.value);
              }}
              placeholder="uuid"
            />
          </div>
          <div className="field">
            <label>Due after</label>
            <input
              type="datetime-local"
              value={dueAfter}
              onChange={(e) => {
                setPage(0);
                setDueAfter(e.target.value);
              }}
            />
          </div>
          <div className="field">
            <label>Due before</label>
            <input
              type="datetime-local"
              value={dueBefore}
              onChange={(e) => {
                setPage(0);
                setDueBefore(e.target.value);
              }}
            />
          </div>
          <div className="field reset-field">
            <button type="button" className="secondary" onClick={resetFilters}>
              Reset
            </button>
          </div>
        </div>
      </div>

      {ticketsQuery.isLoading ? (
        <p>Loading tickets…</p>
      ) : ticketsQuery.isError ? (
        <div className="error-banner">{errorMessage(ticketsQuery.error)}</div>
      ) : ticketsQuery.data?.content?.length === 0 ? (
        <p className="empty">No tickets match the current filters.</p>
      ) : (
        <div>
          <TicketTable
            tickets={ticketsQuery.data!.content}
            onRowClick={(t) => navigate(`/tickets/${t.id}`)}
          />
          <Pagination
            page={ticketsQuery.data!.page}
            totalPages={ticketsQuery.data!.totalPages}
            totalElements={ticketsQuery.data!.totalElements}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
}
