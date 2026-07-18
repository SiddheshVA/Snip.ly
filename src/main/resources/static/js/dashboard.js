/**
 * dashboard.js — User Dashboard JavaScript
 * Handles: fetching URLs, displaying stats, creating URLs via modal, displaying analytics, deleting URLs
 */

const API = '';

// ===== Check Auth =====
function getToken() { return localStorage.getItem('jwt_token'); }
function getUser()  { return JSON.parse(localStorage.getItem('user') || 'null'); }

// Redirect to login if not logged in
if (!getToken()) {
  window.location.href = '/auth.html';
}

// ===== On Page Load =====
document.addEventListener('DOMContentLoaded', () => {
  const user = getUser();
  if (user && user.name) {
    document.getElementById('userGreeting').textContent = `Hi, ${user.name}!`;
  }
  loadDashboardData();
});

// ===== Alert Helpers =====
function showAlert(id, message, type = 'error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = `alert alert-${type} show`;
  el.innerHTML = (type === 'error' ? '⚠️ ' : '✅ ') + message;
}

function hideAlert(id) {
  const el = document.getElementById(id);
  if (el) el.className = 'alert';
}

// ===== Loading States =====
function setButtonLoading(btnId, textId, spinnerId, loading, originalText = '') {
  const btn = document.getElementById(btnId);
  const text = document.getElementById(textId);
  const spinner = document.getElementById(spinnerId);
  if (!btn) return;
  btn.disabled = loading;
  if (text) text.style.display = loading ? 'none' : 'inline';
  if (spinner) spinner.style.display = loading ? 'inline-block' : 'none';
  if (!loading && originalText && text) text.textContent = originalText;
}

// ===== LOAD DASHBOARD DATA =====
async function loadDashboardData() {
  const listEl = document.getElementById('urlList');
  try {
    const res = await fetch(`${API}/api/urls`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    });

    if (res.status === 401 || res.status === 403) {
      // Token expired or invalid
      logout();
      return;
    }

    const urls = await res.json();
    
    if (!res.ok) {
      showAlert('dashboardAlert', urls.error || 'Failed to load URLs.', 'error');
      listEl.innerHTML = '';
      return;
    }

    renderDashboard(urls);

  } catch (err) {
    showAlert('dashboardAlert', 'Network error. Is the server running?', 'error');
    listEl.innerHTML = '';
  }
}

