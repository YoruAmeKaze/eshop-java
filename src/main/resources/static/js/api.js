/* ============================================================
   api.js — 所有后端请求统一管理
   ============================================================ */

const API_BASE = '';

async function request(method, path, body = null, auth = false) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth) {
    const token = localStorage.getItem('kv_token');
    if (token) headers['Authorization'] = 'Bearer ' + token.trim();
  }
  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);

  const res = await fetch(API_BASE + path, options);
  if (res.status === 401) {
    localStorage.removeItem('kv_token');
    window.showPage('page-login');
    throw new Error('登录已过期，请重新登录');
  }
  return res.json();
}

const AuthAPI = {
  login(name, password) {
    return request('POST', '/api/login', { name, password });
  },
  register(name, password, roles, tel) {
    const body = { name, password, roles };
    if (tel) body.tel = tel;
    return request('POST', '/api/register', body);
  },
  getMe() {
    return request('GET', '/api/user/me', null, true);
  },
};

const ProductAPI = {
  async getList(page = 1, limit = 10, category = null) {
    let url = `/api/products?page=${page}&limit=${limit}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    return request('GET', url);
  },
  async getDetail(id) {
    return request('GET', `/api/products/${id}`);
  },
  async getMerchantProducts() {
    return request('GET', '/api/merchant/products', null, true);
  },
  async create(data) {
    return request('POST', '/api/merchant/products', data, true);
  },
  async update(id, data) {
    return request('PUT', `/api/merchant/products/${id}`, data, true);
  },
  async delete(id) {
    return request('DELETE', `/api/merchant/products/${id}`, null, true);
  },
};

const OrderAPI = {
  async getList() { return null; },
  async create(productId, quantity) { return null; },
  async getMerchantOrders() { return null; },
};

const CartAPI = {
  async getCart() {
    return request('GET', '/api/cart', null, true);
  },
  async addItem(goodsId, quantity = 1) {
    return request('POST', '/api/cart', { goods_id: goodsId, quantity }, true);
  },
  async removeItem(goodsId) {
    return request('DELETE', `/api/cart/${goodsId}`, null, true);
  },
  async updateQuantity(goodsId, quantity) {
    return request('PUT', `/api/cart/${goodsId}`, { quantity }, true);
  },
};

const ChatAPI = {
  async getList() { return null; },
  async getMessages(chatId) { return null; },
  async sendMessage(chatId, content) { return null; },
};

const ReportAPI = {
  async getList() { return null; },
  async handle(reportId, action) { return null; },
};

const MerchantAPI = {
  async getStats() { return null; },
};
