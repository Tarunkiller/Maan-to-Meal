/* ─── Shared JS Utilities ─────────────────────────────────────────────────── */

const API_BASE = '/api';

/* ── Token helpers ── */
const Auth = {
  getToken: () => localStorage.getItem('access_token'),
  getRefreshToken: () => localStorage.getItem('refresh_token'),
  getUser: () => { try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch { return null; } },
  setAuth: (data) => {
    if (data.access_token) localStorage.setItem('access_token', data.access_token);
    if (data.refresh_token) localStorage.setItem('refresh_token', data.refresh_token);
    if (data.user) localStorage.setItem('user', JSON.stringify(data.user));
  },
  clear: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
  },
  isLoggedIn: () => !!localStorage.getItem('access_token'),
  getRole: () => { const u = Auth.getUser(); return u ? u.role : null; },
  requireRole: (role) => {
    if (!Auth.isLoggedIn()) { window.location.href = '/frontend/auth/login.html'; return false; }
    const r = Auth.getRole();
    if (role && r !== role && r !== 'admin') {
      Toast.show('Access denied', 'error');
      setTimeout(() => window.location.href = '/', 1000);
      return false;
    }
    return true;
  }
};

/* ── API Fetch wrapper ── */
const Api = {
  async request(method, endpoint, body = null, isFormData = false) {
    const headers = {};
    const token = Auth.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (!isFormData) headers['Content-Type'] = 'application/json';

    const options = { method, headers };
    if (body) options.body = isFormData ? body : JSON.stringify(body);

    try {
      let res = await fetch(`${API_BASE}${endpoint}`, options);

      // Try token refresh on 401
      if (res.status === 401 && Auth.getRefreshToken() && endpoint !== '/auth/refresh') {
        const refreshRes = await fetch(`${API_BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${Auth.getRefreshToken()}` }
        });
        if (refreshRes.ok) {
          const rd = await refreshRes.json();
          Auth.setAuth({ access_token: rd.access_token });
          headers['Authorization'] = `Bearer ${rd.access_token}`;
          options.headers = headers;
          res = await fetch(`${API_BASE}${endpoint}`, options);
        } else {
          Auth.clear();
          window.location.href = '/frontend/auth/login.html';
          return null;
        }
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.error || data.message || 'Request failed');
      return data;
    } catch (err) {
      throw err;
    }
  },

  get: (endpoint) => Api.request('GET', endpoint),
  post: (endpoint, body) => Api.request('POST', endpoint, body),
  put: (endpoint, body) => Api.request('PUT', endpoint, body),
  delete: (endpoint) => Api.request('DELETE', endpoint),
  postForm: (endpoint, formData) => Api.request('POST', endpoint, formData, true),
  putForm: (endpoint, formData) => Api.request('PUT', endpoint, formData, true),
};

