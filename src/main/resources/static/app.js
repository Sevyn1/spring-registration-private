let csrf;
const select = id => document.getElementById(id);
const feedback = (message, error=false) => { select('feedback').textContent=message; select('feedback').classList.toggle('error',error); if(message) select('feedback').scrollIntoView({block:'nearest'}); };
function show(view) {
 document.body.classList.toggle('account-active',view==='profile');
 for(const id of ['register','login','profile']) select(id).hidden=id!==view;
 select('show-register').classList.toggle('active',view==='register');
 select('show-login').classList.toggle('active',view==='login');
 select('auth-tabs').hidden=view==='profile';
}
function settings(view) {
 for(const id of ['details','security']) select(id).hidden=id!==view;
 select('show-details').classList.toggle('active',view==='details');
 select('show-security').classList.toggle('active',view==='security');
 select('details').elements.currentPassword.value='';select('security').reset();feedback('');
}
async function request(url,options={}) {
 const controller=new AbortController();const timer=setTimeout(()=>controller.abort(),12000);
 try{return await fetch(url,{...options,signal:controller.signal});}
 catch(error){throw new Error(error.name==='AbortError'?'The request took too long. Please try again.':'Connection failed. Check the server and try again.');}
 finally{clearTimeout(timer);}
}
async function refreshToken() {
 const response=await request('/api/csrf',{cache:'no-store'});
 if(!response.ok) throw new Error('Could not start a session. Reload to try again.');csrf=await response.json();
}
async function mutate(url,body,type,method='POST') {
 if(!csrf) await refreshToken();
 const response=await request(url,{method,headers:{[csrf.headerName]:csrf.token,...(type?{'Content-Type':type}:{})},body});
 if(response.status===401){clearProfile();show('login');throw new Error('Your session ended. Sign in again.');}
 if(response.status===403){await refreshToken();throw new Error('Session changed. Please submit again.');}
 if(!response.ok){let message='Request failed. Please try again.';try{message=(await response.json()).message||message;}catch{}throw new Error(message);}
 return response;
}
function display(account) {
 select('greeting').textContent=`Hello, ${account.displayName}.`;
 select('account-name').textContent=account.username;
 select('account-date').textContent=new Date(account.createdAt).toLocaleDateString(undefined,{year:'numeric',month:'short',day:'numeric'});
 select('details').elements.displayName.value=account.displayName;
 select('details').elements.email.value=account.email||'';
}
function clearProfile() {
 for(const id of ['greeting','account-name','account-date']) select(id).textContent='';
 select('details').reset();select('security').reset();
}
async function loadProfile() {
 const response=await request('/api/me',{cache:'no-store'});
 if(response.status===401){clearProfile();return false;}
 if(!response.ok)throw new Error('Could not load your account.');
 display(await response.json());settings('details');show('profile');return true;
}
async function busy(button,action) {
 if(button.disabled)return;button.disabled=true;feedback('');
 try{await action();}catch(error){feedback(error.message,true);}finally{button.disabled=false;}
}
function validPassword(password,confirmation){
 if(password!==confirmation){feedback('Passwords do not match.',true);return false;}
 if(new TextEncoder().encode(password).length>72){feedback('Password must fit within 72 UTF-8 bytes.',true);return false;}return true;
}
select('show-register').addEventListener('click',()=>{show('register');feedback('');});
select('show-login').addEventListener('click',()=>{show('login');feedback('');});
select('show-details').addEventListener('click',()=>settings('details'));
select('show-security').addEventListener('click',()=>settings('security'));
select('register').addEventListener('submit',event=>{
 event.preventDefault();const form=event.currentTarget;const v=Object.fromEntries(new FormData(form));
 if(!validPassword(v.password,v.confirmation))return;
 busy(form.querySelector('button'),async()=>{
  await mutate('/api/accounts',JSON.stringify({username:v.username,displayName:v.displayName,email:v.email,password:v.password}),'application/json');
  select('login').elements.username.value=v.username;form.reset();show('login');feedback('Account created. Sign in to continue.');
 });
});
select('login').addEventListener('submit',event=>{
 event.preventDefault();const form=event.currentTarget;
 busy(form.querySelector('button'),async()=>{
  await mutate('/api/session',new URLSearchParams(new FormData(form)).toString(),'application/x-www-form-urlencoded');
  form.elements.password.value='';await refreshToken();
  if(await loadProfile())feedback('Signed in successfully.');else{show('login');feedback('Session expired. Please sign in again.',true);}
 });
});
select('details').addEventListener('submit',event=>{
 event.preventDefault();const form=event.currentTarget;
 busy(form.querySelector('button'),async()=>{
  const response=await mutate('/api/me',JSON.stringify(Object.fromEntries(new FormData(form))),'application/json','PATCH');
  display(await response.json());form.elements.currentPassword.value='';feedback('Your details have been saved.');
 });
});
select('security').addEventListener('submit',event=>{
 event.preventDefault();const form=event.currentTarget;const v=Object.fromEntries(new FormData(form));
 if(!validPassword(v.newPassword,v.confirmation))return;
 busy(form.querySelector('button'),async()=>{
  await mutate('/api/me/password',JSON.stringify({currentPassword:v.currentPassword,newPassword:v.newPassword}),'application/json');
  const username=select('account-name').textContent;clearProfile();select('login').elements.username.value=username;
  select('login').elements.password.value='';show('login');await refreshToken();feedback('Password updated. Sign in with your new password.');
 });
});
select('logout').addEventListener('click',event=>busy(event.currentTarget,async()=>{
 await mutate('/api/session/logout');clearProfile();select('login').elements.password.value='';show('login');await refreshToken();feedback('Signed out.');
}));
(async()=>{try{await refreshToken();await loadProfile();}catch(error){feedback(error.message,true);}})();
