import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/axios';

const STATUS_CONFIG = {
  PENDING:          { label: '심사 중',    badge: 'badge-pending',  icon: '⏳' },
  APPROVED:         { label: '승인됨',     badge: 'badge-approved', icon: '✅' },
  REJECTED:         { label: '거절됨',     badge: 'badge-rejected', icon: '❌' },
  CANCELLED:        { label: '취소됨',     badge: 'badge-rejected', icon: '🚫' },
  SUBSCRIPTION_ENDED: { label: '구독 종료', badge: 'badge-rejected', icon: '⛔' },
};

export default function Status() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ businessNumber: '', password: '' });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/merchant-applications/business-number', form);
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || '조회에 실패했습니다. 사업자번호와 비밀번호를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const cfg = result ? (STATUS_CONFIG[result.status] ?? { label: result.status, badge: 'badge-pending', icon: '❓' }) : null;

  return (
    <>
      <header className="app-header">
        <div className="app-header-inner">
          <span className="app-logo">배민<span>페이</span></span>
          <nav style={{ display: 'flex', gap: '4px', marginLeft: 'auto' }}>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/')}>가맹점 신청</button>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/credentials')}>API 키</button>
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/dashboard')}>대시보드</button>
          </nav>
        </div>
      </header>

      <div className="page">
        <h1 className="page-title">신청 상태 조회</h1>

        <div style={{ display: 'grid', gridTemplateColumns: result ? '1fr 1fr' : '480px 1fr', gap: '24px', alignItems: 'start' }}>
          <div className="card">
            <p className="card-title">🔍 신청 정보 입력</p>

            {error && <div className="alert alert-error">⚠️ {error}</div>}

            <form className="form" onSubmit={handleSubmit}>
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
              <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
                {loading ? '조회 중...' : '상태 조회'}
              </button>
            </form>
          </div>

          {result && (
            <div className="card">
              <p className="card-title">{cfg.icon} 심사 결과</p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div className="info-row">
                  <span className="info-label">상태</span>
                  <span className={`badge ${cfg.badge}`} style={{ fontSize: '14px', padding: '6px 14px' }}>
                    {cfg.label}
                  </span>
                </div>
                <hr className="divider" style={{ margin: '4px 0' }} />
                <div className="info-row"><span className="info-label">상호명</span><span className="info-value">{result.name}</span></div>
                <div className="info-row"><span className="info-label">사업자번호</span><span className="info-value">{result.businessNumber}</span></div>
                <div className="info-row"><span className="info-label">신청일</span><span className="info-value" style={{ fontSize: '13px' }}>{new Date(result.createdAt).toLocaleString('ko-KR')}</span></div>
                {result.rejectReason && (
                  <div className="info-row">
                    <span className="info-label">거절 사유</span>
                    <span className="info-value" style={{ color: '#cf1322' }}>{result.rejectReason}</span>
                  </div>
                )}
                {result.status === 'APPROVED' && (
                  <>
                    <hr className="divider" style={{ margin: '4px 0' }} />
                    <button className="btn btn-primary btn-full" onClick={() => navigate('/credentials')}>
                      API 키 발급받기 →
                    </button>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
