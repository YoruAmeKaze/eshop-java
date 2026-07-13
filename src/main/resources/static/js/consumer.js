/* ============================================================
   consumer.js — 消费者页面逻辑
   ============================================================ */

let currentPage = 1;
let currentCategory = null;
let totalPages = 1;

const categories = [
  { key: null, label: '推荐' },
  { key: 'electronic', label: '数码' },
  { key: 'clothes', label: '服饰' },
  { key: 'living', label: '家居' },
  { key: 'beauty', label: '美妆' },
  { key: 'sports', label: '运动' },
  { key: 'baby', label: '母婴' },
  { key: 'food', label: '食品' },
  { key: 'book', label: '图书' },
  { key: 'others', label: '其他' },
];

window.switchConsumerTab = function (name, el) {
  document.querySelectorAll('.c-tab').forEach(t => t.style.display = 'none');
  document.querySelectorAll('#page-consumer .tab').forEach(t => t.classList.remove('active'));
  const target = document.getElementById('c-' + name);
  target.style.display = 'flex';
  target.style.flexDirection = 'column';
  el.classList.add('active');

  if (name === 'home') loadProducts();
  if (name === 'orders') loadOrders();
  if (name === 'cart') loadCart();
  if (name === 'chat') loadConsumerChats();
  if (name === 'me') window.renderMe('c-me-content');
};

function renderCategoryBar() {
  const bar = document.getElementById('category-bar');
  bar.innerHTML = categories.map(c => `
    <button class="category-btn" data-key="${c.key}">${c.label}</button>
  `).join('');

  bar.onclick = (e) => {
    if (!e.target.classList.contains('category-btn')) return;
    const key = e.target.dataset.key === 'null' ? null : e.target.dataset.key;
    document.querySelectorAll('.category-btn').forEach(b => b.classList.remove('active'));
    e.target.classList.add('active');
    currentCategory = key;
    currentPage = 1;
    loadProducts();
  };
  bar.querySelector('[data-key="null"]').classList.add('active');
}

async function loadProducts() {
  const grid = document.getElementById('product-grid');
  grid.innerHTML = '<div class="loading-state">加载中...</div>';
  try {
    const data = await ProductAPI.getList(currentPage, 10, currentCategory);
    if (data && data.status === 'success') {
      totalPages = data.total_pages || 1;
      renderProducts(grid, data.data);
      renderPagination();
    } else {
      grid.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><div class="empty-title">暂无商品</div></div>';
      document.getElementById('pagination').innerHTML = '';
    }
  } catch (err) {
    grid.innerHTML = '<div class="error-state"><p>加载失败，请稍后重试</p></div>';
  }
}

window.refreshProducts = function () { currentPage = 1; loadProducts(); };

function renderPagination() {
  const container = document.getElementById('pagination');
  if (totalPages <= 1) { container.innerHTML = ''; return; }
  container.innerHTML = `
    <div class="pagination">
      <button onclick="changePage(-1)" ${currentPage <= 1 ? 'disabled' : ''}>上一页</button>
      <span>第 ${currentPage} / ${totalPages} 页</span>
      <button onclick="changePage(1)" ${currentPage >= totalPages ? 'disabled' : ''}>下一页</button>
    </div>
  `;
}

window.changePage = function (delta) {
  const next = currentPage + delta;
  if (next < 1 || next > totalPages) return;
  currentPage = next;
  loadProducts();
  document.getElementById('c-home').querySelector('.content').scrollTop = 0;
};

function renderProducts(grid, list) {
  if (!list || list.length === 0) {
    grid.innerHTML = '<div class="empty-state" style="grid-column:1/-1;"><div class="empty-icon">📦</div><div class="empty-title">暂无商品</div></div>';
    return;
  }
  grid.innerHTML = list.map(p => `
    <div class="product-card" onclick="onProductClick(${p.id})">
      <div class="product-img">${p.image_url ? `<img src="${p.image_url}">` : '🛍️'}</div>
      <div class="product-info">
        <div class="product-name">${p.name}</div>
        <div class="product-price">¥${p.price}</div>
      </div>
    </div>
  `).join('');
}

function onProductClick(id) { if (id) window.openProductDetail(id); }

async function loadOrders() {
  const container = document.getElementById('c-orders-content');
  container.innerHTML = '<div class="loading-state">加载中...</div>';
  try {
    const data = await OrderAPI.getList();
    if (data && data.status === 'success' && Array.isArray(data.data)) {
      renderOrders(container, data.data);
    } else {
      container.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><div class="empty-title">暂无订单</div><div class="empty-sub">订单接口尚未接入</div></div>';
    }
  } catch (e) {
    container.innerHTML = '<div class="error-state"><p>加载失败</p></div>';
  }
}

