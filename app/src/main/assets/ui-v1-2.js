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

  // Etiquetas completas del panel principal.
  const labels = {
    today: '📍 Solicitudes para hoy',
    soon: '⏳ Solicitudes para los próximos 7 días',
    late: '⚠️ Solicitudes vencidas'
  };
  Object.entries(labels).forEach(([kind, text]) => {
    const button = document.querySelector(`[data-stat="${kind}"] span`);
    if (button) button.textContent = text;
  });

  // Nombre y CURP siempre en mayúsculas, aunque el teclado escriba minúsculas.
  const upperInput = input => {
    if (!input) return;
    input.style.textTransform = 'uppercase';
    input.autocapitalize = 'characters';
    input.addEventListener('input', () => {
      const start = input.selectionStart;
      const end = input.selectionEnd;
      input.value = input.value.toLocaleUpperCase('es-MX');
      try { input.setSelectionRange(start, end); } catch {}
    });
  };
  upperInput($('nombre'));
  upperInput($('curp'));

  // Botón de copiar junto al campo CURP.
  const curp = $('curp');
  if (curp && !$('copyCurpForm')) {
    const row = document.createElement('div');
    row.style.cssText = 'display:flex;gap:7px;align-items:stretch';
    curp.parentNode.insertBefore(row, curp);
    row.appendChild(curp);
    const button = document.createElement('button');
    button.id = 'copyCurpForm';
    button.type = 'button';
    button.className = 'secondary';
    button.textContent = '📋';
    button.title = 'Copiar CURP';
    button.style.flex = '0 0 auto';
    button.onclick = () => copyText(curp.value, 'CURP');
    row.appendChild(button);
  }

  // Campo Contraseña AFORE con mayúsculas y minúsculas, más botón para copiar.
  if (curp && !$('contrasenaAfore')) {
    const curpField = curp.closest('.field');
    const field = document.createElement('div');
    field.className = 'field';
    field.innerHTML = '<label>🔑 Contraseña AFORE</label><div style="display:flex;gap:7px;align-items:stretch"><input id="contrasenaAfore" type="text" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="Mayúsculas y minúsculas"><button id="copyAforeForm" type="button" class="secondary" title="Copiar contraseña">📋</button></div>';
    curpField.after(field);
    $('copyAforeForm').onclick = () => copyText($('contrasenaAfore').value, 'Contraseña AFORE');
  }

  // Guardar el nuevo campo sin cambiar la estructura actual de la base local.
  if (typeof put === 'function' && !window.__dvPutV12) {
    window.__dvPutV12 = true;
    const originalPut = put;
    put = function(record) {
      const form = $('formCard');
      if (record && Object.prototype.hasOwnProperty.call(record, 'nombre') && form && !form.classList.contains('hidden')) {
        record.nombre = String(record.nombre || '').toLocaleUpperCase('es-MX');
        record.curp = String(record.curp || '').toLocaleUpperCase('es-MX');
        record.contrasenaAfore = $('contrasenaAfore')?.value || '';
      }
      return originalPut(record);
    };
  }

  // Cargar y limpiar la contraseña al editar o crear.
  if (typeof window.editRow === 'function' && !window.__dvEditV12) {
    window.__dvEditV12 = true;
    const originalEdit = window.editRow;
    window.editRow = id => {
      originalEdit(id);
      const record = Array.isArray(rows) ? rows.find(x => x.id === id) : null;
      if ($('contrasenaAfore')) $('contrasenaAfore').value = record?.contrasenaAfore || '';
    };
  }
  const newBtn = $('newBtn');
  if (newBtn) newBtn.addEventListener('click', () => setTimeout(() => { if ($('contrasenaAfore')) $('contrasenaAfore').value = ''; }, 0));
  const cancelBtn = $('cancelBtn');
  if (cancelBtn) cancelBtn.addEventListener('click', () => { if ($('contrasenaAfore')) $('contrasenaAfore').value = ''; });

  // Mostrar la contraseña AFORE en el expediente guardado y permitir copiarla.
  function decorateSavedRecords() {
    if (!Array.isArray(rows)) return;
    document.querySelectorAll('details[id^="exp-"]').forEach(details => {
      const id = details.id.slice(4);
      const record = rows.find(x => x.id === id);
      const main = details.querySelector('.exp-main');
      if (!main || !record) return;
      let line = details.querySelector('.afore-password-line');
      if (record.contrasenaAfore) {
        if (!line) {
          line = document.createElement('div');
          line.className = 'small afore-password-line';
          line.style.cssText = 'display:flex;align-items:center;gap:7px;margin-top:4px;flex-wrap:wrap';
          const firstActions = main.querySelector('.actions');
          main.insertBefore(line, firstActions || null);
        }
        line.innerHTML = `<span>🔑 Contraseña AFORE: <strong>${typeof esc === 'function' ? esc(record.contrasenaAfore) : record.contrasenaAfore}</strong></span><button type="button" class="secondary copy-afore-saved" style="padding:5px 8px">📋 Copiar</button>`;
        line.querySelector('button').onclick = () => copyText(record.contrasenaAfore, 'Contraseña AFORE');
      } else if (line) line.remove();
    });
  }
  if (typeof render === 'function' && !window.__dvRenderV12) {
    window.__dvRenderV12 = true;
    const previousRender = render;
    render = function() {
      previousRender();
      decorateSavedRecords();
    };
    decorateSavedRecords();
  }

  // Mover Administrador al bloque final de contacto.
  function moveAdminButton() {
    const adminButton = $('adminBtn');
    const footerGrid = document.querySelector('.dv-footer .contact-grid');
    if (adminButton && footerGrid) {
      adminButton.className = 'secondary';
      adminButton.style.width = '100%';
      footerGrid.appendChild(adminButton);
    }
    const version = document.querySelector('.dv-footer p.small');
    if (version) version.textContent = `Versión ${VERSION} · © Diego Valdes Guerrero`;
  }
  setTimeout(moveAdminButton, 0);
  setTimeout(moveAdminButton, 300);
})();