(()=>{
  const VERSION='1.6.0';
  const marks=['XXI BANORTE','PROFUTURO','PENSIONISSSTE','SURA','BANAMEX','PRINCIPAL','COPPEL','INBURSA','INVERCAP','AZTECA'];
  const css=document.createElement('style');
  css.textContent=`
    .dv-file-tools{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}
    .dv-file-tools button{padding:10px 12px}
    .afore-atmosphere{position:fixed;inset:0;z-index:-1;overflow:hidden;pointer-events:none;opacity:.025}
    .afore-mark{position:absolute;font-weight:900;letter-spacing:.8px;white-space:nowrap;transform:rotate(-18deg);color:var(--text,#172033)}
    .dv-doc-toggle{width:100%;display:flex;justify-content:space-between;align-items:center;background:transparent;color:var(--text);border:0;padding:0;text-align:left}
    .dv-doc-body{margin-top:10px}
    .dv-doc-field.collapsed .dv-doc-body{display:none}
    .dv-action-toggle{margin-top:8px;width:100%;background:transparent;color:var(--text);border:1px solid var(--line)}
    .dv-hidden-actions{display:none!important}
    .dv-menu-button{position:fixed;left:12px;top:74px;z-index:65;width:50px;height:50px;border-radius:14px;font-size:24px;padding:0;background:#155eef;color:white;box-shadow:0 8px 24px #0003}
    .dv-menu-overlay{position:fixed;inset:0;background:#0008;z-index:68;display:none}
    .dv-menu-overlay.open{display:block}
    .dv-drawer{position:fixed;left:0;top:0;bottom:0;width:min(320px,86vw);background:var(--card);color:var(--text);z-index:69;transform:translateX(-105%);transition:.22s;padding:24px 16px;overflow:auto;box-shadow:8px 0 28px #0004}
    .dv-drawer.open{transform:translateX(0)}
    .dv-drawer h2{margin:0 0 18px}
    .dv-drawer button{display:block;width:100%;text-align:left;margin:7px 0;background:transparent;color:var(--text);border:1px solid var(--line)}
    .dv-view-hidden{display:none!important}
    .dv-view-title{margin:6px 0 14px;font-size:1.25rem;font-weight:800}
  `;
  document.head.appendChild(css);

  function background(){
    if(document.querySelector('.afore-atmosphere'))return;
    const bg=document.createElement('div');bg.className='afore-atmosphere';
    for(let i=0;i<22;i++){const e=document.createElement('span');e.className='afore-mark';e.textContent=marks[i%marks.length];e.style.left=((i*37)%94)+'%';e.style.top=((i*23)%95)+'%';e.style.fontSize=(13+(i%4)*3)+'px';bg.appendChild(e)}
    document.body.prepend(bg);
  }

  function createTools(input){
    if(!input||input.id==='importFile'||input.id==='userBackupRestore'||input.dataset.dvCamera==='1')return;
    const field=input.closest('.field')||input.parentElement;if(!field)return;
    field.querySelectorAll(':scope .dv-file-tools').forEach(x=>x.remove());
    input.style.display='none';
    const box=document.createElement('div');box.className='dv-file-tools';
    const cam=document.createElement('button');cam.type='button';cam.className='secondary';cam.textContent='📷 Abrir cámara';
    const file=document.createElement('button');file.type='button';file.className='secondary';file.textContent='📁 Adjuntar archivo';
    const capture=document.createElement('input');capture.type='file';capture.accept='image/*';capture.setAttribute('capture','environment');capture.hidden=true;capture.dataset.dvCamera='1';
    cam.onclick=()=>capture.click();
    file.onclick=()=>input.click();
    capture.onchange=()=>{const f=capture.files?.[0];if(!f)return;try{const dt=new DataTransfer();dt.items.add(f);input.files=dt.files;input.dispatchEvent(new Event('change',{bubbles:true}))}catch{alert('No se pudo colocar la fotografía. Intenta nuevamente.')}};
    box.append(cam,file,capture);field.appendChild(box);
  }

  function cleanAndCreateTools(){
    document.querySelectorAll('.dv-file-tools').forEach(x=>x.remove());
    document.querySelectorAll('input[type=file]').forEach(input=>{if(input.id!=='importFile'&&input.id!=='userBackupRestore'&&input.dataset.dvCamera!=='1')createTools(input)});
  }

  function accordion(){
    const panels=[...document.querySelectorAll('#formCard>details')];
    panels.forEach((d,i)=>{if(d.dataset.dvAccordion)return;d.dataset.dvAccordion='1';d.open=i===0;d.addEventListener('toggle',()=>{if(d.open)panels.forEach(o=>{if(o!==d)o.open=false})})});
  }

  function documentCollapsibles(){
    document.querySelectorAll('#formCard details .field').forEach(field=>{
      const input=field.querySelector('input[type=file]:not([data-dv-camera])');
      const label=field.querySelector(':scope>label');
      if(!input||!label||field.dataset.dvCollapse)return;
      field.dataset.dvCollapse='1';field.classList.add('dv-doc-field','collapsed');
      const title=document.createElement('button');title.type='button';title.className='dv-doc-toggle';title.innerHTML=`<span>${label.innerHTML}</span><span>⌄</span>`;
      const body=document.createElement('div');body.className='dv-doc-body';
      label.remove();while(field.firstChild)body.appendChild(field.firstChild);field.append(title,body);
      title.onclick=()=>{field.classList.toggle('collapsed');title.lastElementChild.textContent=field.classList.contains('collapsed')?'⌄':'⌃';if(!field.classList.contains('collapsed'))setTimeout(cleanAndCreateTools,0)};
    });
  }

  function hideSavedActions(){
    document.querySelectorAll('details[id^="exp-"]').forEach(exp=>{const actions=exp.querySelector('.actions');if(!actions||actions.dataset.dvGrouped)return;actions.dataset.dvGrouped='1';actions.classList.add('dv-hidden-actions');const toggle=document.createElement('button');toggle.type='button';toggle.className='dv-action-toggle';toggle.textContent='⚙️ Mostrar acciones';toggle.onclick=()=>{const hidden=actions.classList.toggle('dv-hidden-actions');toggle.textContent=hidden?'⚙️ Mostrar acciones':'⌃ Ocultar acciones'};actions.parentNode.insertBefore(toggle,actions)});
  }

  function showView(kind){
    const main=document.querySelector('main.wrap');if(!main)return;
    const hero=main.querySelector('.card.hero');
    const stats=main.querySelector('.stats');
    const priority=stats?.nextElementSibling;
    const form=document.getElementById('formCard');
    const files=document.getElementById('expedientesCard');
    const backup=document.getElementById('userBackupCard');
    [hero,stats,priority,form,files,backup].forEach(x=>x?.classList.add('dv-view-hidden'));
    if(kind==='home'){hero?.classList.remove('dv-view-hidden');stats?.classList.remove('dv-view-hidden');priority?.classList.remove('dv-view-hidden')}
    if(kind==='new'){form?.classList.remove('dv-view-hidden');if(form?.classList.contains('hidden'))document.getElementById('newBtn')?.click();setTimeout(()=>{accordion();cleanAndCreateTools()},100)}
    if(kind==='files'){files?.classList.remove('dv-view-hidden')}
    if(kind==='backup'){backup?.classList.remove('dv-view-hidden');files?.classList.remove('dv-view-hidden')}
    if(['today','soon','late'].includes(kind)){hero?.classList.remove('dv-view-hidden');stats?.classList.remove('dv-view-hidden');document.querySelector(`[data-stat="${kind}"]`)?.click()}
    window.scrollTo({top:0,behavior:'smooth'});
  }

  function menu(){
    if(document.getElementById('dvDrawer'))return;
    const trigger=document.createElement('button');trigger.id='dvMenuButton';trigger.className='dv-menu-button';trigger.textContent='☰';trigger.title='Abrir menú';
    const overlay=document.createElement('div');overlay.className='dv-menu-overlay';
    const drawer=document.createElement('aside');drawer.id='dvDrawer';drawer.className='dv-drawer';
    drawer.innerHTML=`<h2>☰ Menú</h2>
      <button data-view="home">🏠 Inicio</button>
      <button data-view="new">➕ Nuevo expediente</button>
      <button data-view="files">📚 Ver expedientes</button>
      <button data-view="today">📍 Solicitudes para hoy</button>
      <button data-view="soon">⏳ Próximos 7 días</button>
      <button data-view="late">⚠️ Solicitudes vencidas</button>
      <button data-view="backup">💾 Respaldos y restauración</button>
      <button id="dvCloseMenu">✕ Cerrar menú</button>`;
    const close=()=>{drawer.classList.remove('open');overlay.classList.remove('open')};
    trigger.onclick=()=>{drawer.classList.add('open');overlay.classList.add('open')};overlay.onclick=close;
    drawer.addEventListener('click',e=>{const b=e.target.closest('[data-view]');if(!b)return;showView(b.dataset.view);close()});
    document.body.append(trigger,overlay,drawer);drawer.querySelector('#dvCloseMenu').onclick=close;
    showView('home');
  }

  function hookRender(){if(typeof render==='function'&&!window.__dvLayoutRender16){window.__dvLayoutRender16=true;const old=render;render=function(){old();setTimeout(()=>{hideSavedActions();documentCollapsibles();cleanAndCreateTools()},0)}}

  function run(){background();accordion();documentCollapsibles();cleanAndCreateTools();hideSavedActions();hookRender();menu();const p=document.querySelector('.dv-footer p.small');if(p)p.textContent=`Versión ${VERSION} · © Diego Valdes Guerrero`}
  setTimeout(run,150);setTimeout(run,800);
})();