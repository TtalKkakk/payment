import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/axios';

const STATUS_CONFIG = {
  READY:       { label: '준비',    badge: 'badge-pending', icon: '🕐' },
  AUTHORIZING: { label: '승인 중', badge: 'badge-active',  icon: '⚡' },
  AUTHORIZED:  { label: '승인됨',  badge: 'badge-approved',icon: '✅' },
  FAILED:      { label: '실패',    badge: 'badge-rejected', icon: '❌' },
  CANCELED:    { label: '취소됨',  badge: 'badge-rejected', icon: '🚫' },
};

export default function Dashboard() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('create');
  const [createForm, setCreateForm] = useState({
    amount: '', merchantOrderId: '', orderName: '',
    customerEmail: '', customerName: '', callbackUrl: '',
  });
  const [createdPayment, setCreatedPayment] = useState(null);
  const [searchId, setSearchId] = useState('');
  const [payment, setPayment] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const hasAuth = !!localStorage.getItem('apiKey');

  const handleCreateChange = e => setCreateForm({ ...createForm, [e.target.name]: e.target.value });

  const handleCreate = async e => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/payments', { ...createForm, amount: Number(createForm.amount) });
      setCreatedPayment(res.data);
    } catch (err) {
      setError(err.response?.data?.message || '결제 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async e => {
    e.preventDefault();
    setError('');
    setPayment(null);
    setLoading(true);
    try {
      const res = await api.get(`/payments/${searchId}`);
      setPayment(res.data);
    } catch (err) {
      setError(err.response?.data?.message || '결제를 찾을 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async paymentId => {
    if (!confirm('정말 결제를 취소하시겠습니까?')) return;
    setError('');
    try {
      await api.post(`/payments/${paymentId}/cancel`);
      setPayment(prev => ({ ...prev, status: 'CANCELED' }));
    } catch (err) {
      setError(err.response?.data?.message || '취소에 실패했습니다.');
    }
  };

  if (!hasAuth) {
    return (
      <>
        <header className="app-header">
          <div className="app-header-inner">
            <span className="app-logo">배민<span>페이</span></span>
          </div>
        </header>
        <div className="page">
          <div className="card" style={{ textAlign: 'center', padding: '60px 28px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔐</div>
            <h2 style={{ fontSize: '20px', fontWeight: 900, marginBottom: '10px' }}>API 키가 필요합니다</h2>
            <p style={{ color: '#8B95A1', fontSize: '14px', marginBottom: '28px' }}>
              대시보드를 이용하려면 먼저 API 키를 발급받으세요.
            </p>
            <button className="btn btn-primary" onClick={() => navigate('/credentials')}>
              API 키 발급받기
            </button>
          </div>
        </div>
      </>
    );
  }

  const cfg = payment ? (STATUS_CONFIG[payment.status] ?? { label: payment.status, badge: 'badge-pending', icon: '❓' }) : null;

  return (
    <>
      <header className="app-header">
        <div className="app-header-inner">
          <span className="app-logo">배민<span>페이</span></span>
          <nav style={{ display: 'flex', gap: '4px', marginLeft: 'auto' }}>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/')}>가맹점 신청</button>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/credentials')}>API 키</button>
            <button
              className="btn btn-outline btn-sm"
              style={{ color: '#FFE400', borderColor: '#555' }}
              onClick={() => { localStorage.removeItem('apiKey'); localStorage.removeItem('apiSecret'); navigate('/credentials'); }}
            >
              로그아웃
            </button>
          </nav>
        </div>
      </header>

      <div className="page">
        <h1 className="page-title">가맹점 대시보드</h1>

        {error && <div className="alert alert-error">⚠️ {error}</div>}

        <div className="nav-tabs">
          <button className={`nav-tab ${tab === 'create' ? 'active' : ''}`} onClick={() => { setTab('create'); setCreatedPayment(null); }}>결제 생성</button>
          <button className={`nav-tab ${tab === 'search' ? 'active' : ''}`} onClick={() => { setTab('search'); setPayment(null); }}>결제 조회</button>
        </div>

        {tab === 'create' && (
          <>
            {createdPayment ? (
              <div className="card">
                <div className="success-screen">
                  <div className="success-icon">🧾</div>
                  <h2 className="success-title">결제가 생성되었습니다!</h2>
                  <p className="success-desc">아래 Payment ID로 결제 승인을 진행하세요.</p>
                  <div className="info-box" style={{ textAlign: 'left', marginBottom: '24px' }}>
                    <div className="info-row">
                      <span className="info-label">Payment ID</span>
                      <span className="info-value mono">{createdPayment.paymentId}</span>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                    <button className="btn btn-outline" onClick={() => setCreatedPayment(null)}>새 결제 생성</button>
                    <button className="btn btn-primary" onClick={() => { setSearchId(createdPayment.paymentId); setTab('search'); }}>
                      결제 조회하기
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="card">
                <p className="card-title">💳 새 결제 생성</p>
                <form className="form" onSubmit={handleCreate}>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">금액 (원)</label>
                      <input className="form-input" name="amount" type="number" min="1"
                        value={createForm.amount} onChange={handleCreateChange}
                        placeholder="15,000" required />
                    </div>
                    <div className="form-group">
                      <label className="form-label">주문 ID</label>
                      <input className="form-input" name="merchantOrderId"
                        value={createForm.merchantOrderId} onChange={handleCreateChange}
                        placeholder="ORDER-001" required />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">주문명</label>
                    <input className="form-input" name="orderName"
                      value={createForm.orderName} onChange={handleCreateChange}
                      placeholder="치킨 2마리 외 1건" required />
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">고객 이름</label>
                      <input className="form-input" name="customerName"
                        value={createForm.customerName} onChange={handleCreateChange}
                        placeholder="홍길동" />
                    </div>
                    <div className="form-group">
                      <label className="form-label">고객 이메일</label>
                      <input className="form-input" name="customerEmail" type="email"
                        value={createForm.customerEmail} onChange={handleCreateChange}
                        placeholder="customer@email.com" />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label">콜백 URL</label>
                    <input className="form-input" name="callbackUrl"
                      value={createForm.callbackUrl} onChange={handleCreateChange}
                      placeholder="https://yoursite.com/callback" />
                  </div>

                  <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
                    {loading ? '생성 중...' : '결제 생성하기'}
                  </button>
                </form>
              </div>
            )}
          </>
        )}

        {tab === 'search' && (
          <div className="card">
            <p className="card-title">🔍 결제 조회</p>
            <form onSubmit={handleSearch} style={{ display: 'flex', gap: '10px', marginBottom: '24px' }}>
              <input
                className="form-input"
                value={searchId}
                onChange={e => setSearchId(e.target.value)}
                placeholder="Payment ID를 입력하세요"
                required
                style={{ flex: 1, fontFamily: 'monospace', fontSize: '13px' }}
              />
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? '조회 중...' : '조회'}
              </button>
            </form>

            {payment && (
              <>
                <hr className="divider" />
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                  <h3 style={{ fontWeight: 900, fontSize: '18px' }}>{payment.orderName}</h3>
                  <span className={`badge ${cfg.badge}`} style={{ fontSize: '14px', padding: '6px 14px' }}>
                    {cfg.icon} {cfg.label}
                  </span>
                </div>
                <div className="info-box" style={{ marginTop: 0 }}>
                  <div className="info-row">
                    <span className="info-label">결제 금액</span>
                    <span className="info-value" style={{ fontWeight: 900, fontSize: '18px' }}>
                      {Number(payment.amount).toLocaleString()}원
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">Payment ID</span>
                    <span className="info-value mono">{payment.paymentId}</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">주문 ID</span>
                    <span className="info-value">{payment.merchantOrderId}</span>
                  </div>
                  {payment.customerName && (
                    <div className="info-row">
                      <span className="info-label">고객</span>
                      <span className="info-value">{payment.customerName}</span>
                    </div>
                  )}
                  <div className="info-row">
                    <span className="info-label">생성일</span>
                    <span className="info-value" style={{ fontSize: '13px' }}>
                      {new Date(payment.createdAt).toLocaleString('ko-KR')}
                    </span>
                  </div>
                </div>
                {payment.status === 'AUTHORIZED' && (
                  <button
                    className="btn btn-danger"
                    style={{ marginTop: '20px' }}
                    onClick={() => handleCancel(payment.paymentId)}
                  >
                    결제 취소
                  </button>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </>
  );
}