// ===== RENDER DASHBOARD =====
function renderDashboard(urls) {
  const listEl = document.getElementById('urlList');
  
  // Update Stats
  const totalLinks = urls.length;
  let totalClicks = 0;
  let topLink = null;
  let maxClicks = -1;

  urls.forEach(url => {
    totalClicks += url.clickCount;
    if (url.clickCount > maxClicks) {
      maxClicks = url.clickCount;
      topLink = url;
    }
  });

  document.getElementById('statTotalLinks').textContent = totalLinks;
  document.getElementById('statTotalClicks').textContent = totalClicks;
  document.getElementById('statTopLink').textContent = topLink && maxClicks > 0 ? `${topLink.shortCode} (${maxClicks})` : 'None';

  // Empty State
  if (urls.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">🔗</div>
        <h3>No shortened links yet</h3>
        <p>Shorten your first link using the button above!</p>
      </div>
    `;
    return;
  }

  // Render list
  listEl.innerHTML = urls.map(url => {
    const expiresStr = url.expiresAt 
      ? new Date(url.expiresAt).toLocaleString() 
      : 'Never';

    const isExpired = url.expiresAt && new Date(url.expiresAt) < new Date();
    const expiryBadge = isExpired 
      ? '<span class="badge badge-error">Expired</span>' 
      : (url.expiresAt ? `<span class="badge badge-accent">Expires: ${expiresStr}</span>` : '');

    const aliasBadge = url.customAlias 
      ? '<span class="badge badge-accent">Custom Alias</span>' 
      : '';

    return `
      <div class="url-item" id="url-${url.shortCode}">
        <div class="url-item-left">
          <div class="url-title" title="${url.title || ''}">${url.title || 'Untitled Link'}</div>
          <a class="url-short" href="${url.shortUrl}" target="_blank" id="link-${url.shortCode}">${url.shortUrl}</a>
          <div class="url-original" title="${url.originalUrl}">${url.originalUrl}</div>
          <div class="url-meta">
            <span>Created: ${new Date(url.createdAt).toLocaleDateString()}</span>
            <span class="url-clicks">📈 ${url.clickCount} clicks</span>
            ${aliasBadge}
            ${expiryBadge}
          </div>
        </div>
        <div class="url-item-actions">
          <button class="btn btn-secondary btn-sm" onclick="copyToClipboard('${url.shortUrl}')">📋 Copy</button>
          <button class="btn btn-secondary btn-sm" onclick="showQrCode('${url.shortCode}', '${url.shortUrl}')">📱 QR</button>
          <button class="btn btn-secondary btn-sm" onclick="viewAnalytics('${url.shortCode}')">📊 Analytics</button>
          <button class="btn btn-danger btn-sm" onclick="deleteUrl('${url.shortCode}')">🗑️ Delete</button>
        </div>
      </div>
    `;
  }).join('');
}

// ===== MODAL CONTROLS =====
function openCreateModal() {
  hideAlert('createAlert');
  document.getElementById('modalLongUrl').value = '';
  document.getElementById('modalTitle').value = '';
  document.getElementById('modalAlias').value = '';
  document.getElementById('modalExpiry').value = '';
  document.getElementById('createModal').classList.add('show');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('show');
}

// ===== CREATE URL =====
async function createUrl(event) {
  event.preventDefault();
  hideAlert('createAlert');

  const originalUrl = document.getElementById('modalLongUrl').value.trim();
  const title = document.getElementById('modalTitle').value.trim();
  const customAlias = document.getElementById('modalAlias').value.trim();
  const expiryHours = document.getElementById('modalExpiry').value;

  if (!originalUrl) {
    showAlert('createAlert', 'Please enter a URL.', 'error');
    return;
  }

  setButtonLoading('createBtn', 'createBtnText', 'createBtnSpinner', true);

  const body = { originalUrl };
  if (title) body.title = title;
  if (customAlias) body.customAlias = customAlias;
  if (expiryHours) body.expiryHours = parseInt(expiryHours);

  try {
    const res = await fetch(`${API}/api/urls`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify(body)
    });

    const data = await res.json();

    if (!res.ok) {
      showAlert('createAlert', data.error || 'Failed to create short link.', 'error');
      return;
    }

    closeModal('createModal');
    showToast('✅ Short URL created successfully!');
    loadDashboardData();

  } catch (err) {
    showAlert('createAlert', 'Network error. Try again.', 'error');
  } finally {
    setButtonLoading('createBtn', 'createBtnText', 'createBtnSpinner', false, 'Create Link');
  }
}

// ===== DELETE URL =====
async function deleteUrl(shortCode) {
  if (!confirm(`Are you sure you want to delete the short link /${shortCode}? This action cannot be undone.`)) {
    return;
  }

  try {
    const res = await fetch(`${API}/api/urls/${shortCode}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    });

    if (res.status === 204) {
      showToast('🗑️ Link deleted successfully.');
      loadDashboardData();
    } else {
      const data = await res.json();
      showAlert('dashboardAlert', data.error || 'Failed to delete link.', 'error');
    }
  } catch (err) {
    showAlert('dashboardAlert', 'Network error. Failed to delete link.', 'error');
  }
}

