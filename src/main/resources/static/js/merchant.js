/* merchant.js — 商家页面逻辑 */

window.switchMerchantTab = function(name, el) {
  document.querySelectorAll('.m-tab').forEach(t => t.style.display = 'none');
  document.querySelectorAll('#page-merchant .tab').forEach(t => t.classList.remove('active'));
  const target = document.getElementById('m-' + name);
  target.style.display = 'flex';
  target.style.flexDirection = 'column';
  if (el && el.classList.contains('tab')) el.classList.add('active');

  if (name === 'shop') loadMerchantShop();
  if (name === 'chat') loadMerchantChats();
  if (name === 'me') window.renderMe('m-me-content');
  if (name === 'products') loadMerchantProducts();
};

async function loadMerchantShop() {
  loadMerchantStats();
  document.getElementById('btn-product-manage').onclick = function () {
    document.querySelectorAll('.m-tab').forEach(t => t.style.display = 'none');
    document.getElementById('m-products').style.display = 'flex';
    document.getElementById('m-products').style.flexDirection = 'column';
    loadMerchantProducts();
  };
}

async function loadMerchantStats() {
  const container = document.getElementById('m-stats');
  const data = await MerchantAPI.getStats();
  if (data && data.status === 'success' && data.data) {
    const s = data.data;
    renderStats(container, {
      product_count: s.product_count,
      order_today: s.order_today,
      income_today: s.income_today,
      rating: s.rating,
    });
  } else {
    renderStats(container, { product_count: '—', order_today: '—', income_today: '—', rating: '—' });
  }
}

function renderStats(container, s) {
  container.innerHTML = `
    <div class="stat-box"><div class="stat-num">${s.product_count}</div><div class="stat-label">在售商品</div></div>
    <div class="stat-box"><div class="stat-num">${s.order_today}</div><div class="stat-label">今日订单</div></div>
    <div class="stat-box"><div class="stat-num">${s.income_today === '—' ? '—' : '¥' + s.income_today}</div><div class="stat-label">今日收入</div></div>
    <div class="stat-box"><div class="stat-num">${s.rating}</div><div class="stat-label">店铺评分</div></div>
  `;
}

async function loadMerchantProducts() {
  const container = document.getElementById('merchant-product-list');
  container.innerHTML = '<div class="loading-state">加载中...</div>';
  try {
    const res = await ProductAPI.getMerchantProducts();
    if (res && res.status === 'success' && Array.isArray(res.data)) {
      renderMerchantProductList(container, res.data);
    } else {
      container.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><div class="empty-title">暂无商品</div><div class="empty-sub">点击右上角添加第一个商品</div></div>';
    }
  } catch (e) {
    container.innerHTML = '<div class="error-state"><p>加载失败</p></div>';
  }
}

function renderMerchantProductList(container, list) {
  if (list.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><div class="empty-title">暂无商品</div><div class="empty-sub">点击右上角添加第一个商品</div></div>';
    return;
  }
  container.innerHTML = `
    <div class="product-grid">
      ${list.map(p => `
        <div class="product-card" data-product='${JSON.stringify(p).replace(/'/g, "&#39;")}'>
          <div class="product-img">${p.image_url ? `<img src="${p.image_url}">` : '📦'}</div>
          <div class="product-info">
            <div class="product-name">${p.name}</div>
            <div class="product-price">¥${p.price}</div>
            <div class="product-actions">
              <button class="btn-edit" data-id="${p.id}">编辑</button>
              <button class="btn-delete" data-id="${p.id}">删除</button>
            </div>
          </div>
        </div>
      `).join('')}
    </div>`;

  container.onclick = async (e) => {
    const id = e.target.dataset.id;
    if (!id) return;
    if (e.target.classList.contains('btn-edit')) {
      const card = e.target.closest('.product-card');
      const product = JSON.parse(card.dataset.product);
      openProductModal(product);
    }
    if (e.target.classList.contains('btn-delete')) {
      if (!confirm('确定删除该商品吗？')) return;
      try {
        const res = await ProductAPI.delete(id);
        if (res && res.status === 'success') {
          window.showToast('删除成功');
          loadMerchantProducts();
        } else {
          window.showToast(res?.message || '删除失败');
        }
      } catch (err) {
        window.showToast('删除失败');
      }
    }
  };
}

let _editProductId = null;

window.openProductModal = function (product = null) {
  _editProductId = product ? product.id : null;
  document.getElementById('modal-title').textContent = product ? '编辑商品' : '添加商品';
  document.getElementById('p-name').value = product?.name || '';
  document.getElementById('p-price').value = product?.price || '';
  document.getElementById('p-desc').value = product?.description || '';
  document.getElementById('p-category').value = product?.category || 'electronic';
  document.getElementById('p-image').value = product?.image_url || '';
  document.getElementById('product-modal').classList.add('show');
};

window.closeProductModal = function () {
  document.getElementById('product-modal').classList.remove('show');
  _editProductId = null;
};

document.getElementById('product-modal').addEventListener('click', function (e) {
  if (e.target === this) window.closeProductModal();
});

window.submitProduct = async function () {
  const data = {
    name: document.getElementById('p-name').value.trim(),
    description: document.getElementById('p-desc').value.trim(),
    price: parseFloat(document.getElementById('p-price').value),
    category: document.getElementById('p-category').value,
    image_url: document.getElementById('p-image').value.trim(),
  };
  if (!data.name) { window.showToast('请输入商品名称'); return; }
  if (isNaN(data.price) || data.price < 0) { window.showToast('请输入有效价格'); return; }
  try {
    const res = _editProductId
      ? await ProductAPI.update(_editProductId, data)
      : await ProductAPI.create(data);
    if (res && res.status === 'success') {
      window.showToast(_editProductId ? '修改成功' : '添加成功');
      window.closeProductModal();
      loadMerchantProducts();
    } else {
      window.showToast(res?.message || '操作失败');
    }
  } catch (err) {
    window.showToast('请求失败');
  }
};

async function loadMerchantChats() {
  const container = document.getElementById('m-chat-content');
  container.innerHTML = '<div class="loading-state">加载中...</div>';
  const data = await ChatAPI.getList();
  if (data && data.status === 'success' && Array.isArray(data.data)) {
    renderMerchantChatList(container, data.data);
  } else {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">💬</div><div class="empty-title">暂无顾客消息</div><div class="empty-sub">聊天接口尚未接入</div></div>';
  }
}

function renderMerchantChatList(container, list) {
  if (list.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">💬</div><div class="empty-title">暂无顾客消息</div></div>';
    return;
  }
  container.innerHTML = '<div class="card" style="padding:4px 16px;">' +
    list.map(c => `<div class="chat-item" onclick="openMerchantChat(${c.id})"><div class="chat-avatar">👤</div><div><div class="chat-name">${c.name}</div><div class="chat-preview">${c.last_message || '暂无消息'}</div></div></div>`).join('') +
    '</div>';
}

function openMerchantChat(id) { window.showToast('聊天功能开发中'); }

window.addEventListener('merchant:init', () => {
  loadMerchantShop();
});
