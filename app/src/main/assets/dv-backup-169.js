(() => {
  const $ = id => document.getElementById(id);
  async function buildBackup(){
    const out=[];
    for(const r of rows||[]) out.push({...r,foto:await ser(r.foto),documentos:await Promise.all((r.documentos||[]).map(ser))});
    return JSON.stringify({version:5,createdAt:new Date().toISOString(),expedientes:out});
  }
  function saved(){
    localStorage.lastBackup=new Date().toISOString();
    if(typeof backupLabel==='function')backupLabel();
    if(typeof toast==='function')toast('Respaldo preparado correctamente');
  }
  function install(){
    const button=$('exportBtn');
    if(!button||button.dataset.dvChooser)return;
    button.dataset.dvChooser='1';
    button.onclick=async()=>{
      const json=await buildBackup();
      const d=new Date();
      const name=`respaldo-expedientes-${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}.json`;
      const file=new File([json],name,{type:'application/json'});
      if(navigator.share&&navigator.canShare&&navigator.canShare({files:[file]})){
        try{await navigator.share({files:[file],title:'Guardar respaldo de expedientes'});saved();return;}catch(error){if(error&&error.name==='AbortError')return;}
      }
      const a=document.createElement('a');
      a.href=URL.createObjectURL(file);
      a.download=name;a.click();saved();
    };
  }
  setTimeout(install,700);
})();