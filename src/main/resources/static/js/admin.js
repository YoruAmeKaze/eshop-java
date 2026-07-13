/* admin.js — 管理员页面逻辑 */

window.switchAdminTab = function(name, el) {
  document.querySelectorAll('.a-tab').forEach(t => t.style.display = 'none');
  document.querySelectorAll('#page-admin .tab').forEach(t => t.classList.remove('active'));
  const target = document.getElementById('a-' + name);
  target.style.display = 'flex';
  target.style.flexDirection = 'column';
  el.classList.add('active');

  if (name === 'reports') loadReports();
  if (name === 'me') window.renderMe('a-me-content');
};

async function loadReports() {
  const container = document.getElementById('a-reports-content');
  container.innerHTML = '<div class="loading-state">加载中...</div>';
  const data = await ReportAPI.getList();
  if (data && data.status === 'success' && Array.isArray(data.data)) {
    renderReports(container, data.data);
  } else {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">🚩</div><div class="empty-title">暂无待处理举报</div><div class="empty-sub">举报接口尚未接入</div></div>';
  }
}

const REPORT_TYPE_MAP = { product: '商品举报', user: '用户举报', shop: '店铺举报' };

function renderReports(container, list) {
  if (list.length === 0) {
    container.innerHTML = '<div class="empty-state"><div class="empty-icon">✅</div><div class="empty-title">没有待处理的举报</div></div>';
    return;
  }
  container.innerHTML = list.map(r => `
    <div class="report-card" id="report-${r.id}">
      <div class="report-type">🚩 ${REPORT_TYPE_MAP[r.type] || '举报'} · ${r.created_at}</div>
      <div class="report-content">${r.content}</div>
      <div class="btn-row">
        <button class="btn-sm btn-approve" onclick="handleReport(${r.id}, 'approve', this)">✓ 通过举报</button>
        <button class="btn-sm btn-reject" onclick="handleReport(${r.id}, 'reject', this)">✗ 驳回举报</button>
      </div>
    </div>
  `).join('');
}

window.handleReport = async function(id, action, btn) {
  const card = document.getElementById('report-' + id);
  card.querySelectorAll('button').forEach(b => b.disabled = true);
  try {
    const data = await ReportAPI.handle(id, action);
    const label = action === 'approve' ? '已通过' : '已驳回';
    const color = action === 'approve' ? '#389e0d' : '#cf1322';
    if (!data || data.status === 'success') {
      card.style.opacity = '0.5';
      card.querySelector('.btn-row').innerHTML = `<span style="font-size:13px;color:${color};font-weight:500;">${label}</span>`;
    } else {
      card.querySelectorAll('button').forEach(b => b.disabled = false);
      window.showToast(data.message || '操作失败');
    }
  } catch (e) {
    card.querySelectorAll('button').forEach(b => b.disabled = false);
    window.showToast('网络错误，请重试');
  }
};

window.addEventListener('admin:init', () => { loadReports(); });
