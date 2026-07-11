"use strict";
/* Citas: vista semanal (agenda). El alta/edición se delega en alta.js. */

let SEMANA_INICIO = lunesDeSemana(new Date());
let CITAS = [];
let modoDemo = false;

const DIAS = ["Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"];
const fDia = new Intl.DateTimeFormat("es-ES", {day:"numeric", month:"short"});
const fHora = new Intl.DateTimeFormat("es-ES", {hour:"2-digit", minute:"2-digit"});

function lunesDeSemana(fecha){
  const d = new Date(fecha);
  const dia = d.getDay(); // 0 = domingo
  const diff = (dia === 0 ? -6 : 1) - dia;
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

// "yyyy-MM-ddTHH:mm:ss" sin zona horaria, tal como espera el backend (@DateTimeFormat ISO.DATE_TIME).
function isoLocal(fecha){
  const pad = n => String(n).padStart(2, "0");
  return `${fecha.getFullYear()}-${pad(fecha.getMonth()+1)}-${pad(fecha.getDate())}T${pad(fecha.getHours())}:${pad(fecha.getMinutes())}:${pad(fecha.getSeconds())}`;
}

function rangoSemanaTexto(){
  const fin = new Date(SEMANA_INICIO); fin.setDate(fin.getDate() + 6);
  return `${fDia.format(SEMANA_INICIO)} – ${fDia.format(fin)}`;
}

// ---- carga ----
async function cargar(){
  const fin = new Date(SEMANA_INICIO); fin.setDate(fin.getDate() + 7);
  try{
    const r = await fetch(`/api/citas?desde=${isoLocal(SEMANA_INICIO)}&hasta=${isoLocal(fin)}`);
    if (!r.ok) throw new Error("HTTP " + r.status);
    CITAS = await r.json();
    modoDemo = false;
  }catch(e){
    modoDemo = true;
    $("aviso").classList.add("on");
    CITAS = [];
  }
}

// ---- pintado ----
function render(){
  $("rangoSemana").textContent = rangoSemanaTexto();
  const cont = $("semana");
  cont.innerHTML = "";
  const hoy = new Date(); hoy.setHours(0, 0, 0, 0);

  for (let i = 0; i < 7; i++){
    const dia = new Date(SEMANA_INICIO); dia.setDate(dia.getDate() + i);
    const esHoy = dia.getTime() === hoy.getTime();
    const citasDia = CITAS
      .filter(c => { const f = new Date(c.fechaHora); return f.getFullYear()===dia.getFullYear() && f.getMonth()===dia.getMonth() && f.getDate()===dia.getDate(); })
      .sort((a,b) => new Date(a.fechaHora) - new Date(b.fechaHora));

    const col = document.createElement("div");
    col.className = "dia-col";
    col.innerHTML = `<div class="dia-cabecera ${esHoy ? "hoy" : ""}">${DIAS[i]}<br>${fDia.format(dia)}</div>` +
      citasDia.map(c => `
        <div class="cita-card ${c.estado === "CANCELADA" ? "cancelada" : ""}" data-id="${c.idCita}">
          <div class="hora">${fHora.format(new Date(c.fechaHora))}</div>
          <div class="motivo">${c.matricula} · ${c.motivo || "Sin motivo"}</div>
          <span class="chip" style="--c:${COLOR_CITA[c.estado] || "var(--ink-soft)"}">${ETIQUETA_CITA[c.estado] || c.estado}</span>
        </div>`).join("");
    col.querySelectorAll(".cita-card").forEach(el =>
      el.onclick = () => abrirDetalleCita(CITAS.find(c => String(c.idCita) === el.dataset.id)));
    cont.appendChild(col);
  }
}

// ---- detalle de cita: resumen + acciones ----
function abrirDetalleCita(c){
  $("drawer").innerHTML = `
    <div class="dcabe">
      <div>
        ${placaHtml(c.matricula, c.tipo)}
        <div class="coche">${c.nombreCliente || ""}</div>
        <div class="cliente">${fFechaHora.format(new Date(c.fechaHora))}</div>
      </div>
      <div class="dacc">
        <button class="editar" id="btnEditar" type="button">Editar</button>
        <button class="borrar" id="btnBorrar" type="button">Borrar</button>
        <button class="cerrar" id="btnCerrarDrawer" aria-label="Cerrar">×</button>
      </div>
    </div>

    <div class="bloque">
      <h3>Datos de la cita</h3>
      <div class="datos">
        <div><b>Fecha:</b> ${fFechaHora.format(new Date(c.fechaHora))}</div>
        <div><b>Estado:</b> ${ETIQUETA_CITA[c.estado] || c.estado}</div>
      </div>
      <div style="margin-top:10px"><b>Motivo:</b> ${c.motivo || "—"}</div>
    </div>

    <div class="bloque">${bloqueConversion(c)}</div>`;
  $("btnCerrarDrawer").onclick = cerrarAlta;
  $("btnEditar").onclick = () => abrirAltaCita(onCitaGuardada, c);
  $("btnBorrar").onclick = () => borrarCita(c.idCita);
  if (!SESION || SESION.rol !== "ADMIN") $("btnBorrar").style.display = "none";
  if (c.idOrdenGenerada){
    $("btnVerOrden").onclick = () => location.href = "ordenes.html?orden=" + c.idOrdenGenerada;
  } else if (c.estado !== "CANCELADA"){
    $("btnConvertir").onclick = () => {
      location.href = `crear.html?idVehiculo=${c.idVehiculo}&motivo=${encodeURIComponent(c.motivo || "")}&idCita=${c.idCita}`;
    };
  }
  _abrir();
}

// Tres posibilidades: ya se convirtió (enlaza a la orden), está cancelada (no
// tiene sentido convertirla) o sigue abierta (botón normal de conversión).
function bloqueConversion(c){
  if (c.idOrdenGenerada){
    return `<p style="margin-bottom:12px">✅ Esta cita ya se convirtió en la orden <b>#${c.idOrdenGenerada}</b>.</p>
      <button class="btn secundario" id="btnVerOrden" type="button" style="width:100%">Ver orden generada</button>`;
  }
  if (c.estado === "CANCELADA"){
    return `<p style="color:var(--ink-soft)">Esta cita está cancelada, así que no se puede convertir en una orden de reparación.</p>`;
  }
  return `<button class="btn primario" id="btnConvertir" type="button" style="width:100%">Convertir en orden de reparación</button>`;
}

// ---- callbacks / borrado ----
function onCitaGuardada(){
  cargar().then(render);
}

async function borrarCita(id){
  if (!confirm("¿Borrar esta cita?")) return;
  if (modoDemo){ CITAS = CITAS.filter(c => c.idCita !== id); cerrarAlta(); render(); return; }
  try{
    const r = await fetch("/api/citas/" + id, {method:"DELETE"});
    if (!r.ok) throw new Error("HTTP " + r.status);
    cerrarAlta();
    await cargar();
    render();
  }catch(e){ alert("No se ha podido borrar la cita."); }
}

function cambiarSemana(delta){
  SEMANA_INICIO.setDate(SEMANA_INICIO.getDate() + delta * 7);
  cargar().then(render);
}

$("btnSemanaAnterior").onclick = () => cambiarSemana(-1);
$("btnSemanaSiguiente").onclick = () => cambiarSemana(1);
$("btnNuevaCita").onclick = () => abrirAltaCita(onCitaGuardada, null);
$("backdrop").onclick = cerrarAlta;
document.addEventListener("keydown", e => { if (e.key === "Escape") cerrarAlta(); });

(async function init(){
  await cargarSesion();
  await cargar();
  render();
})();
