(() => {
  const FILE_IDS = ['foto','docIdFrontal','docIdTrasera','docPagare','docCuenta','docFiscal'];
  const style = document.createElement('style');
  style.textContent = `.dv-upload-status{display:inline-flex;align-items:center;gap:6px;margin-top:8px;padding:6px 10px;border-radius:999px;font-size:.78rem;font-weight:800;background:#f2f4f7;color:#667085}.dv-upload-status.ready{background:#dcfae6;color:#05603a}.dv-upload-field-ready{border-color:#12b76a!important;background:rgba(18,183,106,.06)!important}.dv-process-day{display:inline-flex;align-items:center;gap:6px;margin:7px 0;padding:7px 11px;border-radius:999px;background:#eaf2ff;color:#1849a9;font-weight:800;font-size:.82rem}.dv-process-day.complete{background:#fff3cd;color:#7a5200}.dv-process-day.overdue{background:#fee4e2;color:#912018}`;
  document.head.appendChild(style);

  function labelFor(input){
    const field=input.closest('.field');
    const label=field?.querySelector('label')?.textContent?.trim() || 'Archivo';
    return label.replace(/^\S+\s*/,'').trim();
  }
  function updateIndicator(input){
    const field=input.closest('.field') || input.parentElement;
    if(!field) return;
    let badge=field.querySelector('.dv-upload-status');
    if(!badge){ badge=document.createElement('span'); badge.className='dv-upload-status'; field.appendChild(badge); }
    const selected=input.files && input.files.length>0;
    field.classList.toggle('dv-upload-field-ready',selected);
    badge.classList.toggle('ready',selected);
    badge.textContent=selected ? `✅ ${labelFor(input)} adjuntado` : `○ ${labelFor(input)} pendiente`;
  }
  function installIndicators(){
    FILE_IDS.forEach(id=>{
      const input=document.getElementById(id);
      if(!input) return;
      if(!input.dataset.dvIndicator){ input.dataset.dvIndicator='1'; input.addEventListener('change',()=>updateIndicator(input)); }
      updateIndicator(input);
    });
  }
  function dayNumber(start){
    if(!start) return null;
    const [y,m,d]=start.split('-').map(Number);
    const from=new Date(y,m-1,d,12);
    const now=new Date(); now.setHours(12,0,0,0);
    return Math.floor((now-from)/86400000)+1;
  }
  function decorateProcessDays(){
    if(!Array.isArray(rows)) return;
    document.querySelectorAll('details[id^="exp-"]').forEach(details=>{
      const id=details.id.slice(4), record=rows.find(r=>r.id===id), main=details.querySelector('.exp-main');
      if(!record||!main) return;
      let chip=main.querySelector('.dv-process-day');
      if(!chip){ chip=document.createElement('div'); chip.className='dv-process-day'; const firstSmall=main.querySelector('.small'); (firstSmall?.parentNode||main).insertBefore(chip,firstSmall||main.firstChild); }
      const day=dayNumber(record.fechaInicio);
      chip.className='dv-process-day';
      if(day===null||day<1){ chip.textContent='🗓️ Proceso sin fecha de inicio'; }
      else if(day<=46){ chip.textContent=`⏳ Día ${day} de 46 del proceso`; if(day===46) chip.classList.add('complete'); }
      else { chip.textContent=`⚠️ Día ${day} del proceso · solicitud vencida`; chip.classList.add('overdue'); }
    });
  }
  function addBackupHelp(){
    const card=document.getElementById('expedientesCard');
    const toolbar=card?.querySelector('.toolbar');
    if(!toolbar||document.getElementById('dvOpenBackups')) return;
    const button=document.createElement('button');
    button.id='dvOpenBackups'; button.type='button'; button.className='secondary'; button.textContent='📂 Buscar respaldo';
    button.onclick=()=>document.getElementById('importFile')?.click();
    toolbar.appendChild(button);
    const note=document.createElement('div'); note.className='small'; note.style.marginTop='8px';
    note.innerHTML='Los respaldos se guardan con fecha y nombre visible. En Android normalmente quedan en <strong>Descargas</strong>. Usa “Buscar respaldo” para abrir directamente el selector y restaurarlo.';
    toolbar.parentElement.appendChild(note);
  }
  function run(){ installIndicators(); decorateProcessDays(); addBackupHelp(); const v=document.querySelector('.dv-footer p.small'); if(v)v.textContent='Versión 1.7.0 · © Diego Valdes Guerrero'; }
  if(typeof render==='function'&&!window.__dvRender170){ const old=render; render=function(){old();setTimeout(run,0)}; window.__dvRender170=true; }
  document.getElementById('newBtn')?.addEventListener('click',()=>setTimeout(installIndicators,50));
  setTimeout(run,900);
})();