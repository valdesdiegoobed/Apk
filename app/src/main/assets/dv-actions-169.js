(() => {
  const style = document.createElement('style');
  style.textContent = `.dv-request-state{margin:10px 0;padding:11px;border:1px solid var(--line);border-radius:12px;background:var(--bg)}.dv-request-state label{display:block;font-size:.78rem;font-weight:800;color:var(--muted);margin-bottom:5px}.dv-request-state select{width:100%;padding:10px;border:1px solid var(--line);border-radius:9px;background:var(--card);color:var(--text);font-weight:700}.dv-record-actions{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr));gap:9px;margin-top:14px}.dv-record-actions button{width:100%;min-height:48px;display:flex;align-items:center;justify-content:center;text-align:center;padding:10px}.dv-record-admin{grid-column:1/-1;display:grid;grid-template-columns:1fr 1fr;gap:9px;border-top:1px solid var(--line);padding-top:11px}.dv-state-badge{display:inline-block;border-radius:999px;padding:5px 9px;font-size:.77rem;font-weight:800;margin:3px}.dv-state-pending{background:#fff3cd;color:#7a5200}.dv-state-done{background:#dcfae6;color:#05603a}.dv-state-notdone{background:#fee4e2;color:#912018}@media(max-width:430px){.dv-record-actions{grid-template-columns:1fr}.dv-record-admin{grid-column:1;grid-template-columns:1fr 1fr}}`;
  document.head.appendChild(style);
  window.dvSaveRequestState = async (id,value) => {
    const record = Array.isArray(rows) ? rows.find(r => r.id === id) : null;
    if (!record || typeof put !== 'function') return;
    record.solicitudEstado = value;
    await put(record);
    if (typeof reload === 'function') await reload();
    if (typeof toast === 'function') toast('Estado actualizado');
  };
  const decorate = () => {
    if (!Array.isArray(rows)) return;
    document.querySelectorAll('details[id^="exp-"]').forEach(details => {
      const id = details.id.slice(4);
      const record = rows.find(r => r.id === id);
      const main = details.querySelector('.exp-main');
      if (!record || !main) return;
      let box = main.querySelector('.dv-request-state');
      if (!box) {
        box = document.createElement('div');
        box.className = 'dv-request-state';
        main.insertBefore(box, main.querySelector('.actions') || null);
      }
      const state = record.solicitudEstado || 'pendiente';
      box.innerHTML = `<label>Estado de la solicitud de retiro</label><select onchange="dvSaveRequestState('${id}',this.value)"><option value="pendiente" ${state==='pendiente'?'selected':''}>⏳ Pendiente</option><option value="realizada" ${state==='realizada'?'selected':''}>✅ Realizada</option><option value="no_realizada" ${state==='no_realizada'?'selected':''}>⚠️ No realizada</option></select>`;
      const top = main.querySelector('div');
      if (top && !top.querySelector('.dv-state-badge')) {
        const badge = document.createElement('span');
        badge.className = `dv-state-badge ${state==='realizada'?'dv-state-done':state==='no_realizada'?'dv-state-notdone':'dv-state-pending'}`;
        badge.textContent = state==='realizada'?'✅ Solicitud realizada':state==='no_realizada'?'⚠️ No realizada':'⏳ Pendiente';
        top.appendChild(badge);
      }
      const actions = main.querySelector('.actions');
      if (!actions || actions.dataset.dvProfessional === '1') return;
      actions.dataset.dvProfessional = '1';
      actions.classList.add('dv-record-actions');
      [...actions.querySelectorAll('button')].forEach(button => {
        const text = button.textContent.toLowerCase();
        if (text.includes('whatsapp')) button.textContent = '💬 Enviar WhatsApp';
        else if (text.includes('llamar')) button.textContent = '📞 Llamar al cliente';
        else if (text.includes('curp')) button.textContent = '📋 Copiar CURP';
        else if (text.includes('generar pdf')) button.textContent = '📄 Generar PDF';
        else if (text.includes('editar')) button.textContent = '✏️ Editar expediente';
        else if (text.includes('eliminar')) button.textContent = '🗑️ Eliminar expediente';
      });
      const edit = [...actions.querySelectorAll('button')].find(b => b.textContent.includes('Editar expediente'));
      const remove = [...actions.querySelectorAll('button')].find(b => b.textContent.includes('Eliminar expediente'));
      if (edit && remove) {
        const admin = document.createElement('div');
        admin.className = 'dv-record-admin';
        admin.append(edit,remove);
        actions.appendChild(admin);
      }
    });
  };
  if (typeof render === 'function' && !window.__dvRenderActions169) {
    window.__dvRenderActions169 = true;
    const original = render;
    render = function(){ original(); setTimeout(decorate,0); };
  }
  setTimeout(decorate,600);
})();