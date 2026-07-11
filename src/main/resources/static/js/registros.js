"use strict";
/* Fichas: listado y baja de clientes y vehículos.
   El alta y la edición se delegan en el módulo común alta.js. */

let pestana = "vehiculos";
let clientes = [];
let vehiculos = [];
let usuarios = [];
let modoDemo = false;
let buscadorVeh = "";
let filtroTipoVeh = "TODOS";
let buscadorCli = "";

// ---- carga ----
async function cargar(){
  try{
    const [rc, rv] = await Promise.all([fetch("/api/clientes"), fetch("/api/vehiculos")]);
    if (!rc.ok || !rv.ok) throw new Error("HTTP");
    clientes = await rc.json();
    vehiculos = await rv.json();
    modoDemo = false;
  }catch(e){
    modoDemo = true;
    $("aviso").classList.add("on");
    clientes = CLIENTES_DEMO();
    vehiculos = VEHICULOS_DEMO();
  }
}

// ---- pestañas y avisos ----
function renderTabs(){
  const esAdmin = SESION && SESION.rol === "ADMIN";
  $("tabs").innerHTML = `
    <button class="tab ${pestana==="vehiculos"?"activa":""}" data-t="vehiculos">Vehículos</button>
    <button class="tab ${pestana==="clientes"?"activa":""}" data-t="clientes">Clientes</button>
    ${esAdmin ? `<button class="tab ${pestana==="usuarios"?"activa":""}" data-t="usuarios">Usuarios</button>` : ""}`;
  $("tabs").querySelectorAll("button").forEach(b =>
    b.onclick = () => { pestana = b.dataset.t; render(); });
}

function limpiarAviso(){
  const a = $("aviso");
  if (modoDemo){ a.className = "aviso demo on"; a.textContent = "Sin conexión con el servidor: se muestran datos de ejemplo y los cambios no se guardan."; }
  else { a.className = "aviso"; a.textContent = ""; }
}

function render(){
  limpiarAviso();
  renderTabs();
  if (pestana === "usuarios") renderUsuarios();
  else if (pestana === "clientes") renderClientes();
  else renderVehiculos();
}

// ---- clientes ----
function renderClientes(){
  $("filtrosTipoVeh").innerHTML = "";
  $("barra").innerHTML = `
    <input id="buscarCli" type="search" placeholder="Buscar por nombre, teléfono, email o NIF" value="${buscadorCli}">
    <button class="btn primario" id="btnNuevo">+ Nuevo cliente</button>`;
  $("btnNuevo").onclick = () => abrirAltaCliente(onClienteGuardado, null);
  $("buscarCli").oninput = e => { buscadorCli = e.target.value; pintarClientes(); };
  pintarClientes();
}

function pintarClientes(){
  const t = buscadorCli.trim().toLowerCase();
  const datos = [...clientes]
    .sort((a,b) => a.nombre.localeCompare(b.nombre, "es"))
    .filter(c => !t || `${c.nombre} ${c.telefono||""} ${c.email||""} ${c.nifCif||""}`.toLowerCase().includes(t));

  const lista = $("lista");
  if (datos.length === 0){ lista.innerHTML = `<div class="vacio">No hay clientes que coincidan.</div>`; return; }
  lista.innerHTML = "";
  datos.forEach(c => {
    const el = document.createElement("div");
    el.className = "item clic";
    const sub = [c.telefono, c.email, c.nifCif].filter(Boolean).join(" · ");
    el.innerHTML = `
      <div class="cuerpo">
        <div class="prob">${c.nombre}</div>
        <div class="meta">${sub || "—"}</div>
      </div>`;
    el.onclick = () => abrirDetalleCliente(c);
    lista.appendChild(el);
  });
}

// ---- usuarios (solo admin) ----
function renderUsuarios(){
  $("filtrosTipoVeh").innerHTML = "";
  $("barra").innerHTML = `<button class="btn primario" id="btnNuevo">+ Nuevo usuario</button>`;
  $("btnNuevo").onclick = () => abrirAltaUsuario(onUsuarioGuardado);
  cargarUsuarios();
}

async function cargarUsuarios(){
  const lista = $("lista");
  lista.innerHTML = `<div class="vacio">Cargando…</div>`;
  try{
    const r = await fetch("/api/usuarios");
    if (!r.ok) throw new Error("HTTP " + r.status);
    usuarios = await r.json();
    pintarUsuarios();
  }catch(e){
    lista.innerHTML = `<div class="vacio">No se han podido cargar los usuarios.</div>`;
  }
}

