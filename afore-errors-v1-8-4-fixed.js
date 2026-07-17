(() => {
  if (window.__dvAforeErrorsV184Fixed) return;
  window.__dvAforeErrorsV184Fixed = true;
  const rows = [
    ['MC G04','Registro','AforeMóvil','No te encuentras registrado o la información no está sincronizada.','Expediente o registro sin sincronizar.','Verificar CURP, cerrar sesión y pedir validación del expediente biométrico.','Alta'],
    ['MC H29','Servicio','AforeMóvil','No fue posible continuar con la operación.','Falla temporal o validación pendiente.','Actualizar la aplicación, reintentar y contactar a la AFORE si persiste.','Media'],
    ['MC D96','Biometría','AforeMóvil','No fue posible validar la identidad.','Fotografía, iluminación o expediente biométrico insuficiente.','Usar luz frontal, limpiar cámara y revisar expediente biométrico.','Alta'],
    ['MC E86','Validación','AforeMóvil','No se pudo completar la validación solicitada.','Datos capturados no coinciden con el registro.','Confirmar CURP, nombre y fecha de nacimiento; solicitar corrección si continúa.','Media'],
    ['MC 000','General','AforeMóvil','Ocurrió un error inesperado.','Error genérico de la aplicación o servicio.','Cerrar la aplicación, borrar caché, revisar conexión y volver a intentar.','Media'],
    ['MC 03L','Solicitud','AforeMóvil','No fue posible procesar la solicitud.','Solicitud incompleta, duplicada o rechazada.','Revisar solicitudes activas, documentos y estatus con la AFORE.','Media'],
    ['MC G03','Saldo','AforeMóvil','La CURP no cuenta con saldo disponible.','Saldo no reflejado o pendiente después de un movimiento.','Confirmar CURP, esperar actualización y solicitar aclaración.','Media'],
    ['MC 004','Dispositivo','AforeMóvil','La aplicación asociada a la CURP o al dispositivo no está activa.','Asociación incompleta entre CURP, teléfono y dispositivo.','Confirmar número y solicitar liberación o reactivación del dispositivo.','Media'],
    ['MC F05','Servicio','AforeMóvil','La solicitud no puede ser procesada en este momento.','Falla temporal del servidor.','Esperar, cambiar de red y reportar si continúa.','Media'],
    ['MC I73','Activación','AforeMóvil','La aplicación no puede activarse porque existe un proceso en curso.','Solicitud pendiente asociada a la CURP.','Esperar el proceso o pedir a la AFORE que lo libere.','Alta'],
    ['B04','Biometría','Sistema AFORE','Validación biométrica no completada.','Biométricos incompletos o no vigentes.','Actualizar expediente biométrico en sucursal.','Media'],
    ['D55','Datos','Sistema AFORE','No se pudo validar la información del trabajador.','Diferencias entre CURP, NSS o expediente.','Comparar datos y solicitar corrección.','Media'],
    ['D96','Biometría','Sistema AFORE','Error de validación facial o biométrica.','Coincidencia biométrica insuficiente.','Repetir captura y actualizar biometría.','Alta'],
    ['D97','Biometría','Sistema AFORE','No fue posible completar la comparación biométrica.','Imagen o registro biométrico de baja calidad.','Repetir captura y revisar expediente.','Media'],
    ['D98','Biometría','Sistema AFORE','La validación de identidad fue rechazada.','Datos o biométricos no coinciden.','Validar datos y actualizar expediente.','Media'],
    ['D99','Servicio','Sistema AFORE','No fue posible consultar el resultado de validación.','Respuesta incompleta o caída temporal.','Reintentar y reportar si el trámite queda bloqueado.','Media'],
    ['E11','Datos','AforeWeb','Información obligatoria incompleta.','Faltan campos requeridos.','Revisar todos los campos marcados.','Alta'],
    ['E14','Datos','AforeWeb','Los datos ingresados no son válidos.','Formato o dato incorrecto.','Corregir CURP, RFC, NSS, correo o teléfono.','Alta'],
    ['E17','Sesión','AforeWeb','La sesión no es válida o expiró.','Tiempo de sesión agotado.','Cerrar ventanas, ingresar de nuevo y repetir el trámite.','Alta'],
    ['E18','Solicitud','AforeWeb','La solicitud ya existe o está en proceso.','Trámite duplicado o pendiente.','Consultar estatus antes de crear otra solicitud.','Alta'],
    ['E26','Documento','AforeWeb','No fue posible cargar el documento.','Formato, tamaño o conexión no aceptados.','Usar PDF/JPG legible y reducir el tamaño.','Media'],
    ['E27','Biometría','AforeWeb','No fue posible validar la imagen facial.','Rostro no visible o mala iluminación.','Repetir selfie con fondo claro y luz frontal.','Alta'],
    ['E28','Cámara','AforeWeb','No se pudo acceder a la cámara.','Permiso bloqueado o cámara ocupada.','Autorizar cámara, cerrar otras aplicaciones y recargar.','Alta'],
    ['E29','Biometría','AforeWeb','La fotografía no cumple con los requisitos.','Imagen borrosa, oscura o rostro cubierto.','Tomar otra fotografía sin lentes, gorra ni contraluz.','Alta'],
    ['E30','Token','AforeWeb','El código de verificación no es válido.','Token incorrecto o vencido.','Solicitar un nuevo código y capturarlo sin espacios.','Alta'],
    ['E31','Token','AforeWeb','El código de verificación expiró.','Token fuera de vigencia.','Generar otro código y usarlo inmediatamente.','Alta'],
    ['E34','CURP','AforeWeb','La CURP no fue localizada.','CURP incorrecta o aún no sincronizada.','Validar en RENAPO y con la AFORE.','Media'],
    ['E35','Cuenta','AforeWeb','No se encontró una cuenta asociada.','Cuenta no vinculada o datos desactualizados.','Confirmar AFORE administradora y expediente.','Media'],
    ['E36','Trámite','AforeWeb','El trámite no está disponible.','Servicio no habilitado o cuenta no elegible.','Consultar requisitos y disponibilidad con la AFORE.','Media'],
    ['E37','Servicio','AforeWeb','El servicio no está disponible temporalmente.','Mantenimiento o saturación.','Reintentar en otro horario.','Alta'],
    ['E61','Firma','AforeWeb','No fue posible validar la firma o aceptación.','Firma incompleta o sesión interrumpida.','Reiniciar el paso de firma y confirmar aceptación.','Media'],
    ['MC 007','En investigación','AforeMóvil','Mensaje completo pendiente de confirmar.','Evidencia pública insuficiente.','Conservar captura y reportar la etapa exacta del trámite.','Baja'],
    ['MC 521','En investigación','AforeMóvil','Mensaje completo pendiente de confirmar.','Evidencia pública insuficiente.','Conservar captura y reportar la etapa exacta del trámite.','Baja']
  ];
  const errors=rows.map((e,i)=>({id:i+1,code:e[0],category:e[1],app:e[2],message:e[3],cause:e[4],solution:e[5],confidence:e[6]}));
  const css=document.createElement('style');
  css.textContent=`#dvAforeErrorsView{display:none;max-width:1050px;margin:auto;padding:15px}#dvAforeErrorsView.active{display:block}#dvAforeErrorsView .error-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(270px,1fr));gap:12px;margin-top:14px}#dvAforeErrorsView .error-card{background:var(--card,#fff);border:1px solid var(--line,#d7deea);border-radius:15px;padding:14px}#dvAforeErrorsView .error-code{font-size:1.12rem;font-weight:900;color:var(--primary,#1769ff)}#dvAforeErrorsView .error-meta{display:flex;gap:6px;flex-wrap:wrap;margin:8px 0}#dvAforeErrorsView .error-chip{border:1px solid var(--line,#d7deea);border-radius:999px;padding:4px 8px;font-size:.75rem;font-weight:700}#dvAforeSearch,#dvAforeCategory{box-sizing:border-box;width:100%;padding:12px;border:1px solid var(--line,#d7deea);border-radius:10px;background:var(--card,#fff);color:var(--text,#172033)}#dvAforeCategory{margin-top:8px}`;
  document.head.appendChild(css);
  const app=document.getElementById('app'); if(!app)return;
  const view=document.createElement('section'); view.id='dvAforeErrorsView';
  view.innerHTML=`<div class="card"><div class="hero"><div><h1>⚠️ Errores AFORE</h1><div class="small">Catálogo de ${errors.length} códigos y mensajes</div></div><button id="dvAforeBack" class="secondary">← Volver</button></div><input id="dvAforeSearch" placeholder="🔎 Buscar código, mensaje, causa o solución"><select id="dvAforeCategory"><option value="">Todas las categorías</option></select><div id="dvAforeCount" class="small" style="margin-top:9px"></div></div><div id="dvAforeResults" class="error-grid"></div>`;
  app.after(view);
  const select=view.querySelector('#dvAforeCategory');[...new Set(errors.map(e=>e.category))].sort().forEach(c=>{const o=document.createElement('option');o.value=c;o.textContent=c;select.appendChild(o)});
  const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
  function render(){const q=view.querySelector('#dvAforeSearch').value.trim().toLowerCase(),cat=select.value,list=errors.filter(e=>(!cat||e.category===cat)&&(!q||Object.values(e).join(' ').toLowerCase().includes(q)));view.querySelector('#dvAforeCount').textContent=`${list.length} de ${errors.length} errores`;view.querySelector('#dvAforeResults').innerHTML=list.map(e=>`<article class="error-card"><div class="error-code">${esc(e.code)}</div><div class="error-meta"><span class="error-chip">${esc(e.app)}</span><span class="error-chip">${esc(e.category)}</span><span class="error-chip">Confianza ${esc(e.confidence)}</span></div><p><strong>Mensaje:</strong> ${esc(e.message)}</p><p><strong>Causa probable:</strong> ${esc(e.cause)}</p><p><strong>Qué hacer:</strong> ${esc(e.solution)}</p></article>`).join('')||'<div class="card">No se encontraron errores.</div>'}
  function closeMenu(){const overlay=document.getElementById('dvMenuOverlay');if(overlay)overlay.style.display='none'}
  function openErrors(){closeMenu();app.style.display='none';view.classList.add('active');window.scrollTo(0,0);render()}
  function closeErrors(){view.classList.remove('active');app.style.display='';window.scrollTo(0,0)}
  view.querySelector('#dvAforeSearch').addEventListener('input',render);select.addEventListener('change',render);view.querySelector('#dvAforeBack').addEventListener('click',closeErrors);
  function install(){const grid=document.querySelector('#dvMenuPanel .dv-menu-grid');if(!grid||grid.querySelector('[data-a="errors-afore"]'))return false;const b=document.createElement('button');b.type='button';b.dataset.a='errors-afore';b.textContent='⚠️ Errores AFORE';b.onclick=openErrors;const help=grid.querySelector('[data-a="help"]');grid.insertBefore(b,help||grid.querySelector('[data-a="license"]')||grid.lastElementChild);return true}
  install();new MutationObserver(install).observe(document.body,{childList:true,subtree:true});render();
})();