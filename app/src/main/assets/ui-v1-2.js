(() => {
  const VERSION = '1.2.0';
  const $ = id => document.getElementById(id);
  const copyText = async (value, label) => {
    const text = String(value || '').trim();
    if (!text) return typeof toast === 'function' ? toast(`${label} está vacío`) : alert(`${label} está vacío`);
    try {
      await navigator.clipboard.writeText(text);
      typeof toast === 'function' ? toast(`${label} copiada`) : alert(`${label} copiada`);
    } catch {
      const area = document.createElement('textarea');
      area.value = text;
      document.body.appendChild(area);
      area.select();
      document.execCommand('copy');
      area.remove();
      typeof toast === 'function' ? toast(`${label} copiada`) : alert(`${label} copiada`);
    }
  };
  const labels = {today:'📍 Solicitudes para hoy',soon:'⏳ Solicitudes para los próximos 7 días',late:'⚠️ Solicitudes vencidas'};
  Object.entries(labels).forEach(([kind,text])=>{const span=document.querySelector(`[data-stat="${kind}"] span`);if(span)span.textContent=text;});
  const upperInput=input=>{if(!input)return;input.style.textTransform='uppercase';input.autocapitalize='characters';input.addEventListener('input',()=>{const s=input.selectionStart,e=input.selectionEnd;input.value=input.value.toLocaleUpperCase('es-MX');try{input.setSelectionRange(s,e)}catch{}})};
  upperInput($('nombre'));upperInput($('curp'));
  const curp=$('curp');
  if(curp&&!$('copyCurpForm')){const row=document.createElement('div');row.style.cssText='display:flex;gap:7px;align-items:stretch';curp.parentNode.insertBefore(row,curp);row.appendChild(curp);const b=document.createElement('button');b.id='copyCurpForm';b.type='button';b.className='secondary';b.textContent='📋';b.title='Copiar CURP';b.style.flex='0 0 auto';b.onclick=()=>copyText(curp.value,'CURP');row.appendChild(b)}
  if(curp&&!$('contrasenaAfore')){const field=document.createElement('div');field.className='field';field.innerHTML='<label>🔑 Contraseña AFORE</label><div style="display:flex;gap:7px;align-items:stretch"><input id="contrasenaAfore" type="text" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="Mayúsculas y minúsculas"><button id="copyAforeForm" type="button" class="secondary" title="Copiar contraseña">📋</button></div>';curp.closest('.field').after(field);$('copyAforeForm').onclick=()=>copyText($('contrasenaAfore').value,'Contraseña AFORE')}
  if(typeof put==='function'&&!window.__dvPutV12){window.__dvPutV12=true;const originalPut=put;put=function(record){const form=$('formCard');if(record&&Object.prototype.hasOwnProperty.call(record,'nombre')&&form&&!form.classList.contains('hidden')){record.nombre=String(record.nombre||'').toLocaleUpperCase('es-MX');record.curp=String(record.curp||'').toLocaleUpperCase('es-MX');record.contrasenaAfore=$('contrasenaAfore')?.value||''}return originalPut(record)}}
  if(typeof window.editRow==='function'&&!window.__dvEditV12){window.__dvEditV12=true;const originalEdit=window.editRow;window.editRow=id=>{originalEdit(id);const record=Array.isArray(rows)?rows.find(x=>x.id===id):null;if($('contrasenaAfore'))$('contrasenaAfore').value=record?.contrasenaAfore||''}}
  $('newBtn')?.addEventListener('click',()=>setTimeout(()=>{if($('contrasenaAfore'))$('contrasenaAfore').value=''},0));
  $('cancelBtn')?.addEventListener('click',()=>{if($('contrasenaAfore'))$('contrasenaAfore').value=''});
  function decorateSavedRecords(){if(!Array.isArray(rows))return;document.querySelectorAll('details[id^="exp-"]').forEach(details=>{const id=details.id.slice(4),record=rows.find(x=>x.id===id),main=details.querySelector('.exp-main');if(!main||!record)return;let line=details.querySelector('.afore-password-line');if(record.contrasenaAfore){if(!line){line=document.createElement('div');line.className='small afore-password-line';line.style.cssText='display:flex;align-items:center;gap:7px;margin-top:4px;flex-wrap:wrap';main.insertBefore(line,main.querySelector('.actions')||null)}line.innerHTML=`<span>🔑 Contraseña AFORE: <strong>${typeof esc==='function'?esc(record.contrasenaAfore):record.contrasenaAfore}</strong></span><button type="button" class="secondary" style="padding:5px 8px">📋 Copiar</button>`;line.querySelector('button').onclick=()=>copyText(record.contrasenaAfore,'Contraseña AFORE')}else if(line)line.remove()})}
  if(typeof render==='function'&&!window.__dvRenderV12){window.__dvRenderV12=true;const previousRender=render;render=function(){previousRender();decorateSavedRecords()};decorateSavedRecords()}
  function moveAdminButton(){const admin=$('adminBtn'),grid=document.querySelector('.dv-footer .contact-grid');if(admin&&grid){admin.className='secondary';admin.style.width='100%';grid.appendChild(admin)}const version=document.querySelector('.dv-footer p.small');if(version)version.textContent=`Versión ${VERSION} · © Diego Valdes Guerrero`}
  setTimeout(moveAdminButton,0);setTimeout(moveAdminButton,300);
})();