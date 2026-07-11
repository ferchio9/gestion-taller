"use strict";
/* Panel: consume /api/ordenes y calcula las métricas en el navegador. */

const ACTIVOS = ESTADOS.filter(e => e.clave !== "ENTREGADO");   // los que "están en el taller"
const reduce = window.matchMedia("(prefers-reduced-motion:reduce)").matches;
let ORDENES = [];
let VEHICULOS = [];
let ULTIMO_KM_POR_VEHICULO = {};
let filtroTipo = "TODOS";

function esDelMesActual(iso){
  const f = new Date(iso), h = new Date();
  return f.getMonth() === h.getMonth() && f.getFullYear() === h.getFullYear();
}

function contarUp(el, destino, moneda){
  const fin = Number(destino) || 0;
  const pintar = v => el.textContent = moneda ? eur.format(v) : Math.round(v);
  if (reduce){ pintar(fin); return; }
  const t0 = performance.now(), dur = 650;
  (function paso(t){
    const p = Math.min((t - t0)/dur, 1);
    pintar(fin * (1 - Math.pow(1 - p, 3)));
    if (p < 1) requestAnimationFrame(paso);
  })(performance.now());
}

function render(){
  pintarFiltroTipos("filtrosTipo", filtroTipo, t => { filtroTipo = t; render(); });
  renderRevisionProxima();
  const base = filtroTipo === "TODOS" ? ORDENES : ORDENES.filter(o => o.tipo === filtroTipo);
  const activos = base.filter(o => o.estado !== "ENTREGADO");
  $("totalActivos").textContent = activos.length;

  const tablero = $("tablero");
  tablero.innerHTML = "";
  for (const c of ACTIVOS){
    const n = activos.filter(o => o.estado === c.clave).length;
    const div = document.createElement("div");
    div.className = "col";
    div.style.setProperty("--c", c.color);
    div.innerHTML = `<span class="pt"></span><div class="num">${n}</div><div class="lab">${c.etq}</div>`;
    div.onclick = () => location.href = "ordenes.html?estado=" + c.clave;
    tablero.appendChild(div);
  }

  const entregadasMes = base.filter(o => o.estado === "ENTREGADO" && esDelMesActual(o.fechaEntrada));
  const ingresos = entregadasMes.reduce((s,o) => s + totalOrden(o), 0);
  contarUp($("ingresos"), ingresos, true);
  contarUp($("entregados"), entregadasMes.length, false);

  const lista = $("lista");
  lista.innerHTML = "";
  if (activos.length === 0){
    lista.innerHTML = `<div class="vacio">Ahora mismo no hay vehículos en el taller.</div>`;
    return;
  }
  activos.sort((a,b) => new Date(a.fechaEntrada) - new Date(b.fechaEntrada));
  for (const o of activos){
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
    el.onclick = () => location.href = "ordenes.html?orden=" + o.idOrden;
    lista.appendChild(el);
  }
}

async function cargar(){
  try{
    const r = await fetch("/api/ordenes");
    if (!r.ok) throw new Error("HTTP " + r.status);
    ORDENES = await r.json();
    render();
    await cargarRevisionProxima();
  }catch(e){
    $("aviso").classList.add("on");
    ORDENES = datosDemo();
    render();
  }
}

// Vehículos con revisión próxima: km más alto entre sus órdenes vs. kmActual.
// Falla en silencio (igual que cargarVehiculosInfo en ordenes.js): es un aviso
// adicional, no debe romper el resto del panel si no se puede calcular.
async function cargarRevisionProxima(){
  try{
    const r = await fetch("/api/vehiculos");
    if (!r.ok) throw new Error("HTTP " + r.status);
    VEHICULOS = await r.json();
    ULTIMO_KM_POR_VEHICULO = {};
    ORDENES.forEach(o => {
      if (o.kmEntrada == null) return;
      const actual = ULTIMO_KM_POR_VEHICULO[o.idVehiculo];
      if (actual == null || o.kmEntrada > actual) ULTIMO_KM_POR_VEHICULO[o.idVehiculo] = o.kmEntrada;
    });
    renderRevisionProxima();
  }catch(e){ /* sin aviso: no romper el resto del panel */ }
}