function pintarUsuarios(){
  const lista = $("lista");
  if (usuarios.length === 0){ lista.innerHTML = `<div class="vacio">No hay usuarios.</div>`; return; }
  lista.innerHTML = "";
  [...usuarios].sort((a,b) => a.username.localeCompare(b.username, "es")).forEach(u => {
    const el = document.createElement("div");
    el.className = "item";
    el.innerHTML = `
      <div class="cuerpo">
        <div class="prob">${u.username}</div>
      </div>
      <span class="chip" style="--c:${u.rol === "ADMIN" ? "var(--brand)" : "var(--st-recepcion)"}">${u.rol === "ADMIN" ? "Administrador" : "Mecánico"}</span>`;
    lista.appendChild(el);
  });
}

function onUsuarioGuardado(){
  cargarUsuarios();
}

// ---- vehículos ----
function renderVehiculos(){
  $("barra").innerHTML = `
    <input id="buscarVeh" type="search" placeholder="Buscar por matrícula, marca o propietario" value="${buscadorVeh}">
    <button class="btn primario" id="btnNuevo">+ Nuevo vehículo</button>`;
  $("btnNuevo").onclick = () => abrirAltaVehiculo(onVehiculoGuardado, null);
  $("buscarVeh").oninput = e => { buscadorVeh = e.target.value; pintarVehiculos(); };
  pintarFiltroTipos("filtrosTipoVeh", filtroTipoVeh, t => { filtroTipoVeh = t; pintarVehiculos(); });
  pintarVehiculos();
}

function pintarVehiculos(){
  const t = buscadorVeh.trim().toLowerCase();
  const datos = [...vehiculos]
    .sort((a,b) => a.matricula.localeCompare(b.matricula, "es"))
    .filter(v => filtroTipoVeh === "TODOS" || v.tipo === filtroTipoVeh)
    .filter(v => !t || `${v.matricula} ${v.marca} ${v.modelo} ${v.nombreCliente}`.toLowerCase().includes(t));

  const lista = $("lista");
  if (datos.length === 0){ lista.innerHTML = `<div class="vacio">No hay vehículos que coincidan.</div>`; return; }
  lista.innerHTML = "";
  datos.forEach(v => {
    const el = document.createElement("div");
    el.className = "item clic";
    const sub = [TIPO_ETQ[v.tipo] || v.tipo, v.nombreCliente, v.kmActual ? v.kmActual + " km" : null].filter(Boolean).join(" · ");
    el.innerHTML = `
      ${placaHtml(v.matricula, v.tipo)}
      <div class="cuerpo">
        <div class="prob">${v.marca} ${v.modelo}${v.anio ? " · " + v.anio : ""}</div>
        <div class="meta">${sub || "—"}</div>
      </div>`;
    el.onclick = () => abrirDetalleVehiculo(v);
    lista.appendChild(el);
  });
}

// ---- detalle de vehículo: resumen + acciones + historial de reparaciones ----
// opciones.volver: función a la que regresar si este detalle se abrió desde otro
// (p. ej. desde la ficha del propietario). Si no se pasa, no se muestra "Volver".
async function abrirDetalleVehiculo(v, opciones){
  const volver = opciones && opciones.volver;
  const cliente = clientes.find(c => c.idCliente === v.idCliente);
  $("drawer").innerHTML = `
    ${volver ? `<button class="volver" id="btnVolver">← Volver</button>` : ""}
    <div class="dcabe">
      <div>
        ${placaHtml(v.matricula, v.tipo)}
        <div class="coche">${v.marca} ${v.modelo}${v.anio ? " · " + v.anio : ""}</div>
        <div class="cliente ${cliente ? "clic" : ""}" id="clienteLink">${v.nombreCliente || "—"}</div>
      </div>
      <div class="dacc">
        <button class="editar" id="btnEditar" type="button">Editar</button>
        <button class="borrar" id="btnBorrar" type="button">Borrar</button>
        <button class="cerrar" id="btnCerrarDrawer" aria-label="Cerrar">×</button>
      </div>
    </div>

    <div class="bloque">
      <h3>Datos del vehículo</h3>
      <div class="datos">
        <div><b>Matrícula:</b> ${v.matricula}</div>
        <div><b>Tipo:</b> ${TIPO_ETQ[v.tipo] || v.tipo}</div>
        <div><b>Kilómetros:</b> ${v.kmActual ? v.kmActual + " km" : "—"}</div>
        <div><b>Bastidor/VIN:</b> ${v.bastidorVin || "—"}</div>
      </div>
      <div id="badgeRevision"></div>
    </div>

    <div class="bloque">
      <h3>Reparaciones y citas</h3>
      <div id="histLista">Cargando…</div>
    </div>`;
  if (volver) $("btnVolver").onclick = volver;
  $("btnCerrarDrawer").onclick = cerrarAlta;
  $("btnEditar").onclick = () => abrirAltaVehiculo(onVehiculoGuardado, v);
  $("btnBorrar").onclick = () => borrarVehiculo(v.idVehiculo);
  if (!SESION || SESION.rol !== "ADMIN") $("btnBorrar").style.display = "none";
  if (cliente){
    $("clienteLink").onclick = () => abrirDetalleCliente(cliente, {volver: () => abrirDetalleVehiculo(v, opciones)});
  }
  _abrir();

  if (modoDemo){
    $("histLista").innerHTML = `<div class="vacio">Sin conexión con el servidor: no se puede mostrar el historial.</div>`;
    return;
  }
  try{
    const [ordenes, citas] = await Promise.all([
      fetch("/api/ordenes/por-vehiculo/" + v.idVehiculo).then(r => { if (!r.ok) throw new Error("HTTP " + r.status); return r.json(); }),
      fetch("/api/citas/por-vehiculo/" + v.idVehiculo).then(r => r.ok ? r.json() : [])
    ]);
    pintarTimeline("histLista", timelineDe(ordenes, citas), {mostrarVehiculo:false});
    pintarBadgeRevision(v, ordenes);
  }catch(e){
    $("histLista").innerHTML = `<div class="vacio">No se ha podido cargar el historial.</div>`;
  }
}

