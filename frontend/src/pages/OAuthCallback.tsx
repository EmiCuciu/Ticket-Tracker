import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setTokens } from '../api/tokenStore';

export default function OAuthCallback() {
  const navigate = useNavigate();
  const [message, setMessage] = useState('Completing sign-in…');

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (accessToken && refreshToken) {
      setTokens({ accessToken, refreshToken });
      navigate('/', { replace: true });
    } else {
      const googleError = params.get('error');
      setMessage(googleError ? `Sign-in failed: ${googleError}` : 'Sign-in failed. No tokens received.');
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [navigate]);

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <p>{message}</p>
        <button className="secondary" onClick={() => navigate('/login')}>
          Back to sign in
        </button>
      </div>
    </div>
  );
}