/* ── Toast System ── */
const Toast = {
  container: null,
  init() {
    if (!document.getElementById('toast-container')) {
      const c = document.createElement('div');
      c.id = 'toast-container';
      document.body.appendChild(c);
    }
    this.container = document.getElementById('toast-container');
  },
  show(message, type = 'info', title = '', duration = 4000) {
    this.init();
    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    const titles = { success: 'Success', error: 'Error', warning: 'Warning', info: 'Info' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <span class="toast-icon">${icons[type] || '💬'}</span>
      <div class="toast-body">
        <div class="toast-title">${title || titles[type]}</div>
        <div class="toast-message">${message}</div>
      </div>
      <button class="toast-close" onclick="this.parentElement.remove()">✕</button>`;
    this.container.appendChild(toast);
    setTimeout(() => {
      toast.style.animation = 'toast-out 0.3s ease forwards';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }
};

/* ── Loader ── */
const Loader = {
  show(msg = 'Loading...') {
    let el = document.getElementById('loader-overlay');
    if (!el) {
      el = document.createElement('div');
      el.id = 'loader-overlay';
      el.className = 'loader-overlay';
      el.innerHTML = `<div style="text-align:center"><div class="spinner"></div><p style="margin-top:1rem;color:var(--text-secondary);font-size:0.9rem">${msg}</p></div>`;
      document.body.appendChild(el);
    }
  },
  hide() {
    const el = document.getElementById('loader-overlay');
    if (el) el.remove();
  }
};

/* ── Format Helpers ── */
const Fmt = {
  currency: (n) => `₹${Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
  date: (d) => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—',
  datetime: (d) => d ? new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—',
  stars: (rating, max = 5) => {
    const full = Math.floor(rating); const half = rating % 1 >= 0.5;
    let s = '★'.repeat(full);
    if (half) s += '½';
    s += '☆'.repeat(Math.max(0, max - full - (half ? 1 : 0)));
    return s;
  },
  status: (status) => {
    const map = {
      pending: ['warning', '⏳ Pending'],
      confirmed: ['info', '✅ Confirmed'],
      processing: ['info', '🔄 Processing'],
      shipped: ['primary', '📦 Shipped'],
      out_for_delivery: ['primary', '🚚 Out for Delivery'],
      delivered: ['success', '✔ Delivered'],
      cancelled: ['danger', '✕ Cancelled'],
      paid: ['success', '💳 Paid'],
      failed: ['danger', '✕ Failed'],
    };
    const [cls, label] = map[status] || ['muted', status];
    return `<span class="badge badge-${cls}">${label}</span>`;
  },
  timeAgo: (d) => {
    const diff = Date.now() - new Date(d).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return Fmt.date(d);
  }
};

/* ── DOM helpers ── */
const $ = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

function el(tag, cls, html) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (html) e.innerHTML = html;
  return e;
}

/* ── Navbar user state ── */
function initNavbar() {
  const user = Auth.getUser();
  const loginBtn = document.getElementById('nav-login');
  const signupBtn = document.getElementById('nav-signup');
  const userMenu = document.getElementById('nav-user-menu');
  const userNameEl = document.getElementById('nav-user-name');
  const logoutBtn = document.getElementById('nav-logout');
  const dashLink = document.getElementById('nav-dashboard');

  if (user && Auth.isLoggedIn()) {
    if (loginBtn) loginBtn.style.display = 'none';
    if (signupBtn) signupBtn.style.display = 'none';
    if (userMenu) userMenu.style.display = 'flex';
    if (userNameEl) userNameEl.textContent = user.name.split(' ')[0];
    if (dashLink) {
      const links = { farmer: '/frontend/farmer/dashboard.html', consumer: '/frontend/consumer/dashboard.html', admin: '/frontend/admin/dashboard.html' };
      dashLink.href = links[user.role] || '#';
    }
  } else {
    if (userMenu) userMenu.style.display = 'none';
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      Auth.clear();
      Toast.show('Logged out successfully', 'success');
      setTimeout(() => window.location.href = '/frontend/index.html', 800);
    });
  }

  // Cart count
  updateCartCount();
}

async function updateCartCount() {
  const cartBadge = document.getElementById('cart-count');
  if (!cartBadge || !Auth.isLoggedIn() || Auth.getRole() !== 'consumer') return;
  try {
    const data = await Api.get('/consumer/cart');
    cartBadge.textContent = data.count || 0;
    cartBadge.style.display = data.count > 0 ? 'flex' : 'none';
  } catch {}
}

/* ── Notification Bell ── */
async function initNotifications() {
  const badge = document.getElementById('notif-badge');
  if (!badge || !Auth.isLoggedIn()) return;
  try {
    const data = await Api.get('/notifications');
    if (data.unread_count > 0) {
      badge.textContent = data.unread_count;
      badge.style.display = 'flex';
    }
  } catch {}
}

/* ── Product card builder ── */
function buildProductCard(p, onclick) {
  const card = el('div', 'product-card slide-up');
  card.innerHTML = `
    <div class="product-card-img-wrap">
      <img class="product-card-img" src="${p.image_url || '/frontend/static/uploads/default_product.jpg'}" 
           alt="${p.name}" onerror="this.src='/frontend/static/uploads/default_product.jpg'">
      ${p.is_organic ? '<span class="product-badge organic">🌿 Organic</span>' : `<span class="product-badge">${p.quality_grade || 'A'} Grade</span>`}
    </div>
    <div class="product-card-body">
      <div class="product-name">${p.name}</div>
      <div class="product-farmer">🌾 ${p.farm_name || p.farmer_name || 'Farm Fresh'}</div>
      <div class="product-price">${Fmt.currency(p.price_per_unit)}<span>/${p.unit}</span></div>
      <div class="product-meta">
        <div class="product-rating">⭐ ${p.rating || 0} <span style="color:var(--text-muted)">(${p.total_reviews || 0})</span></div>
        <div class="product-location">📍 ${(p.farm_location || p.location || '').split(',')[0]}</div>
      </div>
    </div>`;
  card.addEventListener('click', onclick || (() => window.location.href = `/frontend/consumer/product.html?id=${p.id}`));
  return card;
}

/* ── Local storage cart helpers (offline) ── */
const LocalCart = {
  get: () => JSON.parse(localStorage.getItem('local_cart') || '[]'),
  set: (items) => localStorage.setItem('local_cart', JSON.stringify(items)),
  count: () => LocalCart.get().length,
};

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initNotifications();
});
