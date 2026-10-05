const form = document.querySelector('#signup-form');
if (form) form.addEventListener('submit', async event => {
 event.preventDefault();
 const button = form.querySelector('button');
 if (button.disabled) return;
 const status = document.querySelector('#status');
 const values = Object.fromEntries(new FormData(form));
 if (values.password !== values.confirmation) { status.textContent = 'Passwords do not match.'; return; }
 button.disabled = true; status.textContent = 'Creating account…';
 try {
  const response = await fetch('/req/signup', {method:'POST', headers:{'Content-Type':'application/json', [document.querySelector('meta[name="_csrf_header"]').content]:document.querySelector('meta[name="_csrf"]').content}, body:JSON.stringify({username:values.username,email:values.email,password:values.password})});
  if (response.ok) { form.reset(); status.textContent = 'Account created. You can sign in now.'; }
  else { status.textContent = response.status === 409 ? 'That username is unavailable.' : 'Could not create account. Check your details and try again.'; }
 } catch { status.textContent = 'Connection failed. Please try again.'; }
 finally {button.disabled = false;}
});
