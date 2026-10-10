package com.cadence.songs.server

/** Page web servie à l'ordinateur : gérer la bibliothèque depuis un navigateur sur le même Wi-Fi. */
object WebUi {
    val HTML: String = """<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Cadence sur PC</title>
<style>
:root{
  --bg:#faf8f5;--card:#f4f1ed;--card2:#ebe8f1;--ink:#1c1b22;--muted:#5d5c68;--line:#d9d5de;
  --primary:#4f46e5;--on-primary:#fff;--accent:#0d9488;--danger:#ba1a1a;--radius:16px;
}
@media (prefers-color-scheme:dark){
  :root{--bg:#0e1015;--card:#14161d;--card2:#1f232c;--ink:#e7e5ec;--muted:#a5a4b0;--line:#33353f;
    --primary:#a9acff;--on-primary:#1b1a6b;--accent:#5eead4;--danger:#ffb4ab}
}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.45 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
button,input,textarea,select{font:inherit;color:inherit}
header{display:flex;align-items:center;gap:12px;padding:14px 24px;border-bottom:1px solid var(--line);background:var(--card)}
header h1{font-size:20px;margin:0;letter-spacing:-.2px}
header .dot{width:34px;height:34px;border-radius:11px;background:var(--primary);color:var(--on-primary);display:grid;place-items:center;font-weight:700}
header .sp{flex:1}
.btn{border:0;border-radius:999px;padding:9px 18px;background:var(--card2);cursor:pointer;font-weight:600}
.btn:hover{filter:brightness(.96)}
.btn.primary{background:var(--primary);color:var(--on-primary)}
.btn.danger{color:var(--danger)}
.btn:disabled{opacity:.5;cursor:default}
main{display:grid;grid-template-columns:minmax(280px,380px) 1fr;height:calc(100vh - 63px)}
aside{border-right:1px solid var(--line);display:flex;flex-direction:column;min-height:0}
aside .tools{padding:14px;display:flex;gap:8px}
input[type=text],input[type=search],input[type=number],input[type=password],textarea,select{
  width:100%;border:1px solid var(--line);background:var(--bg);border-radius:12px;padding:10px 12px}
input:focus,textarea:focus,select:focus{outline:2px solid var(--primary);outline-offset:-1px}
#list{overflow:auto;padding:0 14px 14px;flex:1}
.song{display:flex;gap:12px;align-items:center;padding:10px 12px;border-radius:14px;background:var(--card);margin-bottom:8px;cursor:pointer;border:2px solid transparent}
.song.sel{border-color:var(--primary)}
.badge{min-width:42px;height:42px;border-radius:13px;background:var(--card2);display:grid;place-items:center;font-weight:700;font-size:14px}
.song .t{font-weight:600}.song .a{color:var(--muted);font-size:14px}
#editor{overflow:auto;padding:24px;min-height:0}
.form{max-width:820px;margin:0 auto;display:grid;gap:14px}
.row{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}
@media(max-width:900px){main{grid-template-columns:1fr;height:auto}.row{grid-template-columns:1fr 1fr}aside{border-right:0}}
label{display:grid;gap:5px;font-size:13px;font-weight:600;color:var(--muted)}
textarea#content{min-height:380px;font:15px/1.5 ui-monospace,Menlo,Consolas,monospace;white-space:pre;overflow:auto}
.hint{color:var(--muted);font-size:13px;margin:0}
.actions{display:flex;gap:10px;flex-wrap:wrap;align-items:center}
.empty{display:grid;place-items:center;height:100%;color:var(--muted);text-align:center;padding:30px}
#login{max-width:360px;margin:12vh auto;padding:28px;background:var(--card);border-radius:24px;display:grid;gap:14px;text-align:center}
#login input{font-size:28px;text-align:center;letter-spacing:10px}
.msg{min-height:20px;font-size:14px}.msg.err{color:var(--danger)}.msg.ok{color:var(--accent)}
.drop{border:2px dashed var(--line);border-radius:var(--radius);padding:14px;text-align:center;color:var(--muted)}
.drop.over{border-color:var(--primary);background:var(--card)}
.pdfbox{display:flex;gap:10px;align-items:center;flex-wrap:wrap;background:var(--card);padding:12px 14px;border-radius:14px}
.hidden{display:none!important}
</style>
</head>
<body>
<div id="login" class="hidden">
  <div class="dot" style="margin:auto;width:54px;height:54px;border-radius:17px;background:var(--primary);color:var(--on-primary);display:grid;place-items:center;font-weight:700;font-size:26px">C</div>
  <h1 style="margin:0">Cadence sur PC</h1>
  <p class="hint">Entrez le code à 4 chiffres affiché dans l'application, sur l'écran « Transfert Wi-Fi ».</p>
  <input id="pin" type="password" inputmode="numeric" maxlength="4" autocomplete="off" placeholder="••••">
  <button class="btn primary" id="go">Se connecter</button>
  <div id="loginMsg" class="msg err"></div>
</div>

<div id="app" class="hidden">
  <header>
    <div class="dot">C</div><h1>Cadence sur PC</h1><span class="sp"></span>
    <span id="count" class="hint"></span>
    <button class="btn" id="quit">Se déconnecter</button>
  </header>
  <main>
    <aside>
      <div class="tools">
        <input id="q" type="search" placeholder="Rechercher…">
        <button class="btn primary" id="new">+ Nouveau</button>
      </div>
      <div style="padding:0 14px 10px">
        <div class="drop" id="drop">Glissez ici des fichiers <b>.pdf</b>, <b>.txt</b> ou <b>.cho</b> pour les ajouter</div>
      </div>
      <div id="list"></div>
    </aside>
    <section id="editor"></section>
  </main>
</div>

<script>
const ${'$'} = s => document.querySelector(s);
let PIN = sessionStorage.getItem('pin') || '';
let songs = [], current = null, dirty = false;

function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}

async function api(path, opts={}){
  const headers = Object.assign({'X-Pin':PIN}, opts.headers||{});
  const r = await fetch('/api/'+path, Object.assign({}, opts, {headers}));
  if (r.status === 401){ showLogin('Code incorrect.'); throw new Error('401'); }
  const text = await r.text();
  let data = null; try { data = JSON.parse(text); } catch(e) {}
  if (!r.ok) throw new Error((data && data.error) || ('Erreur '+r.status));
  return data;
}

function showLogin(msg){
  ${'$'}('#app').classList.add('hidden'); ${'$'}('#login').classList.remove('hidden');
  ${'$'}('#loginMsg').textContent = msg||''; ${'$'}('#pin').value=''; ${'$'}('#pin').focus();
}
async function login(){
  PIN = ${'$'}('#pin').value.trim();
  try { await api('ping'); sessionStorage.setItem('pin', PIN); start(); } catch(e){ /* géré dans api */ }
}
${'$'}('#go').onclick = login;
${'$'}('#pin').addEventListener('keydown', e=>{ if(e.key==='Enter') login(); });
${'$'}('#quit').onclick = ()=>{ sessionStorage.removeItem('pin'); PIN=''; showLogin(''); };

async function start(){
  ${'$'}('#login').classList.add('hidden'); ${'$'}('#app').classList.remove('hidden');
  await refresh(); if (!current) renderEmpty();
}
async function refresh(){
  songs = await api('songs');
  renderList();
}
function renderList(){
  const q = ${'$'}('#q').value.trim().toLowerCase();
  const items = songs.filter(s=>!q || (s.title+' '+s.artist+' '+s.tags).toLowerCase().includes(q))
    .sort((a,b)=>a.title.localeCompare(b.title,'fr',{sensitivity:'base'}));
  ${'$'}('#count').textContent = songs.length + ' morceau' + (songs.length>1?'x':'');
  ${'$'}('#list').innerHTML = items.map(s=>
    '<div class="song'+(current&&current.id===s.id?' sel':'')+'" data-id="'+s.id+'">'+
    '<div class="badge">'+esc(s.key|| (s.hasPdf?'PDF':'♪'))+'</div>'+
    '<div><div class="t">'+esc(s.title||'Sans titre')+'</div><div class="a">'+esc(s.artist)+'</div></div></div>').join('')
    || '<p class="hint" style="padding:10px">Aucun morceau.</p>';
  document.querySelectorAll('.song').forEach(el=>el.onclick=()=>openSong(+el.dataset.id));
}
${'$'}('#q').oninput = renderList;

function renderEmpty(){
  current = null;
  ${'$'}('#editor').innerHTML = '<div class="empty"><div><h2>Choisissez un morceau</h2><p>ou cliquez sur « + Nouveau » pour en créer un.<br>Les modifications apparaissent tout de suite sur l\'appareil.</p></div></div>';
  renderList();
}
async function openSong(id){
  if (dirty && !confirm('Abandonner les modifications non enregistrées ?')) return;
  const s = await api('songs/'+id);
  showEditor(s);
}
function newSong(){
  if (dirty && !confirm('Abandonner les modifications non enregistrées ?')) return;
  showEditor({id:0,title:'',artist:'',key:'',tempo:0,timeSig:'4/4',capo:0,tags:'',notes:'',content:'',hasPdf:false});
}
${'$'}('#new').onclick = newSong;

function showEditor(s){
  current = s; dirty = false;
  const pdf = s.id ? (s.hasPdf
    ? '<div class="pdfbox"><b>Partition PDF jointe</b><a class="btn" target="_blank" href="/api/songs/'+s.id+'/pdf?pin='+encodeURIComponent(PIN)+'">Ouvrir</a>'+
      '<label class="btn" style="display:inline-block;color:var(--ink)">Remplacer<input id="pdfin" type="file" accept="application/pdf" class="hidden"></label>'+
      '<button class="btn danger" id="pdfdel">Retirer le PDF</button></div>'
    : '<div class="pdfbox"><b>Partition PDF</b><label class="btn" style="display:inline-block;color:var(--ink)">Joindre un PDF<input id="pdfin" type="file" accept="application/pdf" class="hidden"></label></div>')
    : '<p class="hint">Enregistrez d\'abord le morceau pour pouvoir y joindre un PDF.</p>';
  ${'$'}('#editor').innerHTML =
  '<div class="form">'+
  '<div class="row" style="grid-template-columns:2fr 2fr">'+
    '<label>Titre<input type="text" id="title" value="'+esc(s.title)+'"></label>'+
    '<label>Artiste<input type="text" id="artist" value="'+esc(s.artist)+'"></label></div>'+
  '<div class="row">'+
    '<label>Tonalité<input type="text" id="key" value="'+esc(s.key)+'" placeholder="Ex. G, Am"></label>'+
    '<label>Tempo (bpm)<input type="number" id="tempo" min="0" max="400" value="'+(s.tempo||'')+'"></label>'+
    '<label>Mesure<input type="text" id="timeSig" value="'+esc(s.timeSig||'4/4')+'"></label>'+
    '<label>Capo<input type="number" id="capo" min="0" max="11" value="'+(s.capo||0)+'"></label></div>'+
  '<label>Tags (séparés par des virgules)<input type="text" id="tags" value="'+esc(s.tags)+'"></label>'+
  '<label>Paroles et accords (accords entre crochets : [C]Bonjour [G]toi)<textarea id="content" spellcheck="false">'+esc(s.content)+'</textarea></label>'+
  '<div class="actions"><button class="btn" id="conv">Convertir « accords au-dessus des paroles »</button>'+
    '<span class="hint">Collez un texte avec les accords sur la ligne du dessus, puis convertissez.</span></div>'+
  '<label>Notes<textarea id="notes" style="min-height:70px">'+esc(s.notes)+'</textarea></label>'+
  pdf+
  '<div class="actions"><button class="btn primary" id="save">Enregistrer</button>'+
    (s.id?'<button class="btn danger" id="del">Supprimer le morceau</button>':'')+
    '<span id="msg" class="msg"></span></div></div>';
  document.querySelectorAll('#editor input:not([type=file]),#editor textarea').forEach(el=>el.addEventListener('input',()=>{dirty=true}));
  ${'$'}('#save').onclick = save;
  ${'$'}('#conv').onclick = convert;
  if (${'$'}('#del')) ${'$'}('#del').onclick = del;
  if (${'$'}('#pdfin')) ${'$'}('#pdfin').onchange = e=>uploadPdf(s.id, e.target.files[0]);
  if (${'$'}('#pdfdel')) ${'$'}('#pdfdel').onclick = async()=>{ if (dirty) await save(); await api('songs/'+s.id+'/pdf',{method:'DELETE'}); await refresh(); openSong(s.id); };
  renderList();
}
function say(text, cls){ const m=${'$'}('#msg'); if(m){ m.textContent=text; m.className='msg '+(cls||''); } }
function formData(){
  return {title:${'$'}('#title').value,artist:${'$'}('#artist').value,key:${'$'}('#key').value,tempo:+${'$'}('#tempo').value||0,
    timeSig:${'$'}('#timeSig').value,capo:+${'$'}('#capo').value||0,tags:${'$'}('#tags').value,notes:${'$'}('#notes').value,content:${'$'}('#content').value};
}
async function save(){
  const body = JSON.stringify(formData());
  try{
    const saved = current.id
      ? await api('songs/'+current.id,{method:'PUT',body})
      : await api('songs',{method:'POST',body});
    dirty = false; await refresh(); showEditor(saved); say('Enregistré sur l\'appareil ✓','ok');
  }catch(e){ say(e.message,'err'); }
}
async function del(){
  if(!confirm('Supprimer définitivement « '+(current.title||'Sans titre')+' » ?')) return;
  await api('songs/'+current.id,{method:'DELETE'}); dirty=false; await refresh(); renderEmpty();
}
async function convert(){
  try{
    const r = await api('convert',{method:'POST',body:JSON.stringify({text:${'$'}('#content').value})});
    ${'$'}('#content').value = r.content; if(!${'$'}('#key').value) ${'$'}('#key').value = r.key||''; dirty=true; say('Converti ✓','ok');
  }catch(e){ say(e.message,'err'); }
}
async function uploadPdf(id, file){
  if(!file) return;
  try{
    if (dirty) await save();
    await api('songs/'+id+'/pdf',{method:'POST',headers:{'X-Filename':encodeURIComponent(file.name),'Content-Type':'application/pdf'},body:file});
    await refresh(); openSong(id);
  }catch(e){ say(e.message,'err'); }
}

// Glisser-déposer : ajoute un morceau par fichier
const drop = ${'$'}('#drop');
['dragenter','dragover'].forEach(t=>drop.addEventListener(t,e=>{e.preventDefault();drop.classList.add('over')}));
['dragleave','drop'].forEach(t=>drop.addEventListener(t,e=>{e.preventDefault();drop.classList.remove('over')}));
drop.addEventListener('drop', async e=>{
  let n=0, bad=0;
  for (const f of e.dataTransfer.files){
    const base = f.name.replace(/\.[^.]+${'$'}/,'');
    try{
      if (/\.pdf${'$'}/i.test(f.name)){
        const s = await api('songs',{method:'POST',body:JSON.stringify({title:base})});
        await api('songs/'+s.id+'/pdf',{method:'POST',headers:{'X-Filename':encodeURIComponent(f.name),'Content-Type':'application/pdf'},body:f});
      } else {
        await api('import',{method:'POST',body:JSON.stringify({title:base,text:await f.text()})});
      }
      n++;
    }catch(err){ bad++; }
  }
  await refresh();
  drop.textContent = n+' fichier(s) ajouté(s)'+(bad?(' — '+bad+' refusé(s)'):'');
  setTimeout(()=>{drop.innerHTML='Glissez ici des fichiers <b>.pdf</b>, <b>.txt</b> ou <b>.cho</b> pour les ajouter'},4000);
});
window.addEventListener('beforeunload', e=>{ if(dirty){ e.preventDefault(); e.returnValue=''; } });

if (PIN) { api('ping').then(start).catch(()=>showLogin('')); } else showLogin('');
</script>
</body>
</html>
"""
}
