const state = {
  apiBase: localStorage.getItem('seck8s.apiBase') || 'http://127.0.0.1:8080',
  token: localStorage.getItem('seck8s.token') || '',
  profile: null,
  authMode: 'login'
};

const $ = (selector) => document.querySelector(selector);
const text = (value) => value == null || value === '' ? '-' : value;

function toast(message) {
  const node = $('#toast');
  node.textContent = message;
  node.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => node.classList.remove('show'), 2600);
}

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  let response;
  try {
    response = await fetch(`${state.apiBase}${path}`, { ...options, headers });
  } catch {
    throw new Error('无法连接网关，请确认服务已启动且浏览器请求已配置 CORS。');
  }
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.code !== 200) {
    if (response.status === 401) logout();
    throw new Error(payload.message || `请求失败（${response.status}）`);
  }
  return payload.data;
}

function money(value) { return value == null ? '--' : `¥${Number(value).toFixed(2)}`; }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '时间待定'; }

async function loadTickets() {
  const list = $('#ticket-list');
  list.innerHTML = '<p class="empty">正在读取票务信息...</p>';
  try {
    const keyword = $('#ticket-search').value.trim();
    const tickets = await api(`/api/tickets${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`);
    list.innerHTML = tickets?.length ? tickets.map(ticket => `<article class="ticket"><div class="ticket-visual"><span class="ticket-category">${text(ticket.category)}</span></div><div class="ticket-body"><h3>${text(ticket.name)}</h3><p>${text(ticket.description)}</p><div class="ticket-footer"><span class="price">${money(ticket.price)} 起</span><button data-ticket="${ticket.ticketId}">查看票档</button></div></div></article>`).join('') : '<p class="empty">暂无在售票务。</p>';
  } catch (error) { list.innerHTML = `<p class="empty">${error.message}</p>`; }
}

async function showTicketSkus(ticketId) {
  try {
    const skus = await api(`/api/tickets/${ticketId}/skus`);
    toast(skus?.length ? skus.map(s => `${s.name} ${money(s.price)}，余 ${text(s.extStock)}`).join('；') : '暂无可售票档');
  } catch (error) { toast(error.message); }
}

async function loadActivities() {
  const list = $('#activity-list');
  list.innerHTML = '<p class="empty">正在读取活动信息...</p>';
  try {
    const activities = await api('/api/activities/ongoing');
    if (!activities?.length) { list.innerHTML = '<p class="empty">当前没有进行中的秒杀活动。</p>'; return; }
    const rows = await Promise.all(activities.map(async activity => ({ activity, skus: await api(`/api/activities/${activity.activityId}/skus`).catch(() => []) })));
    list.innerHTML = rows.map(({ activity, skus }) => `<article class="activity"><div><h3>${text(activity.name)}</h3><p class="activity-meta">${formatTime(activity.startTime)} 至 ${formatTime(activity.endTime)}</p></div><span class="status">进行中</span><div class="activity-skus">${skus.length ? skus.map(sku => `<div class="sku"><span>${text(sku.skuId)}</span><strong>${money(sku.seckillPrice)}</strong><span>限 ${text(sku.quotaPerUser)} 张</span><button data-activity="${activity.activityId}" data-sku="${sku.skuId}">立即秒杀</button></div>`).join('') : '<span class="muted">暂无票档</span>'}</div></article>`).join('');
  } catch (error) { list.innerHTML = `<p class="empty">${error.message}</p>`; }
}

async function seckill(activityId, skuId) {
  if (!state.token) { openAuth(); toast('请先登录后再参与秒杀。'); return; }
  try {
    const result = await api('/api/seckill', { method: 'POST', body: JSON.stringify({ activityId, skuId, quantity: 1 }) });
    toast(result.message || `请求已提交，订单号：${result.orderId}`);
    await loadOrders();
  } catch (error) { toast(error.message); }
}