const ORDER_STATUS_MAP = {
  pending: { label: '待付款', cls: 'status-pending' },
  paid: { label: '已付款', cls: 'status-done' },
  shipped: { label: '已发货', cls: 'status-shipped' },
  done: { label: '已完成', cls: 'status-done' },
  cancelled: { label: '已取消', cls: 'status-pending' },
};

function renderOrders(container, list) {
  if (list.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><div class="empty-title">还没有订单</div><div class="empty-sub">去首页逛逛吧</div></div>';
    return;
  }
  container.innerHTML = list.map(o => {
    const s = ORDER_STATUS_MAP[o.status] || { label: o.status, cls: '' };
    return `<div class="order-card">
      <div class="order-header"><span class="order-id">订单号：#${o.id}</span><span class="order-status ${s.cls}">${s.label}</span></div>
      <div style="font-size:14px;color:#333;margin-bottom:8px;">${o.product_name} × ${o.quantity}</div>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span style="font-size:12px;color:#999;">${o.created_at}</span>
        <span style="font-size:16px;font-weight:600;color:#1D9E75;">¥${o.total_price}</span>
      </div>
    </div>`;
  }).join('');
}

async function loadCart() {
  const container = document.getElementById('c-cart-content');
  container.innerHTML = '<div class="loading-state">加载中...</div>';
  try {
    const data = await CartAPI.getCart();
    if (data && data.status === 'success' && Array.isArray(data.data)) {
      renderCart(container, data.data);
    } else {
      renderCart(container, []);
    }
  } catch (err) {
    container.innerHTML = '<div class="error-state"><p>加载失败，请重试</p></div>';
  }
}

function renderCart(container, list) {
  if (list.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">🛒</div><div class="empty-title">购物车是空的</div><div class="empty-sub">去首页挑选喜欢的商品吧</div></div>';
    return;
  }
  let total = 0;
  const items = list.map(item => {
    const subtotal = item.price * item.quantity;
    total += subtotal;
    return `<div class="cart-item">
      <div class="cart-img">${item.image_url ? `<img src="${item.image_url}">` : '🛍️'}</div>
      <div class="cart-info">
        <div class="cart-name">${item.name}</div>
        <div class="cart-price">¥${item.price}</div>
        <div class="cart-qty">
          <button onclick="changeCartQuantity(${item.goods_id}, ${item.quantity - 1})">−</button>
          <span>${item.quantity}</span>
          <button onclick="changeCartQuantity(${item.goods_id}, ${item.quantity + 1})">+</button>
          <button class="cart-delete" onclick="removeCartItem(${item.goods_id})">删除</button>
        </div>
      </div>
    </div>`;
  }).join('');
  container.innerHTML = `
    <div class="card" style="padding:4px 16px;">${items}
      <div class="cart-total"><span class="cart-total-label">合计</span><span class="cart-total-price">¥${total.toFixed(2)}</span></div>
    </div>
    <button class="cart-checkout" onclick="window.showToast('结算功能开发中')">结算</button>`;
}

window.removeCartItem = async function (goodsId) {
  try {
    const data = await CartAPI.removeItem(goodsId);
    if (data && data.status === 'success') { window.showToast('已删除'); loadCart(); }
    else { window.showToast(data?.message || '删除失败'); }
  } catch (err) { window.showToast('删除失败'); }
};

window.changeCartQuantity = async function (goodsId, quantity) {
  if (quantity <= 0) { window.removeCartItem(goodsId); return; }
  try {
    const data = await CartAPI.updateQuantity(goodsId, quantity);
    if (data && data.status === 'success') { loadCart(); }
    else { window.showToast(data?.message || '修改失败'); }
  } catch (err) { window.showToast('修改失败'); }
};

async function loadConsumerChats() {
  const container = document.getElementById('c-chat-content');
  container.innerHTML = '<div class="loading-state">加载中...</div>';
  const data = await ChatAPI.getList();
  if (data && data.status === 'success' && Array.isArray(data.data)) {
    renderChatList(container, data.data);
  } else {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">💬</div><div class="empty-title">暂无消息</div><div class="empty-sub">聊天接口尚未接入</div></div>';
  }
}

function renderChatList(container, list) {
  if (list.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">💬</div><div class="empty-title">暂无消息</div></div>';
    return;
  }
  container.innerHTML = '<div class="card" style="padding:4px 16px;">' +
    list.map(c => `<div class="chat-item" onclick="openChat(${c.id})"><div class="chat-avatar">🏪</div><div><div class="chat-name">${c.name}</div><div class="chat-preview">${c.last_message || '暂无消息'}</div></div></div>`).join('') +
    '</div>';
}

function openChat(id) { window.showToast('聊天功能开发中'); }

window.addEventListener('consumer:init', () => { renderCategoryBar(); loadProducts(); });
