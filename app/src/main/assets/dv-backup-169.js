(() => {
  const $ = id => document.getElementById(id);
  async function buildBackup(){
    const out=[];
    for(const r of rows||[]) out.push({...r,foto:await ser(r.foto),documentos:await Promise.all((r.documentos||[]).map(ser))});
    return JSON.stringify({version:6,createdAt:new Date().toISOString(),expedientes:out});
  }
  function install(){
    const button=$('exportBtn');
    if(!button||button.dataset.dvChooser)return;
    button.dataset.dvChooser='1';
    button.onclick=async()=>{
      const json=await buildBackup();
      const d=new Date();
      const stamp=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}_${String(d.getHours()).padStart(2,'0')}-${String(d.getMinutes()).padStart(2,'0')}`;
      const name=`DV-Respaldo-Expedientes-${stamp}.json`;
      const blob=new Blob([json],{type:'application/json'});
      const a=document.createElement('a');
      a.href=URL.createObjectURL(blob); a.download=name; document.body.appendChild(a); a.click(); a.remove();
      setTimeout(()=>URL.revokeObjectURL(a.href),1500);
      localStorage.lastBackup=new Date().toISOString();
      localStorage.lastBackupName=name;
      if(typeof backupLabel==='function') backupLabel();
      const info=$('backupInfo'); if(info) info.textContent=`Último respaldo: ${name} · carpeta Descargas`;
      if(typeof toast==='function') toast(`Respaldo guardado en Descargas: ${name}`);
    };
  }
  setTimeout(install,700);
})();