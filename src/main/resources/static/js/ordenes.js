"use strict";
/* Órdenes: lista completa + panel de detalle con cambio de estado. */

let ORDENES = [];
let modoDemo = false;
let filtro = "TODAS";
let filtroTipo = "TODOS";
let buscador = "";
let abierta = null;
let VEHICULOS_INFO = {};   // idVehiculo -> {marca, modelo, nombreCliente}, para poder buscar por ellos

// ---- filtros ----
function renderFiltros(){
  const cont = $("filtros");
  const ops = [{clave:"TODAS",etq:"Todas"}, ...ESTADOS];
  cont.innerHTML = ops.map(o =>
    `<button class="filtro ${filtro===o.clave?"activo":""}" data-f="${o.clave}">${o.etq}</button>`).join("");
  cont.querySelectorAll("button").forEach(b =>
    b.onclick = () => { filtro = b.dataset.f; renderFiltros(); renderLista(); });
}

// ---- lista ----
function renderLista(){
  const lista = $("lista");
  pintarFiltroTipos("filtrosTipo", filtroTipo, t => { filtroTipo = t; renderLista(); });
  const q = buscador.trim().toLowerCase();
  const datos = ORDENES
    .filter(o => filtro==="TODAS" || o.estado===filtro)
    .filter(o => filtroTipo==="TODOS" || o.tipo===filtroTipo)
    .filter(o => !q || haystack(o).includes(q))
    .sort((a,b) => new Date(b.fechaEntrada) - new Date(a.fechaEntrada));
  if (datos.length === 0){
    lista.innerHTML = `<div class="vacio">No hay órdenes de reparación que coincidan.</div>`;
    return;
  }
  lista.innerHTML = "";
  for (const o of datos){
    const est = INFO[o.estado] || {etq:o.estado, color:"var(--ink-soft)"};
    const el = document.createElement("div");
    el.className = "item clic";
    el.innerHTML = `
      ${placaHtml(o.matricula, o.tipo)}
      <div class="cuerpo">
        <div class="prob">${o.descripcionProblema || "Sin descripción"}</div>
        <div class="meta">Entró el ${fFecha.format(new Date(o.fechaEntrada))}</div>
      </div>
      <span class="chip" style="--c:${est.color}">${est.etq}</span>
      <span class="imp">${eur.format(totalOrden(o))}</span>`;
    el.onclick = () => abrirDetalle(o.idOrden);
    lista.appendChild(el);
  }
}

// Texto combinado de una orden (vehículo, propietario, descripción y conceptos) para el buscador.
function haystack(o){
  const v = VEHICULOS_INFO[o.idVehiculo] || {};
  const conceptos = (o.lineas || []).map(l => l.descripcion).join(" ");
  return `${o.matricula || ""} ${o.descripcionProblema || ""} ${v.marca || ""} ${v.modelo || ""} ${v.nombreCliente || ""} ${conceptos}`.toLowerCase();
}

// ---- detalle (drawer) ----
async function datosVehiculo(idVehiculo){
  if (modoDemo) return VEHICULOS_DEMO[idVehiculo] || null;
  try{
    const r = await fetch(`/api/vehiculos/${idVehiculo}`);
    if (!r.ok) return null;
    const v = await r.json();
    return {marca:v.marca, modelo:v.modelo, cliente:v.nombreCliente, idCliente:v.idCliente};
  }catch(e){ return null; }
}

async function abrirDetalle(id){
  const o = ORDENES.find(x => x.idOrden === id);
  if (!o) return;
  abierta = id;
  limpiarError();
  const veh = await datosVehiculo(o.idVehiculo);
  pintarDrawer(o, veh);
  $("drawer").classList.add("on");
  $("drawer").setAttribute("aria-hidden","false");
  $("backdrop").classList.add("on");
}

// Mismo patrón que crear.js: un único aviso rojo fijo en la cabecera de la página.
function mostrarAviso(msg, tipo){
  const el = $("avisoError");
  el.textContent = msg;
  el.classList.remove("error", "ok");
  el.classList.add(tipo || "error", "on");
}
function mostrarError(msg){
  mostrarAviso(msg, "error");
}
function limpiarError(){
  $("avisoError").classList.remove("on");
}

function textoPresupuesto(aprobado){
  if (aprobado === true) return "✅ Aprobado por el cliente";
  if (aprobado === false) return "❌ Rechazado por el cliente";
  return "Pendiente de respuesta del cliente";
}

function copiarEnlaceCliente(codigoSeguimiento){
  if (!codigoSeguimiento){
    mostrarError("Esta orden todavía no tiene enlace de seguimiento (modo sin conexión).");
    return;
  }
  const enlace = location.origin + "/seguimiento.html?codigo=" + codigoSeguimiento;
  if (navigator.clipboard && navigator.clipboard.writeText){
    navigator.clipboard.writeText(enlace).then(
      () => mostrarAviso("Enlace copiado: " + enlace, "ok"),
      () => mostrarError("No se ha podido copiar. Enlace: " + enlace)
    );
  } else {
    mostrarAviso("Enlace para el cliente: " + enlace, "ok");
  }
}

