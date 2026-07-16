(()=>{
  'use strict';
  const VERSION='1.9.0';
  if(document.getElementById('dv-ui-19-style')) return;

  const style=document.createElement('style');
  style.id='dv-ui-19-style';
  style.textContent=`
  :root{
    --dv-bg:#f3f6fb;--dv-surface:#ffffff;--dv-surface-2:#f8fafc;
    --dv-text:#132238;--dv-muted:#64748b;--dv-line:#dbe4f0;
    --dv-primary:#155eef;--dv-primary-dark:#0f46b8;--dv-accent:#d4af37;
    --dv-success:#16804b;--dv-warning:#b7791f;--dv-danger:#c53b3b;
    --dv-radius:18px;--dv-shadow:0 10px 30px rgba(28,48,78,.10);
  }
  @media(prefers-color-scheme:dark){:root{
    --dv-bg:#0e1726;--dv-surface:#172235;--dv-surface-2:#111c2d;
    --dv-text:#edf4ff;--dv-muted:#9fb0c8;--dv-line:#2d3d55;
    --dv-shadow:0 12px 35px rgba(0,0,0,.28)
  }}
  html{scroll-behavior:smooth}body{background:var(--dv-bg)!important;color:var(--dv-text)!important;font-family:Inter,Roboto,system-ui,-apple-system,sans-serif!important;line-height:1.45}
  .wrap,main.wrap{max-width:1080px!important;padding:18px!important}
  .card,.dv-agenda-card,#formCard,#expedientesCard,#userBackupCard,details{background:var(--dv-surface)!important;border:1px solid var(--dv-line)!important;border-radius:var(--dv-radius)!important;box-shadow:var(--dv-shadow)!important}
  .card{padding:20px!important;margin-bottom:16px!important}.hero{position:relative;overflow:hidden;background:linear-gradient(135deg,var(--dv-surface),var(--dv-surface-2))!important}
  .hero:after{content:'DV';position:absolute;right:-18px;bottom:-42px;font-size:132px;font-weight:900;opacity:.035;pointer-events:none}
  h1,h2,h3,strong,label,summary{color:var(--dv-text)!important}h1{letter-spacing:-.035em}h2{letter-spacing:-.02em}.small,.muted,p{color:var(--dv-muted)}
  button,.button,input[type=button],input[type=submit],a.button{min-height:46px;border-radius:13px!important;font-weight:750!important;letter-spacing:.005em;transition:transform .16s ease,box-shadow .16s ease,background .16s ease!important}
  button:active,.button:active{transform:scale(.975)}button:not(.secondary),.primary{box-shadow:0 7px 18px rgba(21,94,239,.22)}
  button.secondary,.secondary{background:var(--dv-surface-2)!important;color:var(--dv-text)!important;border:1px solid var(--dv-line)!important;box-shadow:none!important}
  input,select,textarea{background:var(--dv-surface-2)!important;color:var(--dv-text)!important;border:1px solid var(--dv-line)!important;border-radius:12px!important;min-height:46px;padding:11px 13px!important;outline:none!important}
  input:focus,select:focus,textarea:focus{border-color:var(--dv-primary)!important;box-shadow:0 0 0 3px rgba(21,94,239,.15)!important}
  textarea{min-height:110px}.field{margin-bottom:15px!important}.grid{gap:15px!important}
  details{overflow:hidden;margin-bottom:12px!important;box-shadow:none!important}details>summary{padding:16px 18px!important;font-weight:800!important;background:var(--dv-surface-2)!important;border-radius:inherit;cursor:pointer}details[open]>summary{border-bottom:1px solid var(--dv-line);border-radius:var(--dv-radius) var(--dv-radius) 0 0}
  .stats{gap:12px!important}.stat,.stats>*{border-radius:16px!important;border:1px solid var(--dv-line)!important;background:var(--dv-surface)!important;box-shadow:0 6px 20px rgba(28,48,78,.07)!important}
  .dv-drawer{background:var(--dv-surface)!important;border-right:1px solid var(--dv-line)!important;box-shadow:24px 0 60px rgba(0,0,0,.25)!important}.dv-drawer button{min-height:50px!important;border-radius:13px!important;padding:12px 14px!important}
  .dv-menu-button{box-shadow:0 10px 25px rgba(21,94,239,.32)!important;border:1px solid rgba(255,255,255,.25)!important}
  #expedientesCard .card,[data-client-card],.expediente,.client-card{border-left:5px solid var(--dv-primary)!important;transition:transform .16s ease,box-shadow .16s ease!important}
  #expedientesCard .card:active,[data-client-card]:active,.expediente:active,.client-card:active{transform:scale(.988)}
  .dv-file-tools{gap:9px!important}.dv-upload-status{padding:7px 10px;border-radius:10px;background:var(--dv-surface-2);display:inline-block!important}
  .dv-footer{opacity:.9;padding-bottom:28px}.dv-version-badge{display:inline-flex;align-items:center;gap:7px;padding:7px 11px;border:1px solid var(--dv-line);border-radius:999px;background:var(--dv-surface);font-size:.78rem;color:var(--dv-muted)}
  .dv-section-label{display:flex;align-items:center;gap:9px;margin:8px 0 14px;font-size:.76rem;text-transform:uppercase;letter-spacing:.09em;color:var(--dv-muted);font-weight:850}.dv-section-label:after{content:'';height:1px;background:var(--dv-line);flex:1}
  @media(max-width:640px){.wrap,main.wrap{padding:12px!important}.card{padding:16px!important;border-radius:16px!important}h1{font-size:1.65rem!important}button,.button{width:100%}.dv-file-tools button{width:auto;flex:1;min-width:145px}.stats{grid-template-columns:repeat(2,minmax(0,1fr))!important}.dv-menu-button{top:68px!important;left:10px!important}}
  `;
  document.head.appendChild(style);

  function decorate(){
    document.documentElement.dataset.dvUi='1.9';
    document.querySelectorAll('button').forEach(b=>{
      if(!b.getAttribute('aria-label') && b.textContent.trim()) b.setAttribute('aria-label',b.textContent.trim().replace(/\s+/g,' '));
    });
    const footer=document.querySelector('.dv-footer');
    if(footer && !footer.querySelector('.dv-version-badge')){
      const badge=document.createElement('div');
      badge.className='dv-version-badge';
      badge.textContent='◆ Diseño profesional · v'+VERSION;
      footer.prepend(badge);
    }
    document.querySelectorAll('#formCard>details').forEach((d,i)=>{
      if(d.dataset.dvVisual19) return;
      d.dataset.dvVisual19='1';
      const content=[...d.children].find(x=>x.tagName!=='SUMMARY');
      if(content && !content.querySelector?.('.dv-section-label')){
        const title=document.createElement('div');
        title.className='dv-section-label';
        title.textContent=i===0?'Información principal':i===1?'Datos y fechas':i===2?'Documentos y evidencias':'Información adicional';
        content.prepend?.(title);
      }
    });
  }
  decorate();
  new MutationObserver(()=>requestAnimationFrame(decorate)).observe(document.body,{childList:true,subtree:true});
})();
