(() => {
  const VERSION = '1.7.2';
  const byId = id => document.getElementById(id);

  async function copyText(value, label) {
    const text = String(value || '').trim();
    if (!text) return typeof toast === 'function' ? toast(`${label} está vacío`) : alert(`${label} está vacío`);
    try {
      await navigator.clipboard.writeText(text);
      if (typeof toast === 'function') toast(`${label} copiada`);
    } catch {
      const area = document.createElement('textarea');
      area.value = text;
      document.body.appendChild(area);
      area.select();
      document.execCommand('copy');
      area.remove();
    }
  }

  function configurePersonalData() {
    const nombre = byId('nombre');
    const curp = byId('curp');
    [nombre, curp].forEach(input => {
      if (!input || input.dataset.uppercaseReady) return;
      input.dataset.uppercaseReady = '1';
      input.style.textTransform = 'uppercase';
      input.autocapitalize = 'characters';
      input.addEventListener('input', () => {
        const start = input.selectionStart;
        const end = input.selectionEnd;
        input.value = input.value.toLocaleUpperCase('es-MX');
        try { input.setSelectionRange(start, end); } catch {}
      });
    });

    if (curp && !byId('copyCurpForm')) {
      const row = document.createElement('div');
      row.style.cssText = 'display:flex;gap:7px;align-items:stretch';
      curp.parentNode.insertBefore(row, curp);
      row.appendChild(curp);
      const button = document.createElement('button');
      button.id = 'copyCurpForm';
      button.type = 'button';
      button.className = 'secondary';
      button.textContent = '📋';
      button.onclick = () => copyText(curp.value, 'CURP');
      row.appendChild(button);
    }

    if (curp && !byId('contrasenaAfore')) {
      const field = document.createElement('div');
      field.className = 'field';
      field.innerHTML = '<label>🔑 Contraseña AFORE</label><div style="display:flex;gap:7px"><input id="contrasenaAfore" type="text" autocomplete="off"><button id="copyAforeForm" type="button" class="secondary">📋</button></div>';
      curp.closest('.field').after(field);
      byId('copyAforeForm').onclick = () => copyText(byId('contrasenaAfore').value, 'Contraseña AFORE');
    }
  }

  function arrangeStableInterface() {
    if (window.__dvStableInterface168) return;
    window.__dvStableInterface168 = true;

    if (!byId('dv168style')) {
      const style = document.createElement('style');
      style.id = 'dv168style';
      style.textContent = '#licenseBanner{position:sticky;top:64px;z-index:18;margin:10px 0}.dv-section-help{display:block;font-size:.78rem;font-weight:500;color:var(--muted);margin-top:3px}.dv-form-inline{margin-top:12px}';
      document.head.appendChild(style);
    }

    const main = document.querySelector('main.wrap');
    const license = byId('licenseBanner');
    if (main && license) {
      main.prepend(license);
      if (!license.querySelector('.dv-current-date')) {
        const date = document.createElement('div');
        date.className = 'dv-current-date';
        date.style.cssText = 'font-size:.78rem;margin-top:3px;opacity:.9';
        date.textContent = '📅 Fecha actual: ' + new Date().toLocaleDateString('es-MX', { weekday:'long', day:'numeric', month:'long', year:'numeric' });
        license.querySelector('div')?.appendChild(date);
      }
    }

    const form = byId('formCard');
    const newButton = byId('newBtn');
    if (form && newButton) {
      const anchor = newButton.closest('.toolbar') || newButton.parentElement;
      if (anchor) {
        anchor.insertAdjacentElement('afterend', form);
        form.classList.add('dv-form-inline');
      }
    }

    if (form) {
      const sections = [
        ['Fotografía', '📷 Fotografía del cliente', 'Captura o selecciona la fotografía del cliente.'],
        ['Datos personales', '👤 Datos personales', 'Nombre, CURP, contraseña AFORE y teléfono.'],
        ['Información del trámite', '📅 Fechas del trámite', 'Fecha de inicio del trámite y fecha de solicitud de retiro por desempleo.'],
        ['Documentos', '📁 Documentos personales', 'INE, pagaré, estado de cuenta y constancia fiscal.'],
        ['Notas', '📝 Notas', 'Observaciones adicionales.']
      ];
      form.querySelectorAll('summary').forEach(summary => {
        const section = sections.find(item => summary.textContent.includes(item[0]));
        if (section && !summary.querySelector('.dv-section-help')) {
          summary.innerHTML = `<span>${section[1]}</span><small class="dv-section-help">${section[2]}</small>`;
        }
      });
      form.querySelectorAll('label').forEach(label => {
        if (/Fecha de inicio/i.test(label.textContent)) label.textContent = '🚩 Fecha de inicio del trámite';
        if (/Fecha para solicitud/i.test(label.textContent)) label.textContent = '⏰ Fecha de solicitud de retiro por desempleo';
      });
    }

    const version = document.querySelector('.dv-footer p.small');
    if (version) version.textContent = `Versión ${VERSION} · © Diego Valdes Guerrero`;
  }

  function loadModule(name) {
    fetch(`https://appassets.androidplatform.net/assets/${name}`).then(r => r.text()).then(code => (0,eval)(code)).catch(() => {});
  }

  configurePersonalData();
  setTimeout(arrangeStableInterface, 350);
  setTimeout(() => {
    loadModule('dv-scheduling-169.js');
    loadModule('dv-actions-169.js');
    loadModule('dv-backup-169.js');
    loadModule('dv-polish-170.js');
    loadModule('dv-v172.js');
  }, 900);
})();