// Igual que el resto del panel, respeta el filtro de tipo de vehículo activo.
function renderRevisionProxima(){
  const base = filtroTipo === "TODOS" ? VEHICULOS : VEHICULOS.filter(v => v.tipo === filtroTipo);
  const pendientes = base
    .map(v => ({v, rev: proximaRevision(v, ULTIMO_KM_POR_VEHICULO[v.idVehiculo])}))
    .filter(x => x.rev.toca);
  pintarRevisionProxima(pendientes);
}

function pintarRevisionProxima(pendientes){
  const seccion = $("seccionRevision");
  if (pendientes.length === 0){ seccion.style.display = "none"; return; }
  seccion.style.display = "";
  const lista = $("listaRevision");
  lista.innerHTML = "";
  pendientes.forEach(({v}) => {
    const el = document.createElement("div");
    el.className = "item clic";
    el.innerHTML = `
      ${placaHtml(v.matricula, v.tipo)}
      <div class="cuerpo">
        <div class="prob">${v.marca} ${v.modelo}</div>
        <div class="meta">${v.nombreCliente || ""}${v.kmActual ? " · " + v.kmActual + " km" : ""}</div>
      </div>
      <span class="badge-revision">⚠ Revisión recomendada</span>`;
    el.onclick = () => location.href = "/registros.html?vehiculo=" + v.idVehiculo;
    lista.appendChild(el);
  });
}

$("fecha").textContent =
  new Intl.DateTimeFormat("es-ES", {weekday:"long", day:"numeric", month:"long", year:"numeric"}).format(new Date());

cargar();

// ---- Datos de muestra solo para previsualizar sin backend. Borrar al desplegar. ----
function datosDemo(){
  const d = n => new Date(Date.now() - n*86400000).toISOString();
  const L = (t,desc,c,p) => ({tipo:t,descripcion:desc,cantidad:c,precioUnitario:p});
  return [
    {idOrden:1,matricula:"1234ABC",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(3),descripcionProblema:"Revisión de los 80.000 km",lineas:[L("MANO_OBRA","",2,45),L("MANO_OBRA","",1,35),L("PIEZA","",1,12.5)]},
    {idOrden:2,matricula:"5678DEF",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(4),descripcionProblema:"Ruido en frenos delanteros",lineas:[L("PIEZA","",1,60),L("MANO_OBRA","",1.5,45)]},
    {idOrden:3,matricula:"9012GHI",tipo:"FURGONETA",estado:"EN_REPARACION",fechaEntrada:d(1),descripcionProblema:"Embrague patina",lineas:[L("MANO_OBRA","",4,45),L("PIEZA","",1,320)]},
    {idOrden:4,matricula:"7890MNO",tipo:"COCHE",estado:"LISTO",fechaEntrada:d(2),descripcionProblema:"Preparación para ITV",lineas:[L("MANO_OBRA","",1,40),L("MANO_OBRA","",1,45)]},
    {idOrden:5,matricula:"2345PQR",tipo:"COCHE",estado:"DIAGNOSTICO",fechaEntrada:d(0),descripcionProblema:"Ruido no identificado en motor",lineas:[L("MANO_OBRA","",1,45)]},
    {idOrden:6,matricula:"6789STU",tipo:"FURGONETA",estado:"RECEPCION",fechaEntrada:d(0),descripcionProblema:"Mantenimiento programado flota",lineas:[]},
    {idOrden:7,matricula:"3456JKL",tipo:"FURGONETA",estado:"EN_REPARACION",fechaEntrada:d(2),descripcionProblema:"Cambio de neumáticos",lineas:[L("PIEZA","",2,85),L("MANO_OBRA","",1,45)]},
    {idOrden:8,matricula:"8901VWX",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(30),descripcionProblema:"Batería descargada",lineas:[L("PIEZA","",1,95),L("MANO_OBRA","",0.5,45)]},
    {idOrden:9,matricula:"1234ABC",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(2),descripcionProblema:"Cambio de aceite y filtro de aire",lineas:[L("MANO_OBRA","",1,35),L("PIEZA","",1,15)]},
    {idOrden:10,matricula:"5678DEF",tipo:"COCHE",estado:"ENTREGADO",fechaEntrada:d(35),descripcionProblema:"Cambio de aceite",lineas:[L("MANO_OBRA","",1,35),L("PIEZA","",1,12.5)]},
  ];
}
