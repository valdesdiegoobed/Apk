(() => {
  const style = document.createElement('style');
  style.textContent = `
    .card, details { position: relative; }
    .card::after, details::after { content:'Diego Valdes'; position:absolute; right:12px; bottom:7px; font-size:10px; font-weight:800; opacity:.055; pointer-events:none; letter-spacing:.8px; }
    .dv-footer { margin:18px 0 8px; text-align:center; }
    .dv-footer .contact-grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:8px; margin-top:12px; }
    .dv-logo { width:54px;height:54px;border-radius:14px;background:#0f172a;color:#d4af37;display:grid;place-items:center;font-weight:900;font-size:22px;margin:auto;border:2px solid #d4af37; }
    .agenda-day { padding:9px 0;border-bottom:1px solid var(--line); }
    .favorite-btn { background:transparent!important;color:#d4af37!important;border:0!important;padding:4px 8px!important;font-size:20px; }
    .photo-modal { position:fixed;inset:0;background:#000d;z-index:100;display:flex;align-items:center;justify-content:center;padding:16px; }
    .photo-modal img { max-width:96%;max-height:90%;border-radius:12px; }
  `;
  document.head.appendChild(style);

  const brand = document.querySelector('.brand');
  if (brand) brand.innerHTML = '🗂️ DV Control de Trámites<div style="font-size:.72rem;font-weight:500;color:#cbd5e1">Diego Valdes Guerrero</div>';
  document.title = 'DV Control de Trámites';

  const priority = document.getElementById('priority')?.closest('.card');
  if (priority && !document.getElementById('dvAgenda')) {
    const agenda = document.createElement('div');
    agenda.id = 'dvAgenda';
    agenda.className = 'card';
    agenda.innerHTML = '<div class="section-title">📅 Agenda mensual</div><input id="agendaMonth" type="month" style="width:100%;padding:11px;border:1px solid var(--line);border-radius:9px;background:var(--card);color:var(--text)"><div id="agendaList" class="small"></div>';
    priority.after(agenda);
    const now = new Date();
    document.getElementById('agendaMonth').value = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
    document.getElementById('agendaMonth').addEventListener('change', renderAgenda);
  }

  const footer = document.createElement('section');
  footer.className = 'card dv-footer';
  footer.innerHTML = `
    <div class="dv-logo">DV</div>
    <h3>DV Control de Trámites</h3>
    <div class="small">Desarrollado por Diego Valdes Guerrero · Monclova, Coahuila</div>
    <div class="contact-grid">
      <button type="button" data-link="https://wa.me/528663305029">💬 WhatsApp</button>
      <button type="button" class="secondary" data-link="mailto:valdesdiegoobed@gmail.com">✉️ Correo</button>
      <button type="button" class="secondary" data-link="https://www.facebook.com/share/1H5bgiH1Yv/">📘 Facebook</button>
      <button type="button" class="ok" data-link="https://wa.me/528663305029?text=Hola%20Diego,%20uso%20DV%20Control%20de%20Tr%C3%A1mites%20y%20quiero%20solicitar%20una%20mejora.">💡 Solicitar mejora</button>
    </div>
    <p class="small">Versión 1.0.0 · © Diego Valdes Guerrero</p>`;
  document.querySelector('main.wrap')?.appendChild(footer);
  footer.querySelectorAll('[data-link]').forEach(b => b.addEventListener('click', () => location.href = b.dataset.link));

  function favorites() { try { return new Set(JSON.parse(localStorage.dvFavorites || '[]')); } catch { return new Set(); } }
  function saveFavorites(set) { localStorage.dvFavorites = JSON.stringify([...set]); }
  function decorateExpedientes() {
    const favs = favorites();
    document.querySelectorAll('details[id^="exp-"]').forEach(d => {
      const id = d.id.slice(4);
      const summary = d.querySelector('summary');
      if (!summary || summary.querySelector('.favorite-btn')) return;
      const star = document.createElement('button');
      star.type = 'button';
      star.className = 'favorite-btn';
      star.textContent = favs.has(id) ? '★' : '☆';
      star.title = 'Marcar como favorito';
      star.addEventListener('click', e => {
        e.preventDefault(); e.stopPropagation();
        const set = favorites();
        set.has(id) ? set.delete(id) : set.add(id);
        saveFavorites(set); star.textContent = set.has(id) ? '★' : '☆';
      });
      summary.insertBefore(star, summary.lastElementChild);
    });
    document.querySelectorAll('.thumb').forEach(img => {
      if (img.tagName !== 'IMG' || img.dataset.zoomReady) return;
      img.dataset.zoomReady = '1'; img.style.cursor = 'zoom-in';
      img.addEventListener('click', () => {
        const modal = document.createElement('div'); modal.className = 'photo-modal';
        modal.innerHTML = `<img src="${img.src}" alt="Fotografía ampliada">`;
        modal.addEventListener('click', () => modal.remove()); document.body.appendChild(modal);
      });
    });
  }
  function renderAgenda() {
    const month = document.getElementById('agendaMonth')?.value;
    const list = document.getElementById('agendaList');
    if (!month || !list || typeof rows === 'undefined') return;
    const items = rows.filter(r => (r.fechaSolicitud || '').startsWith(month)).sort((a,b)=>(a.fechaSolicitud||'').localeCompare(b.fechaSolicitud||''));
    list.innerHTML = items.length ? items.map(r => `<div class="agenda-day"><strong>${r.fechaSolicitud}</strong> — ${r.nombre}</div>`).join('') : '<p>Sin solicitudes en este mes.</p>';
  }

  if (typeof render === 'function') {
    const baseRender = render;
    render = function() { baseRender(); decorateExpedientes(); renderAgenda(); };
    decorateExpedientes(); renderAgenda();
  }
})();