// Usa el km más alto entre las órdenes del vehículo como referencia de "última entrada".
function pintarBadgeRevision(v, ordenes){
  const cont = $("badgeRevision");
  if (!cont) return;
  const kms = (ordenes || []).map(o => o.kmEntrada).filter(km => km != null);
  const ultimoKm = kms.length ? Math.max(...kms) : null;
  const { toca } = proximaRevision(v, ultimoKm);
  cont.innerHTML = toca
    ? `<span class="badge-revision">⚠ Revisión recomendada (cada ${INTERVALO_KM[v.tipo] || INTERVALO_KM.COCHE} km)</span>`
    : "";
}

// ---- detalle de cliente: resumen + acciones + sus vehículos ----
// opciones.volver: función a la que regresar si este detalle se abrió desde otro
// (p. ej. desde la ficha de uno de sus vehículos).
async function abrirDetalleCliente(c, opciones){
  const volver = opciones && opciones.volver;
  const vehiculosCliente = vehiculos.filter(v => v.idCliente === c.idCliente);
  $("drawer").innerHTML = `
    ${volver ? `<button class="volver" id="btnVolver">← Volver</button>` : ""}
    <div class="dcabe">
      <div>
        <div class="coche">${c.nombre}</div>
        <div class="cliente">${c.nifCif || ""}</div>
      </div>
      <div class="dacc">
        <button class="editar" id="btnEditar" type="button">Editar</button>
        <button class="borrar" id="btnBorrar" type="button">Borrar</button>
        <button class="cerrar" id="btnCerrarDrawer" aria-label="Cerrar">×</button>
      </div>
    </div>

    <div class="bloque">
      <h3>Datos del cliente</h3>
      <div class="datos">
        <div><b>Teléfono:</b> ${c.telefono || "—"}</div>
        <div><b>Email:</b> ${c.email || "—"}</div>
        <div><b>NIF/CIF:</b> ${c.nifCif || "—"}</div>
        <div><b>Alta:</b> ${c.fechaAlta ? fFecha.format(new Date(c.fechaAlta)) : "—"}</div>
      </div>
    </div>

    <div class="bloque">
      <h3>Vehículos</h3>
      <div id="vehiculosCliente"></div>
    </div>

    <div class="bloque">
      <h3>Reparaciones y citas</h3>
      <div id="timelineCliente">Cargando…</div>
    </div>`;
  if (volver) $("btnVolver").onclick = volver;
  $("btnCerrarDrawer").onclick = cerrarAlta;
  $("btnEditar").onclick = () => abrirAltaCliente(onClienteGuardado, c);
  $("btnBorrar").onclick = () => borrarCliente(c.idCliente);
  if (!SESION || SESION.rol !== "ADMIN") $("btnBorrar").style.display = "none";
  pintarVehiculosDeCliente(vehiculosCliente, c, opciones);
  _abrir();

  if (modoDemo){
    $("timelineCliente").innerHTML = `<div class="vacio">Sin conexión con el servidor: no se puede mostrar el historial.</div>`;
    return;
  }
  const items = await cargarTimelineCliente(vehiculosCliente);
  pintarTimeline("timelineCliente", items, {mostrarVehiculo: vehiculosCliente.length > 1});
}