function pintarDrawer(o, veh){
  const filas = (o.lineas||[]).map(l => `
    <tr>
      <td>${l.descripcion}</td>
      <td class="n">${Number(l.cantidad)}</td>
      <td class="n">${eur.format(Number(l.precioUnitario)||0)}</td>
      <td class="n">${eur.format(importeLinea(l))}</td>
    </tr>`).join("") || `<tr><td colspan="4" style="color:var(--ink-soft)">Esta orden de reparación aún no tiene conceptos.</td></tr>`;

  const botones = ESTADOS.map(e => {
    const actual = e.clave === o.estado;
    return `<button class="btn-est ${actual?"actual":""}" style="--c:${e.color}"
              ${actual?"disabled":""} data-e="${e.clave}">${e.etq}</button>`;
  }).join("");

  $("drawer").innerHTML = `
    <div class="dcabe">
      <div>
        ${placaHtml(o.matricula, o.tipo)}
        <div class="coche">${veh ? (veh.marca+" "+veh.modelo) : "Vehículo #"+o.idVehiculo}</div>
        <div class="cliente ${veh && veh.idCliente ? "clic" : ""}" id="clienteLink">${veh ? veh.cliente : ""}</div>
      </div>
      <div class="dacc">
        <a class="editar" href="crear.html?id=${o.idOrden}">Editar</a>
        <a class="pdf" href="/api/ordenes/${o.idOrden}/pdf" target="_blank" rel="noopener">PDF</a>
        <button class="borrar" id="btnBorrar" type="button">Borrar</button>
        <button class="cerrar" id="btnCerrar" aria-label="Cerrar">×</button>
      </div>
    </div>

    <div class="bloque">
      <h3>Estado</h3>
      <div class="estados">${botones}</div>
    </div>

    <div class="bloque">
      <h3>Datos de la orden de reparación</h3>
      <div class="datos">
        <div><b>Entrada:</b> ${fFecha.format(new Date(o.fechaEntrada))}</div>
        <div><b>Salida:</b> ${o.fechaSalida ? fFecha.format(new Date(o.fechaSalida)) : "—"}</div>
        <div><b>Km:</b> ${o.kmEntrada ?? "—"}</div>
        <div><b>Orden:</b> #${o.idOrden}</div>
        <div><b>Presupuesto:</b> ${textoPresupuesto(o.presupuestoAprobado)}</div>
      </div>
      <div style="margin-top:10px"><b>Problema:</b> ${o.descripcionProblema || "—"}</div>
      <button class="btn linea" id="btnEnlaceCliente" type="button">Copiar enlace para el cliente</button>
    </div>

    <div class="bloque">
      <h3>Conceptos</h3>
      <table>
        <thead><tr><th>Concepto</th><th class="n">Cant.</th><th class="n">Precio</th><th class="n">Importe</th></tr></thead>
        <tbody>${filas}</tbody>
      </table>
      <div class="total"><span>Total</span><b>${eur.format(totalOrden(o))}</b></div>
    </div>

    <div class="bloque">
      <h3>Historial de estados</h3>
      <div id="histEstados">Cargando…</div>
    </div>`;

  $("btnCerrar").onclick = cerrarDetalle;
  $("btnBorrar").onclick = () => borrarOrden(o.idOrden);
  if (!SESION || SESION.rol !== "ADMIN") $("btnBorrar").style.display = "none";
  $("btnEnlaceCliente").onclick = () => copiarEnlaceCliente(o.codigoSeguimiento);
  if (veh && veh.idCliente){
    $("clienteLink").onclick = () => location.href = "/registros.html?cliente=" + veh.idCliente;
  }
  document.querySelectorAll(".btn-est:not(.actual)").forEach(b =>
    b.onclick = () => cambiarEstado(o.idOrden, b.dataset.e));
  cargarAuditoria(o.idOrden);
}

// ---- historial de estados (auditoría) ----
async function cargarAuditoria(id){
  const cont = $("histEstados");
  if (!cont) return;
  if (modoDemo){
    cont.innerHTML = `<div class="vacio">Sin conexión con el servidor: no se puede mostrar el historial.</div>`;
    return;
  }
  try{
    const r = await fetch(`/api/ordenes/${id}/auditoria`);
    if (!r.ok) throw new Error("HTTP " + r.status);
    pintarAuditoria(await r.json());
  }catch(e){
    cont.innerHTML = `<div class="vacio">No se ha podido cargar el historial de estados.</div>`;
  }
}

