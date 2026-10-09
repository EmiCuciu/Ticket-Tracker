import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { runTicketDigest } from '../api/admin';
import { errorMessage } from '../api/client';

export default function Layout() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const digestMutation = useMutation({
      mutationFn: runTicketDigest,
      onSuccess: () => {
            window.open('http://localhost:8025', '_blank');
          },
      onError: (e) => alert('Error on email job: ' + errorMessage(e)),
    });

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
      <div className="layout">
        <header className="topbar">
          <Link to="/tickets" className="brand">
            Ticket Tracker
          </Link>
          <nav className="nav">
            <Link to="/tickets">Tickets</Link>
            <Link to="/projects">Projects</Link>
          </nav>
          <div className="topbar-right">
            {isAuthenticated && (
              <>
                <button 
                  className="secondary" 
                  onClick={() => digestMutation.mutate()} 
                  disabled={digestMutation.isPending}
                >
                  {digestMutation.isPending ? 'Pending...' : 'Test Email Digest'}
                </button>
                <button className="secondary" onClick={handleLogout}>
                  Logout
                </button>
              </>
            )}
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    );
}