(() => {
  const $ = id => document.getElementById(id);
  const DAY = 86400000;
  const localISO = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  };
  const daysUntil = value => {
    if (!value) return 99999;
    const [y,m,d] = value.split('-').map(Number);
    const target = new Date(y,m-1,d);
    const now = new Date(); now.setHours(0,0,0,0);
    return Math.round((target-now)/DAY);
  };
  const due = r => r.fechaInicio && typeof plus46 === 'function' ? plus46(r.fechaInicio) : r.fechaSolicitud;
  const completed = r => (r.solicitudEstado || 'pendiente') === 'realizada';
  const urgency = r => {
    if (completed(r)) return 'done';
    const n = daysUntil(due(r));
    return n < 0 ? 'late' : n === 0 ? 'today' : n <= 7 ? 'soon' : 'ok';
  };
  window.today = localISO;
  window.diffDays = daysUntil;
  window.status = urgency;
  window.statPeople = kind => {
    const all = Array.isArray(rows) ? rows : [];
    const active = all.filter(r => !completed(r));
    if (kind === 'all') return all;
    if (kind === 'today') return active.filter(r => urgency(r) === 'today');
    if (kind === 'soon') return active.filter(r => urgency(r) === 'soon');
    return active.filter(r => urgency(r) === 'late');
  };
  window.updateDashboard = () => {
    const all = Array.isArray(rows) ? rows : [];
    const active = all.filter(r => !completed(r));
    if ($('stTotal')) $('stTotal').textContent = all.length;
    if ($('stToday')) $('stToday').textContent = active.filter(r => urgency(r)==='today').length;
    if ($('stSoon')) $('stSoon').textContent = active.filter(r => urgency(r)==='soon').length;
    if ($('stLate')) $('stLate').textContent = active.filter(r => urgency(r)==='late').length;
    const priority = active.filter(r => daysUntil(due(r)) <= 3).sort((a,b)=>(due(a)||'').localeCompare(due(b)||'')).slice(0,8);
    if ($('priority')) $('priority').innerHTML = priority.length ? priority.map(r => `<div style="padding:7px 0;border-bottom:1px solid var(--line)"><strong>${typeof esc==='function'?esc(r.nombre):r.nombre}</strong> — ${due(r)||'Sin fecha'}</div>`).join('') : 'Sin solicitudes prioritarias.';
    const card = document.querySelector('.dv-priority-stat');
    if (card) card.querySelector('b').textContent = String(active.filter(r => daysUntil(due(r)) <= 3).length);
  };
  const addField = () => {
    const form = $('formCard');
    if (!form || $('solicitudEstado')) return;
    const field = document.createElement('div');
    field.className = 'field';
    field.innerHTML = '<label>✅ Estado de la solicitud</label><select id="solicitudEstado" style="width:100%;padding:10px;border:1px solid var(--line);border-radius:9px;background:var(--card);color:var(--text)"><option value="pendiente">Pendiente</option><option value="realizada">Realizada</option><option value="no_realizada">No realizada</option></select><small>Al marcarla como realizada dejará de aparecer en las alertas.</small>';
    const dateField = $('fechaSolicitud')?.closest('.field');
    if (dateField) dateField.after(field);
  };
  const hook = () => {
    addField();
    if (typeof put === 'function' && !window.__dvPut169) {
      window.__dvPut169 = true;
      const original = put;
      put = function(record) {
        if (record && $('formCard') && !$('formCard').classList.contains('hidden')) {
          record.solicitudEstado = $('solicitudEstado')?.value || record.solicitudEstado || 'pendiente';
          if (record.fechaInicio && typeof plus46 === 'function') record.fechaSolicitud = plus46(record.fechaInicio);
        }
        return original(record);
      };
    }
    if (typeof window.editRow === 'function' && !window.__dvEdit169) {
      window.__dvEdit169 = true;
      const original = window.editRow;
      window.editRow = id => {
        original(id);
        const r = Array.isArray(rows) ? rows.find(x => x.id === id) : null;
        setTimeout(() => { if ($('solicitudEstado')) $('solicitudEstado').value = r?.solicitudEstado || 'pendiente'; }, 0);
      };
    }
  };
  setTimeout(hook,500);
})();