async function loadOrders() {
  const list = $('#order-list');
  if (!state.token) { list.innerHTML = '<p class="empty">登录后可查看订单。</p>'; return; }
  list.innerHTML = '<p class="empty">正在读取订单...</p>';
  try {
    const orders = await api('/api/orders');
    list.innerHTML = orders?.length ? orders.map(order => `<article class="order"><div class="order-main"><h3>${text(order.orderId)}</h3><p>${text(order.skuName || order.skuId)} · ${money(order.amount || order.payAmount)}</p></div><span class="status ${text(order.status)}">${text(order.status)}</span>${order.status === 'UNPAID' ? `<button class="button button-primary button-small" data-pay="${order.orderId}">支付</button>` : ''}</article>`).join('') : '<p class="empty">还没有订单记录。</p>';
  } catch (error) { list.innerHTML = `<p class="empty">${error.message}</p>`; }
}

async function pay(orderId) {
  try {
    await api(`/api/payments/${orderId}`, { method: 'POST' });
    toast('已发起模拟支付，请稍后刷新订单状态。');
    setTimeout(loadOrders, 2200);
  } catch (error) { toast(error.message); }
}

function renderAccount() {
  $('#user-name').textContent = state.profile?.username || '未登录';
  $('#auth-button').textContent = state.token ? '退出' : '登录';
}

async function restoreSession() {
  if (!state.token) return renderAccount();
  try { state.profile = await api('/api/users/profile'); } catch { state.token = ''; localStorage.removeItem('seck8s.token'); }
  renderAccount();
}

function openAuth() { $('#auth-dialog').showModal(); }
function logout() { state.token = ''; state.profile = null; localStorage.removeItem('seck8s.token'); renderAccount(); loadOrders(); toast('已退出登录'); }

function updateAuthMode() {
  const register = state.authMode === 'register';
  $('#auth-title').textContent = register ? '注册账号' : '登录';
  $('#auth-hint').textContent = register ? '注册后即可参加秒杀活动。' : '登录后可参与秒杀并管理订单。';
  $('#registration-fields').hidden = !register;
  $('#submit-auth').textContent = register ? '注册并登录' : '登录';
  $('#auth-mode-toggle').textContent = register ? '已有账号？登录' : '没有账号？注册';
}

$('#auth-button').addEventListener('click', () => state.token ? logout() : openAuth());
$('#settings-button').addEventListener('click', () => { $('#api-base').value = state.apiBase; $('#settings-dialog').showModal(); });
$('#auth-mode-toggle').addEventListener('click', () => { state.authMode = state.authMode === 'login' ? 'register' : 'login'; $('#auth-error').textContent = ''; updateAuthMode(); });
$('#auth-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const error = $('#auth-error'); error.textContent = '';
  const username = $('#username').value.trim(), password = $('#password').value;
  try {
    if (state.authMode === 'register') {
      await api('/api/users/register', { method: 'POST', body: JSON.stringify({ username, password, phone: $('#phone').value.trim(), realName: $('#real-name').value.trim(), idCard: $('#id-card').value.trim() }) });
    }
    const login = await api('/api/users/login', { method: 'POST', body: JSON.stringify({ username, password }) });
    state.token = login.token; localStorage.setItem('seck8s.token', state.token); state.profile = await api('/api/users/profile');
    $('#auth-dialog').close(); renderAccount(); loadOrders(); toast('登录成功');
  } catch (err) { error.textContent = err.message; }
});
$('#settings-form').addEventListener('submit', (event) => { event.preventDefault(); state.apiBase = $('#api-base').value.replace(/\/$/, ''); localStorage.setItem('seck8s.apiBase', state.apiBase); $('#settings-dialog').close(); loadTickets(); loadActivities(); toast('网关地址已更新'); });
$('#ticket-search').addEventListener('change', loadTickets);
$('#refresh-activities').addEventListener('click', loadActivities);
$('#refresh-orders').addEventListener('click', loadOrders);
document.addEventListener('click', (event) => { const button = event.target.closest('button'); if (!button) return; if (button.dataset.ticket) showTicketSkus(button.dataset.ticket); if (button.dataset.activity) seckill(button.dataset.activity, button.dataset.sku); if (button.dataset.pay) pay(button.dataset.pay); });

updateAuthMode();
restoreSession().then(() => { loadTickets(); loadActivities(); loadOrders(); });
