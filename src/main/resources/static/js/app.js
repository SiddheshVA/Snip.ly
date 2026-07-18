/**
 * app.js — Landing Page JavaScript
 * Handles: URL shortening form, auth state, copy-to-clipboard
 */

const API = '';  // Empty = same origin (Spring Boot serves both)

// ===== Auth State =====
function getToken()    { return localStorage.getItem('jwt_token'); }
function getUser()     { return JSON.parse(localStorage.getItem('user') || 'null'); }
function isLoggedIn()  { return !!getToken(); }

// ===== On Page Load =====
document.addEventListener('DOMContentLoaded', () => {
  updateNavbar();
  handleUrlParams();
  if (isLoggedIn()) {
    document.getElementById('optionsRow').style.display = 'grid';
    document.getElementById('loginNudge').style.display = 'none';
  }
});

function updateNavbar() {
  const loginBtn     = document.getElementById('navLoginBtn');
  const dashBtn      = document.getElementById('navDashboardBtn');
  const logoutBtn    = document.getElementById('navLogoutBtn');

  if (!loginBtn) return;

  if (isLoggedIn()) {
    loginBtn.style.display    = 'none';
    dashBtn.style.display     = 'inline-flex';
    logoutBtn.style.display   = 'inline-flex';
  } else {
    loginBtn.style.display    = 'inline-flex';
    dashBtn.style.display     = 'none';
    logoutBtn.style.display   = 'none';
  }
}

function logout() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('user');
  window.location.href = '/';
}

// ===== Show error/success messages =====
function showAlert(id, message, type = 'error') {
  const el = document.getElementById(id);
  if (!el) return;
  el.className = `alert alert-${type} show`;
  el.innerHTML = (type === 'error' ? '⚠️ ' : '✅ ') + message;
  if (type === 'success') setTimeout(() => el.className = 'alert', 4000);
}

function hideAlert(id) {
  const el = document.getElementById(id);
  if (el) el.className = 'alert';
}

// ===== Handle URL query params (e.g. ?error=not-found) =====
function handleUrlParams() {
  const params = new URLSearchParams(window.location.search);
  if (params.get('error') === 'not-found') {
    showAlert('homeAlert', 'That short link was not found or has been deleted.', 'error');
  }
  if (params.get('error') === 'expired') {
    showAlert('homeAlert', 'That short link has expired.', 'error');
  }
}

// ===== SHORTEN URL =====
async function shortenUrl() {
  const urlInput  = document.getElementById('longUrlInput');
  const url       = urlInput.value.trim();

  if (!url) {
    showAlert('homeAlert', 'Please paste a URL first.', 'error');
    urlInput.focus();
    return;
  }

  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    showAlert('homeAlert', 'URL must start with http:// or https://', 'error');
    return;
  }

  hideAlert('homeAlert');
  setLoading('shortenBtn', 'shortenBtnText', 'shortenBtnSpinner', true);

  const body = { originalUrl: url };

  if (isLoggedIn()) {
    const alias  = document.getElementById('customAlias')?.value.trim();
    const expiry = document.getElementById('expiryHours')?.value;
    if (alias)  body.customAlias = alias;
    if (expiry) body.expiryHours = parseInt(expiry);
  }

  try {
    const headers = { 'Content-Type': 'application/json' };
    if (isLoggedIn()) headers['Authorization'] = `Bearer ${getToken()}`;

    const res  = await fetch(`${API}/api/urls`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    });

    const data = await res.json();

    if (!res.ok) {
      showAlert('homeAlert', data.error || 'Failed to shorten URL. Try again.', 'error');
      return;
    }

    // Show result
    const resultBox  = document.getElementById('resultBox');
    const resultUrl  = document.getElementById('resultUrl');
    const resultLink = document.getElementById('resultLink');

    resultUrl.textContent    = data.shortUrl;
    resultLink.href          = data.shortUrl;
    resultBox.classList.add('show');

    urlInput.value = '';
    if (document.getElementById('customAlias')) document.getElementById('customAlias').value = '';

  } catch (err) {
    showAlert('homeAlert', 'Network error. Is the server running?', 'error');
  } finally {
    setLoading('shortenBtn', 'shortenBtnText', 'shortenBtnSpinner', false, 'Shorten');
  }
}

// Allow Enter key on URL input
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('longUrlInput')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') shortenUrl();
  });
});

// ===== COPY TO CLIPBOARD =====
async function copyUrl() {
  const url = document.getElementById('resultUrl')?.textContent;
  if (!url) return;
  try {
    await navigator.clipboard.writeText(url);
    showToast();
  } catch {
    // Fallback
    const ta = document.createElement('textarea');
    ta.value = url;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    showToast();
  }
}

function showToast(msg = '✅ Copied to clipboard!') {
  const toast = document.getElementById('copyToast');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 2500);
}

// ===== LOADING STATE HELPER =====
function setLoading(btnId, textId, spinnerId, loading, originalText = '') {
  const btn     = document.getElementById(btnId);
  const text    = document.getElementById(textId);
  const spinner = document.getElementById(spinnerId);
  if (!btn) return;
  btn.disabled        = loading;
  text.style.display  = loading ? 'none' : 'inline';
  spinner.style.display = loading ? 'inline-block' : 'none';
  if (!loading && originalText) text.textContent = originalText;
}