function pintarVehiculosDeCliente(lista, cliente, opciones){
  const cont = $("vehiculosCliente");
  if (!lista || lista.length === 0){
    cont.innerHTML = `<div class="vacio">Este cliente no tiene vehículos registrados.</div>`;
    return;
  }
  cont.innerHTML = "";
  lista.forEach(v => {
    const el = document.createElement("div");
    el.className = "item clic";
    el.innerHTML = `
      ${placaHtml(v.matricula, v.tipo)}
      <div class="cuerpo">
        <div class="prob">${v.marca} ${v.modelo}${v.anio ? " · " + v.anio : ""}</div>
        <div class="meta">${TIPO_ETQ[v.tipo] || v.tipo}${v.kmActual ? " · " + v.kmActual + " km" : ""}</div>
      </div>`;
    el.onclick = () => abrirDetalleVehiculo(v, {volver: () => abrirDetalleCliente(cliente, opciones)});
    cont.appendChild(el);
  });
}

// ---- timeline combinada de reparaciones + citas (ficha de vehículo y de cliente) ----
function timelineDe(ordenes, citas){
  const items = (ordenes || []).map(o => ({fecha: new Date(o.fechaEntrada), tipo:"orden", datos:o}));
  (citas || []).forEach(c => items.push({fecha: new Date(c.fechaHora), tipo:"cita", datos:c}));
  return items;
}

function pintarTimeline(contenedorId, items, opciones){
  const cont = $(contenedorId);
  if (!cont) return;
  if (!items || items.length === 0){
    cont.innerHTML = `<div class="vacio">Todavía no hay reparaciones ni citas registradas.</div>`;
    return;
  }
  cont.innerHTML = [...items]
    .sort((a,b) => b.fecha - a.fecha)
    .map(item => filaTimeline(item, opciones))
    .join("");
}

function filaTimeline(item, opciones){
  const mostrarVeh = opciones && opciones.mostrarVehiculo;
  if (item.tipo === "orden"){
    const o = item.datos;
    const est = INFO[o.estado] || {etq:o.estado, color:"var(--ink-soft)"};
    return `
      <div class="item">
        <div class="cuerpo">
          <div class="prob">${mostrarVeh ? o.matricula + " · " : ""}${o.descripcionProblema || "Sin descripción"}</div>
          <div class="meta">Entró el ${fFecha.format(new Date(o.fechaEntrada))}</div>
        </div>
        <span class="chip" style="--c:${est.color}">${est.etq}</span>
        <span class="imp">${eur.format(Number(o.total)||0)}</span>
        <a class="pdf" href="/api/ordenes/${o.idOrden}/pdf" target="_blank" rel="noopener">PDF</a>
      </div>`;
  }
  const c = item.datos;
  return `
    <div class="item">
      <div class="cuerpo">
        <div class="prob">🗓 ${mostrarVeh ? c.matricula + " · " : ""}Cita: ${c.motivo || "sin motivo"}</div>
        <div class="meta">${fFechaHora.format(new Date(c.fechaHora))}</div>
      </div>
      <span class="chip" style="--c:${COLOR_CITA[c.estado] || "var(--ink-soft)"}">${ETIQUETA_CITA[c.estado] || c.estado}</span>
    </div>`;
}

// Reparaciones + citas de TODOS los vehículos de un cliente, en paralelo.
async function cargarTimelineCliente(vehiculosCliente){
  if (!vehiculosCliente || vehiculosCliente.length === 0) return [];
  try{
    const porVehiculo = await Promise.all(vehiculosCliente.map(v => Promise.all([
      fetch("/api/ordenes/por-vehiculo/" + v.idVehiculo).then(r => r.ok ? r.json() : []),
      fetch("/api/citas/por-vehiculo/" + v.idVehiculo).then(r => r.ok ? r.json() : [])
    ])));
    return porVehiculo.flatMap(([ordenes, citas]) => timelineDe(ordenes, citas));
  }catch(e){
    return [];
  }
}

// ---- callbacks de alta.js ----
// En modo real recargamos (para reflejar cambios en cascada, p. ej. el nombre del
// propietario en sus vehículos). Sin backend, actualizamos las listas en memoria.
function onClienteGuardado(cli, meta){
  if (modoDemo){
    if (meta.modo === "crear") clientes.push(cli);
    else clientes = clientes.map(c => c.idCliente === cli.idCliente ? cli : c);
    render();
  } else {
    cargar().then(render);
  }
}
function onVehiculoGuardado(veh, meta){
  if (modoDemo){
    if (meta.clienteNuevo) clientes.push(meta.clienteNuevo);
    if (meta.modo === "crear") vehiculos.push(veh);
    else vehiculos = vehiculos.map(v => v.idVehiculo === veh.idVehiculo ? veh : v);
    render();
  } else {
    cargar().then(render);
  }
}

