/**
 * auth.js — Login / Register page
 * Handles: tab switching, form submission, JWT storage, redirect
 */

// ===== TAB SWITCHING =====
function switchTab(tab) {
  document.getElementById('loginForm').style.display    = tab === 'login'    ? 'flex' : 'none';
  document.getElementById('registerForm').style.display = tab === 'register' ? 'flex' : 'none';
  document.getElementById('loginTab').classList.toggle('active',    tab === 'login');
  document.getElementById('registerTab').classList.toggle('active', tab === 'register');
  hideAlert('authAlert');
}

// ===== If already logged in, redirect to dashboard =====
document.addEventListener('DOMContentLoaded', () => {
  if (localStorage.getItem('jwt_token')) {
    window.location.href = '/dashboard.html';
  }

  // Check if URL says ?tab=register
  const params = new URLSearchParams(window.location.search);
  if (params.get('tab') === 'register') switchTab('register');
});

// ===== ALERT =====
function showAlert(id, message, type = 'error') {
  const el = document.getElementById(id);
  el.className = `alert alert-${type} show`;
  el.innerHTML = (type === 'error' ? '⚠️ ' : '✅ ') + message;
}

function hideAlert(id) {
  document.getElementById(id).className = 'alert';
}

// ===== LOGIN =====
async function handleLogin(event) {
  event.preventDefault();
  hideAlert('authAlert');

  const email    = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  if (!email || !password) {
    showAlert('authAlert', 'Please fill in all fields.', 'error');
    return;
  }

  setLoading('loginBtn', 'loginBtnText', 'loginBtnSpinner', true);

  try {
    const res  = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'N/A', email, password })
    });

    const data = await res.json();

    if (!res.ok) {
      showAlert('authAlert', data.error || 'Invalid email or password.', 'error');
      return;
    }

    // Save JWT & user info
    localStorage.setItem('jwt_token', data.token);
    localStorage.setItem('user', JSON.stringify({ email: data.email, name: data.name }));

    showAlert('authAlert', `Welcome back, ${data.name}! Redirecting...`, 'success');
    setTimeout(() => window.location.href = '/dashboard.html', 1000);

  } catch (err) {
    showAlert('authAlert', 'Network error. Is the Spring Boot server running?', 'error');
  } finally {
    setLoading('loginBtn', 'loginBtnText', 'loginBtnSpinner', false, 'Sign In');
  }
}

// ===== REGISTER =====
async function handleRegister(event) {
  event.preventDefault();
  hideAlert('authAlert');

  const name     = document.getElementById('regName').value.trim();
  const email    = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;

  if (!name || !email || !password) {
    showAlert('authAlert', 'Please fill in all fields.', 'error');
    return;
  }

  if (password.length < 6) {
    showAlert('authAlert', 'Password must be at least 6 characters.', 'error');
    return;
  }

  setLoading('registerBtn', 'registerBtnText', 'registerBtnSpinner', true);

  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password })
    });

    const data = await res.json();

    if (!res.ok) {
      showAlert('authAlert', data.error || 'Registration failed. Try again.', 'error');
      return;
    }

    localStorage.setItem('jwt_token', data.token);
    localStorage.setItem('user', JSON.stringify({ email: data.email, name: data.name }));

    showAlert('authAlert', `Account created! Welcome, ${data.name}! Redirecting...`, 'success');
    setTimeout(() => window.location.href = '/dashboard.html', 1000);

  } catch (err) {
    showAlert('authAlert', 'Network error. Is the Spring Boot server running?', 'error');
  } finally {
    setLoading('registerBtn', 'registerBtnText', 'registerBtnSpinner', false, 'Create Account');
  }
}

// ===== PASSWORD TOGGLE =====
function togglePassword(inputId, btn) {
  const input = document.getElementById(inputId);
  if (input.type === 'password') {
    input.type = 'text';
    btn.textContent = '🙈';
  } else {
    input.type = 'password';
    btn.textContent = '👁️';
  }
}

// ===== LOADING HELPER =====
function setLoading(btnId, textId, spinnerId, loading, originalText = '') {
  const btn = document.getElementById(btnId);
  btn.disabled = loading;
  document.getElementById(textId).style.display   = loading ? 'none' : 'inline';
  document.getElementById(spinnerId).style.display = loading ? 'inline-block' : 'none';
  if (!loading && originalText) document.getElementById(textId).textContent = originalText;
}
