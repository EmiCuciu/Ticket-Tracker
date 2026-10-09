interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements?: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return totalElements != null ? (
      <div className="pagination">
        <span>{totalElements} item(s)</span>
      </div>
    ) : null;
  }

  return (
    <div className="pagination">
      <button disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
        {totalElements != null ? ` · ${totalElements} item(s)` : ''}
      </span>
      <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
        Next
      </button>
    </div>
  );
}