function pintarAuditoria(cambios){
  const cont = $("histEstados");
  if (!cont) return;
  if (!cambios || cambios.length === 0){
    cont.innerHTML = `<div class="vacio">Todavía no se ha registrado ningún cambio de estado.</div>`;
    return;
  }
  cont.innerHTML = cambios.map(c => {
    const de = ETIQUETA[c.estadoAnterior] || c.estadoAnterior;
    const a = ETIQUETA[c.estadoNuevo] || c.estadoNuevo;
    return `<div class="item">
        <div class="cuerpo">
          <div class="prob">De ${de} a ${a}</div>
          <div class="meta">${c.usuario} · ${fFechaHora.format(new Date(c.fecha))}</div>
        </div>
      </div>`;
  }).join("");
}

function cerrarDetalle(){
  abierta = null;
  $("drawer").classList.remove("on");
  $("drawer").setAttribute("aria-hidden","true");
  $("backdrop").classList.remove("on");
}

async function borrarOrden(id){
  if (!confirm("¿Borrar esta orden de reparación? Se eliminará junto con sus conceptos.")) return;
  if (modoDemo){ ORDENES = ORDENES.filter(o => o.idOrden !== id); cerrarDetalle(); renderLista(); return; }
  limpiarError();
  try{
    const r = await fetch(`/api/ordenes/${id}`, {method:"DELETE"});
    if (!r.ok){
      let detalle = "No se ha podido borrar la orden de reparación.";
      try{ const j = await r.json(); if (j.mensaje) detalle = j.mensaje; }catch(_){}
      return mostrarError(detalle);
    }
    cerrarDetalle();
    await cargar();
    renderLista();
  }catch(e){ mostrarError("No hay conexión con el servidor."); }
}

async function cambiarEstado(id, nuevo){
  if (modoDemo){
    const o = ORDENES.find(x => x.idOrden === id);
    o.estado = nuevo;
    if (nuevo === "ENTREGADO" && !o.fechaSalida) o.fechaSalida = new Date().toISOString();
    refrescar(id);
    return;
  }
  limpiarError();
  try{
    const r = await fetch(`/api/ordenes/${id}/estado`, {
      method:"PUT",
      headers:{"Content-Type":"application/json"},
      body:JSON.stringify({estado:nuevo})
    });
    if (!r.ok){
      let detalle = "No se ha podido cambiar el estado.";
      try{ const j = await r.json(); if (j.mensaje) detalle = j.mensaje; }catch(_){}
      return mostrarError(detalle);
    }
    await cargar();
    refrescar(id);
  }catch(e){
    mostrarError("No hay conexión con el servidor.");
  }
}

async function refrescar(id){
  renderLista();
  const o = ORDENES.find(x => x.idOrden === id);
  if (o && abierta === id){
    const veh = await datosVehiculo(o.idVehiculo);
    pintarDrawer(o, veh);
  }
}

// ---- carga ----
async function cargar(){
  try{
    const r = await fetch("/api/ordenes");
    if (!r.ok) throw new Error("HTTP "+r.status);
    ORDENES = await r.json();
    modoDemo = false;
    await cargarVehiculosInfo();
  }catch(e){
    modoDemo = true;
    $("aviso").classList.add("on");
    ORDENES = ORDENES_DEMO();
    VEHICULOS_INFO = Object.fromEntries(Object.entries(VEHICULOS_DEMO)
      .map(([id, v]) => [id, {marca:v.marca, modelo:v.modelo, nombreCliente:v.cliente}]));
  }
}

// Mapa idVehiculo -> {marca, modelo, nombreCliente}, usado solo para el buscador.
// Si falla (p. ej. sin permisos), la búsqueda sigue funcionando con los datos de la propia orden.
async function cargarVehiculosInfo(){
  try{
    const r = await fetch("/api/vehiculos");
    if (!r.ok) throw new Error("HTTP " + r.status);
    const lista = await r.json();
    VEHICULOS_INFO = Object.fromEntries(lista.map(v =>
      [v.idVehiculo, {marca:v.marca, modelo:v.modelo, nombreCliente:v.nombreCliente}]));
  }catch(e){
    VEHICULOS_INFO = {};
  }
}

$("backdrop").onclick = cerrarDetalle;
$("buscarOrden").oninput = e => { buscador = e.target.value; renderLista(); };
document.addEventListener("keydown", e => { if (e.key === "Escape") cerrarDetalle(); });

(async function init(){
  await cargarSesion();
  await cargar();
  const params = new URLSearchParams(location.search);
  const estadoParam = params.get("estado");
  if (estadoParam) filtro = estadoParam;
  renderFiltros();
  renderLista();
  const ordenParam = params.get("orden");
  if (ordenParam) abrirDetalle(Number(ordenParam));
})();

