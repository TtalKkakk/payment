import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/axios';

export default function Credentials() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ businessNumber: '', password: '' });
  const [creds, setCreds] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState('');

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/merchants/credentials', form);
      setCreds(res.data);
      localStorage.setItem('apiKey', res.data.apiKey);
      localStorage.setItem('apiSecret', res.data.apiSecret);
    } catch (err) {
      setError(err.response?.data?.message || 'API 키 발급에 실패했습니다. 승인된 가맹점인지 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text, key) => {
    navigator.clipboard.writeText(text);
    setCopied(key);
    setTimeout(() => setCopied(''), 2000);
  };

  return (
    <>
      <header className="app-header">
        <div className="app-header-inner">
          <span className="app-logo">배민<span>페이</span></span>
          <nav style={{ display: 'flex', gap: '4px', marginLeft: 'auto' }}>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/')}>가맹점 신청</button>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/status')}>신청 상태</button>
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/dashboard')}>대시보드</button>
          </nav>
        </div>
      </header>

      <div className="page">
        <h1 className="page-title">API 키 발급</h1>

        <div className="card">
          <p className="card-title">🔑 가맹점 인증</p>
          <p style={{ fontSize: '14px', color: '#8B95A1', marginBottom: '20px' }}>
            승인된 가맹점의 사업자번호와 비밀번호를 입력하면 API 키를 발급합니다.
          </p>

          {error && <div className="alert alert-error">⚠️ {error}</div>}

          <form className="form" onSubmit={handleSubmit} style={{ maxWidth: '480px' }}>
            <div className="form-group">
              <label className="form-label">사업자번호</label>
              <input className="form-input" name="businessNumber" value={form.businessNumber}
                onChange={handleChange} placeholder="000-00-00000" required />
            </div>
            <div className="form-group">
              <label className="form-label">비밀번호</label>
              <input className="form-input" name="password" type="password" value={form.password}
                onChange={handleChange} placeholder="신청 시 입력한 비밀번호" required />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '발급 중...' : 'API 키 발급'}
            </button>
          </form>
        </div>

        {creds && (
          <div className="card">
            <p className="card-title">✅ API 키 발급 완료</p>
            <div className="alert alert-info" style={{ marginBottom: '20px' }}>
              ⚠️ API Secret은 외부에 노출되지 않도록 안전하게 보관하세요.
            </div>

            {[
              { label: 'API Key', value: creds.apiKey, key: 'apiKey' },
              { label: 'API Secret', value: creds.apiSecret, key: 'apiSecret' },
            ].map(({ label, value, key }) => (
              <div key={key} style={{ marginBottom: '16px' }}>
                <p style={{ fontSize: '13px', fontWeight: 700, color: '#8B95A1', marginBottom: '8px' }}>{label}</p>
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                  <code style={{
                    flex: 1, background: '#F2F4F6', padding: '12px 16px',
                    borderRadius: '8px', fontSize: '13px', fontFamily: 'monospace',
                    wordBreak: 'break-all', color: '#191F28', border: '1px solid #E5E8EB'
                  }}>
                    {value}
                  </code>
                  <button
                    className="btn btn-outline btn-sm"
                    onClick={() => copyToClipboard(value, key)}
                    style={{ whiteSpace: 'nowrap' }}
                  >
                    {copied === key ? '복사됨 ✓' : '복사'}
                  </button>
                </div>
              </div>
            ))}

            <hr className="divider" />
            <button className="btn btn-primary btn-full" onClick={() => navigate('/dashboard')}>
              대시보드로 이동 →
            </button>
          </div>
        )}
      </div>
    </>
  );
}