// ===== VIEW ANALYTICS =====
async function viewAnalytics(shortCode) {
  const contentEl = document.getElementById('analyticsContent');
  contentEl.innerHTML = '<div style="text-align:center;padding:40px;"><span class="spinner"></span><p style="margin-top:12px;color:var(--text-secondary);">Loading analytics...</p></div>';
  document.getElementById('analyticsModal').classList.add('show');

  try {
    const res = await fetch(`${API}/api/urls/${shortCode}/analytics`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${getToken()}`
      }
    });

    const data = await res.json();

    if (!res.ok) {
      contentEl.innerHTML = `<div class="alert alert-error show">⚠️ ${data.error || 'Failed to load analytics.'}</div>`;
      return;
    }

    renderAnalytics(data);

  } catch (err) {
    contentEl.innerHTML = '<div class="alert alert-error show">⚠️ Network error loading analytics.</div>';
  }
}

// ===== SHOW QR CODE =====
function showQrCode(shortCode, shortUrl) {
  const imgEl = document.getElementById('qrCodeImg');
  const textEl = document.getElementById('qrCodeText');
  const dlLink = document.getElementById('qrDownloadLink');
  
  const qrUrl = `${API}/api/urls/${shortCode}/qr`;
  imgEl.src = qrUrl;
  textEl.textContent = shortUrl;
  dlLink.href = qrUrl;
  
  document.getElementById('qrModal').classList.add('show');
}

// Global reference to store active Chart instances so we can destroy them on reload
let activeCharts = [];

// ===== RENDER ANALYTICS =====
function renderAnalytics(data) {
  const contentEl = document.getElementById('analyticsContent');
  
  // Clean up any old charts to prevent canvas reuse errors
  activeCharts.forEach(chart => chart.destroy());
  activeCharts = [];

  if (data.totalClicks === 0) {
    contentEl.innerHTML = `
      <div class="analytics-grid" style="grid-template-columns:1fr;margin-bottom:20px;">
        <div class="analytics-stat">
          <div class="number">0</div>
          <div class="label">Total Clicks</div>
        </div>
      </div>
      <div class="empty-state" style="padding:40px 20px;">
        <div class="empty-icon">📈</div>
        <h3>No click data yet</h3>
        <p>Share your short link to start collecting statistics!</p>
      </div>
    `;
    return;
  }

  contentEl.innerHTML = `
    <div class="analytics-grid" style="grid-template-columns: 1fr; margin-bottom:20px;">
      <div class="analytics-stat">
        <div class="number" style="font-size:2.5rem;font-weight:700;color:var(--accent);">${data.totalClicks}</div>
        <div class="label">Total Clicks</div>
      </div>
    </div>
    <div style="max-height: 440px; overflow-y: auto; padding-right:8px; display:flex; flex-direction:column; gap:32px;">
      <!-- Chart Containers -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap:20px;">
        <div>
          <h4 style="font-size:0.85rem;text-align:center;margin-bottom:12px;color:var(--text-secondary);">Devices</h4>
          <div style="position:relative; height:180px;"><canvas id="deviceChart"></canvas></div>
        </div>
        <div>
          <h4 style="font-size:0.85rem;text-align:center;margin-bottom:12px;color:var(--text-secondary);">Browsers</h4>
          <div style="position:relative; height:180px;"><canvas id="browserChart"></canvas></div>
        </div>
      </div>
      <div>
        <h4 style="font-size:0.85rem;margin-bottom:12px;color:var(--text-secondary);">Countries</h4>
        <div style="position:relative; height:180px;"><canvas id="countryChart"></canvas></div>
      </div>
    </div>
  `;

  // Colors mapping (Sleek curated theme)
  const colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
  const textClr = getComputedStyle(document.body).getPropertyValue('--text-primary').trim() || '#1f2937';

  // 1. Devices Chart (Doughnut)
  const deviceCtx = document.getElementById('deviceChart').getContext('2d');
  const deviceLabels = Object.keys(data.clicksByDevice);
  const deviceValues = Object.values(data.clicksByDevice);
  activeCharts.push(new Chart(deviceCtx, {
    type: 'doughnut',
    data: {
      labels: deviceLabels,
      datasets: [{
        data: deviceValues,
        backgroundColor: colors.slice(0, deviceLabels.length),
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, color: textClr, font: { size: 10 } } }
      }
    }
  }));

  // 2. Browsers Chart (Doughnut)
  const browserCtx = document.getElementById('browserChart').getContext('2d');
  const browserLabels = Object.keys(data.clicksByBrowser);
  const browserValues = Object.values(data.clicksByBrowser);
  activeCharts.push(new Chart(browserCtx, {
    type: 'doughnut',
    data: {
      labels: browserLabels,
      datasets: [{
        data: browserValues,
        backgroundColor: colors.slice(2, 2 + browserLabels.length),
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, color: textClr, font: { size: 10 } } }
      }
    }
  }));

  // 3. Countries Chart (Horizontal Bar)
  const countryCtx = document.getElementById('countryChart').getContext('2d');
  const countryLabels = Object.keys(data.clicksByCountry);
  const countryValues = Object.values(data.clicksByCountry);
  activeCharts.push(new Chart(countryCtx, {
    type: 'bar',
    data: {
      labels: countryLabels,
      datasets: [{
        label: 'Clicks',
        data: countryValues,
        backgroundColor: '#6366f1',
        borderRadius: 4
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false }, ticks: { color: textClr, stepSize: 1 } },
        y: { grid: { display: false }, ticks: { color: textClr } }
      }
    }
  }));
}

// ===== COPY TO CLIPBOARD =====
async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
    showToast('✅ Link copied to clipboard!');
  } catch {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    showToast('✅ Link copied to clipboard!');
  }
}

function showToast(msg) {
  const toast = document.getElementById('copyToast');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 2500);
}

// ===== LOGOUT =====
function logout() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('user');
  window.location.href = '/auth.html';
}

// ===== INSTANT QR GENERATION =====
function generateInstantQr() {
  const urlInput = document.getElementById('instantQrUrl');
  const previewArea = document.getElementById('instantQrPreviewArea');
  const previewImg = document.getElementById('instantQrPreviewImg');
  const downloadBtn = document.getElementById('instantQrDownloadBtn');

  const rawUrl = urlInput.value.trim();
  if (!rawUrl) {
    alert("Please enter a valid URL.");
    return;
  }

  // Generate target API URL
  const targetUrl = `${API}/api/public/qr?url=${encodeURIComponent(rawUrl)}`;

  // Update preview image source and download link
  previewImg.src = targetUrl;
  downloadBtn.href = targetUrl;

  // Show the preview area
  previewArea.style.display = 'flex';
}
