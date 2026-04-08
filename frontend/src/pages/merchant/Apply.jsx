import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/axios';

export default function Apply() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', businessNumber: '', phone: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/merchant-applications', form);
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || '신청에 실패했습니다. 입력 정보를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  if (result) {
    return (
      <>
        <header className="app-header">
          <div className="app-header-inner">
            <span className="app-logo">배민<span>페이</span></span>
          </div>
        </header>
        <div className="page">
          <div className="card" style={{ textAlign: 'center' }}>
            <div className="success-screen">
              <div className="success-icon">🎉</div>
              <h2 className="success-title">신청이 완료되었습니다!</h2>
              <p className="success-desc">
                가맹점 심사가 진행 중입니다.<br />
                승인 후 API 키를 발급받아 결제 서비스를 이용하실 수 있습니다.
              </p>
              <div className="info-box" style={{ textAlign: 'left', marginBottom: '24px' }}>
                <div className="info-row">
                  <span className="info-label">신청 ID</span>
                  <span className="info-value mono">{result.id}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">상태</span>
                  <span className="badge badge-pending">심사 중</span>
                </div>
              </div>
              <button className="btn btn-primary btn-full" onClick={() => navigate('/status')}>
                신청 상태 확인하기
              </button>
            </div>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <header className="app-header">
        <div className="app-header-inner">
          <span className="app-logo">배민<span>페이</span></span>
          <nav style={{ display: 'flex', gap: '4px', marginLeft: 'auto' }}>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/status')}>신청 상태</button>
            <button className="btn btn-outline btn-sm" style={{ color: '#fff', borderColor: '#444' }} onClick={() => navigate('/credentials')}>API 키</button>
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/dashboard')}>대시보드</button>
          </nav>
        </div>
      </header>

      <div className="page">
        <h1 className="page-title">가맹점 신청</h1>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: '24px', alignItems: 'start' }}>
          <div className="card">
            <p className="card-title">📋 사업자 정보 입력</p>

            {error && <div className="alert alert-error">⚠️ {error}</div>}

            <form className="form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">상호명 (가맹점명)</label>
                <input className="form-input" name="name" value={form.name} onChange={handleChange}
                  placeholder="예) 맛있는 치킨집" required />
              </div>

              <div className="form-group">
                <label className="form-label">사업자번호</label>
                <input className="form-input" name="businessNumber" value={form.businessNumber} onChange={handleChange}
                  placeholder="000-00-00000" required />
                <p style={{ fontSize: '12px', color: '#8B95A1' }}>형식: XXX-XX-XXXXX</p>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">연락처</label>
                  <input className="form-input" name="phone" value={form.phone} onChange={handleChange}
                    placeholder="010-0000-0000" required />
                </div>
                <div className="form-group">
                  <label className="form-label">이메일</label>
                  <input className="form-input" name="email" type="email" value={form.email} onChange={handleChange}
                    placeholder="example@email.com" required />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">비밀번호</label>
                <input className="form-input" name="password" type="password" value={form.password} onChange={handleChange}
                  placeholder="8자 이상" required />
                <p style={{ fontSize: '12px', color: '#8B95A1' }}>신청 상태 조회 및 API 키 발급 시 사용됩니다.</p>
              </div>

              <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
                {loading ? '신청 중...' : '가맹점 신청하기'}
              </button>
            </form>
          </div>

          <div className="card">
            <p className="card-title">📌 신청 안내</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', fontSize: '14px', color: '#8B95A1' }}>
              <div>
                <p style={{ fontWeight: 700, color: '#191F28', marginBottom: '6px' }}>1. 신청 접수</p>
                <p>사업자 정보를 입력하고 가맹점을 신청합니다.</p>
              </div>
              <div>
                <p style={{ fontWeight: 700, color: '#191F28', marginBottom: '6px' }}>2. 심사</p>
                <p>관리자가 신청 내용을 검토합니다. 보통 1~3 영업일이 소요됩니다.</p>
              </div>
              <div>
                <p style={{ fontWeight: 700, color: '#191F28', marginBottom: '6px' }}>3. API 키 발급</p>
                <p>승인 후 API 키를 발급받아 결제 연동을 시작하세요.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