// ---- borrar ----
async function borrarCliente(id){
  if (!confirm("¿Borrar este cliente? No se puede borrar si tiene vehículos asociados.")) return;
  await borrar("/api/clientes/" + id, () => { clientes = clientes.filter(c => c.idCliente !== id); });
}
async function borrarVehiculo(id){
  if (!confirm("¿Borrar este vehículo? No se puede borrar si tiene órdenes de reparación asociadas.")) return;
  await borrar("/api/vehiculos/" + id, () => { vehiculos = vehiculos.filter(v => v.idVehiculo !== id); });
}
async function borrar(url, quitarLocal){
  limpiarAviso();
  if (modoDemo){ quitarLocal(); cerrarAlta(); render(); return; }
  try{
    const r = await fetch(url, {method:"DELETE"});
    if (r.status === 409){
      let m = "No se puede borrar: tiene otros datos asociados.";
      try{ const j = await r.json(); if (j.mensaje) m = j.mensaje; }catch(_){}
      $("aviso").className = "aviso error on"; $("aviso").textContent = m;
      return;
    }
    if (!r.ok) throw new Error("HTTP " + r.status);
    cerrarAlta();
    await cargar();
    render();
  }catch(e){ $("aviso").className = "aviso error on"; $("aviso").textContent = "No se ha podido borrar."; }
}

(async function init(){
  await cargarSesion();
  await cargar();
  const params = new URLSearchParams(location.search);
  const clienteParam = params.get("cliente");
  const vehiculoParam = params.get("vehiculo");
  const cliente = clienteParam ? clientes.find(c => String(c.idCliente) === clienteParam) : null;
  const vehiculo = vehiculoParam ? vehiculos.find(v => String(v.idVehiculo) === vehiculoParam) : null;
  if (cliente || vehiculo) pestana = vehiculo ? "vehiculos" : "clientes";
  render();
  if (vehiculo) abrirDetalleVehiculo(vehiculo);
  else if (cliente) abrirDetalleCliente(cliente);
})();

// ---- Datos de muestra solo para previsualizar sin backend. Borrar al desplegar. ----
function CLIENTES_DEMO(){
  return [
    {idCliente:1, nombre:"Ana Torres Ruiz", telefono:"611223344", email:"ana.torres@email.com", nifCif:"12345678Z"},
    {idCliente:2, nombre:"José Martín Gómez", telefono:"622334455", email:"jose.martin@email.com", nifCif:"23456789R"},
    {idCliente:3, nombre:"Transportes Vega S.L.", telefono:"954112233", email:"flota@transportesvega.es", nifCif:"B41222333"},
    {idCliente:4, nombre:"Lucía Ramírez Ortiz", telefono:"633445566", email:"lucia.ramirez@email.com", nifCif:"34567890W"},
  ];
}
function VEHICULOS_DEMO(){
  return [
    {idVehiculo:1, matricula:"1234ABC", marca:"Seat", modelo:"Ibiza", anio:2019, kmActual:82000, tipo:"COCHE", idCliente:1, nombreCliente:"Ana Torres Ruiz"},
    {idVehiculo:2, matricula:"5678DEF", marca:"Renault", modelo:"Clio", anio:2017, kmActual:120000, tipo:"COCHE", idCliente:2, nombreCliente:"José Martín Gómez"},
    {idVehiculo:3, matricula:"9012GHI", marca:"Renault", modelo:"Master", anio:2021, kmActual:95000, tipo:"FURGONETA", idCliente:3, nombreCliente:"Transportes Vega S.L."},
    {idVehiculo:5, matricula:"7890MNO", marca:"Volkswagen", modelo:"Golf", anio:2018, kmActual:76000, tipo:"COCHE", idCliente:4, nombreCliente:"Lucía Ramírez Ortiz"},
    {idVehiculo:9, matricula:"4321KLM", marca:"Honda", modelo:"CB500F", anio:2021, kmActual:21000, tipo:"MOTOCICLETA", idCliente:1, nombreCliente:"Ana Torres Ruiz"},
    {idVehiculo:10, matricula:"7777BCD", marca:"MAN", modelo:"TGX 18.470", anio:2019, kmActual:310000, tipo:"CAMION", idCliente:3, nombreCliente:"Transportes Vega S.L."},
  ];
}