// ---- Datos de muestra solo para previsualizar sin backend. Borrar al desplegar. ----
const VEHICULOS_DEMO = {
  1:{marca:"Seat",modelo:"Ibiza",cliente:"Ana Torres Ruiz"},
  2:{marca:"Renault",modelo:"Clio",cliente:"José Martín Gómez"},
  3:{marca:"Renault",modelo:"Master",cliente:"Transportes Vega S.L."},
  4:{marca:"Citroën",modelo:"Jumper",cliente:"Transportes Vega S.L."},
  5:{marca:"Volkswagen",modelo:"Golf",cliente:"Lucía Ramírez Ortiz"},
  6:{marca:"Ford",modelo:"Focus",cliente:"Carlos Núñez Prieto"},
  7:{marca:"Peugeot",modelo:"Partner",cliente:"Reparto Rápido S.L."},
  8:{marca:"Seat",modelo:"León",cliente:"Ana Torres Ruiz"},
};
function ORDENES_DEMO(){
  const d = n => new Date(Date.now() - n*86400000).toISOString();
  const L = (tipo,descripcion,cantidad,precioUnitario) => ({tipo,descripcion,cantidad,precioUnitario});
  return [
    {idOrden:1,idVehiculo:1,matricula:"1234ABC",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(3),fechaSalida:d(1),descripcionProblema:"Revisión de los 80.000 km",kmEntrada:82000,
      lineas:[L("MANO_OBRA","Mano de obra revisión",2,45),L("MANO_OBRA","Cambio de aceite y filtro",1,35),L("PIEZA","Filtro de aceite",1,12.5)]},
    {idOrden:2,idVehiculo:2,matricula:"5678DEF",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(4),fechaSalida:d(2),descripcionProblema:"Ruido en frenos delanteros",kmEntrada:120000,
      lineas:[L("PIEZA","Juego de pastillas delanteras",1,60),L("MANO_OBRA","Mano de obra frenos",1.5,45)]},
    {idOrden:3,idVehiculo:3,matricula:"9012GHI",tipo:"FURGONETA",estado:"EN_REPARACION",fechaEntrada:d(1),fechaSalida:null,descripcionProblema:"Embrague patina",kmEntrada:95000,
      lineas:[L("MANO_OBRA","Diagnóstico y sustitución embrague",4,45),L("PIEZA","Kit de embrague",1,320)]},
    {idOrden:4,idVehiculo:5,matricula:"7890MNO",tipo:"COCHE",estado:"LISTO",fechaEntrada:d(2),fechaSalida:null,descripcionProblema:"Preparación para ITV",kmEntrada:76000,
      lineas:[L("MANO_OBRA","Revisión Pre-ITV",1,40),L("MANO_OBRA","Ajustes varios",1,45)]},
    {idOrden:5,idVehiculo:6,matricula:"2345PQR",tipo:"COCHE",estado:"DIAGNOSTICO",fechaEntrada:d(0),fechaSalida:null,descripcionProblema:"Ruido no identificado en motor",kmEntrada:165000,
      lineas:[L("MANO_OBRA","Diagnóstico ruido motor",1,45)]},
    {idOrden:6,idVehiculo:7,matricula:"6789STU",tipo:"FURGONETA",estado:"RECEPCION",fechaEntrada:d(0),fechaSalida:null,descripcionProblema:"Mantenimiento programado flota",kmEntrada:45000,
      lineas:[]},
    {idOrden:7,idVehiculo:4,matricula:"3456JKL",tipo:"FURGONETA",estado:"EN_REPARACION",fechaEntrada:d(2),fechaSalida:null,descripcionProblema:"Cambio de neumáticos",kmEntrada:140000,
      lineas:[L("PIEZA","Neumático 215/65 R16",2,85),L("MANO_OBRA","Montaje y equilibrado",1,45)]},
    {idOrden:8,idVehiculo:8,matricula:"8901VWX",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(30),fechaSalida:d(28),descripcionProblema:"Batería descargada",kmEntrada:189000,
      lineas:[L("PIEZA","Batería 70Ah",1,95),L("MANO_OBRA","Sustitución batería",0.5,45)]},
    {idOrden:9,idVehiculo:1,matricula:"1234ABC",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(2),fechaSalida:d(0),descripcionProblema:"Cambio de aceite y filtro de aire",kmEntrada:82000,
      lineas:[L("MANO_OBRA","Cambio de aceite",1,35),L("PIEZA","Filtro de aire",1,15)]},
    {idOrden:10,idVehiculo:2,matricula:"5678DEF",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(35),fechaSalida:d(33),descripcionProblema:"Cambio de aceite",kmEntrada:119000,
      lineas:[L("MANO_OBRA","Cambio de aceite y filtro",1,35),L("PIEZA","Filtro de aceite",1,12.5)]},
  ];
}
