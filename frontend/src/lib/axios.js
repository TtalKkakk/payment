import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
});

// 가맹점 API 키 인증 헤더 자동 첨부
api.interceptors.request.use(config => {
  const apiKey = localStorage.getItem('apiKey');
  const apiSecret = localStorage.getItem('apiSecret');
  if (apiKey && apiSecret) {
    config.headers['X-API-KEY'] = apiKey;
    config.headers['X-API-SECRET'] = apiSecret;
  }
  return config;
});

export default api;
