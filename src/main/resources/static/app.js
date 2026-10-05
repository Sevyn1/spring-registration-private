let csrf;
const select = id => document.getElementById(id);
const feedback = message => { select('feedback').textContent = message; };
function show(view) {
 for (const id of ['register','login','profile']) select(id).hidden = id !== view;
 select('show-register').classList.toggle('active',view === 'register');
 select('show-login').classList.toggle('active',view === 'login');
 select('show-register').parentElement.hidden = view === 'profile';
}
async function refreshToken() {
 const response = await fetch('/api/csrf', {cache:'no-store'});
 if (!response.ok) throw new Error('Could not start a session. Reload to try again.');
 csrf = await response.json();
}
async function mutate(url, body, type) {
 if (!csrf) await refreshToken();
 const response = await fetch(url, {method:'POST', headers:{[csrf.headerName]:csrf.token,...(type?{'Content-Type':type}:{})},body});
 if (response.status === 403) { await refreshToken(); throw new Error('Session changed. Please submit again.'); }
 if (!response.ok) {
  let message = 'Request failed. Please try again.';
  try { message = (await response.json()).message || message; } catch { /* Retain a generic error for non-JSON responses. */ }
  throw new Error(message);
 }
 return response;
}
async function loadProfile() {
 const response = await fetch('/api/me', {cache:'no-store'});
 if (response.status === 401) return false;
 if (!response.ok) throw new Error('Could not load your account.');
 const account = await response.json();
 select('greeting').textContent = `Hello, ${account.displayName}.`;
 select('account-name').textContent = account.username;
 select('account-date').textContent = new Date(account.createdAt).toLocaleString();
 show('profile'); return true;
}
async function busy(button, action) {
 if (button.disabled) return;
 button.disabled = true;
 try { await action(); } catch (error) { feedback(error instanceof TypeError ? 'Connection failed. Check the server and try again.' : error.message); }
 finally {button.disabled = false;}
}
select('show-register').addEventListener('click',()=>{show('register');feedback('');});
select('show-login').addEventListener('click',()=>{show('login');feedback('');});
select('register').addEventListener('submit',event=>{
 event.preventDefault(); const form=event.currentTarget; const values=Object.fromEntries(new FormData(form));
 if(values.password !== values.confirmation){feedback('Passwords do not match.');return;}
 if(new TextEncoder().encode(values.password).length>72){feedback('Password must fit within 72 UTF-8 bytes.');return;}
 busy(form.querySelector('button'),async()=>{
  await mutate('/api/accounts',JSON.stringify({username:values.username,displayName:values.displayName,password:values.password}),'application/json');
  select('login').elements.username.value=values.username;form.reset();show('login');feedback('Account created. Sign in to continue.');
 });
});
select('login').addEventListener('submit',event=>{
 event.preventDefault();const form=event.currentTarget;
 busy(form.querySelector('button'),async()=>{
  await mutate('/api/session',new URLSearchParams(new FormData(form)).toString(),'application/x-www-form-urlencoded');
  form.elements.password.value='';await refreshToken();
  if(await loadProfile()) feedback('Signed in successfully.'); else {show('login');feedback('Session expired. Please sign in again.');}
 });
});
select('logout').addEventListener('click',event=>busy(event.currentTarget,async()=>{
 await mutate('/api/session/logout');await refreshToken();select('greeting').textContent='';select('account-name').textContent='';select('account-date').textContent='';show('login');feedback('Signed out.');
}));
(async()=>{try{await refreshToken();await loadProfile();}catch(error){feedback(error.message);}})();
