"use strict";
/* Módulo común de alta/edición en el panel lateral (drawer).
   Requiere en la página: #drawer y #backdrop, y comun.js cargado antes.

   API pública:
     abrirAltaCliente(onGuardado, dato)    dato = cliente a editar, o null para crear
     abrirAltaVehiculo(onGuardado, dato)   dato = vehículo a editar, o null para crear
     abrirAltaCita(onGuardado, dato)       dato = cita a editar, o null para crear
     abrirAltaUsuario(onGuardado)          solo alta (sin edición), requiere rol ADMIN en el backend
     cerrarAlta()

   onGuardado(objeto, meta) se llama al guardar con éxito:
     objeto: ClienteResponse o VehiculoResponse (o su equivalente simulado sin backend)
     meta:   {tipo:"cliente"|"vehiculo", modo:"crear"|"editar", clienteNuevo?}
             clienteNuevo aparece cuando en el alta de vehículo se crea el propietario al vuelo. */

let _clientesAlta = [];
let _altaDemo = false;
let _ctx = null;                 // {tipo, modo, dato, onGuardado}
let _seqDemo = 900000;

// ---- panel ----
function _abrir(){
  $("backdrop").onclick = cerrarAlta;
  $("drawer").classList.add("on");
  $("drawer").setAttribute("aria-hidden", "false");
  $("backdrop").classList.add("on");
}
function cerrarAlta(){
  _ctx = null;
  $("drawer").classList.remove("on");
  $("drawer").setAttribute("aria-hidden", "true");
  $("backdrop").classList.remove("on");
}
document.addEventListener("keydown", e => { if (e.key === "Escape") cerrarAlta(); });

// ---- utilidades ----
const _val = x => (x == null ? "" : String(x).replace(/"/g, "&quot;"));
function _err(msg){ const a = $("altaError"); if (a){ a.textContent = msg; a.classList.add("on"); } }
function _cabecera(titulo){
  return `<div class="dcabe"><div><div class="coche">${titulo}</div></div>
    <div class="dacc"><button class="cerrar" id="altaCerrar" aria-label="Cerrar">×</button></div></div>
    <div class="aviso error" id="altaError"></div>`;
}
function _pie(txt){
  return `<div class="acciones">
    <button class="btn secundario" id="altaCancelar" type="button">Cancelar</button>
    <button class="btn primario" id="altaGuardar" type="button">${txt}</button></div>`;
}
function _engancharPie(){
  $("altaCerrar").onclick = cerrarAlta;
  $("altaCancelar").onclick = cerrarAlta;
}
function _nid(){ return ++_seqDemo; }
function _nombreDe(idCliente){
  const c = _clientesAlta.find(x => String(x.idCliente) === String(idCliente));
  return c ? c.nombre : "—";
}

async function _cargarClientes(){
  try{
    const r = await fetch("/api/clientes");
    if (!r.ok) throw new Error("HTTP " + r.status);
    _clientesAlta = await r.json();
    _altaDemo = false;
  }catch(e){
    _altaDemo = true;
    _clientesAlta = _CLIENTES_ALTA_DEMO();
  }
}

// POST/PUT real; si no hay backend (falla la red) pasa a modo demostración y simula.
async function _persistir(tipo, modo, id, payload){
  if (_altaDemo) return {objeto: _simular(tipo, modo, id, payload)};
  const base = tipo === "cliente" ? "/api/clientes" : "/api/vehiculos";
  try{
    const url = modo === "editar" ? base + "/" + id : base;
    const metodo = modo === "editar" ? "PUT" : "POST";
    const r = await fetch(url, {method:metodo, headers:{"Content-Type":"application/json"}, body:JSON.stringify(payload)});
    if (!r.ok){
      let m = tipo === "cliente" ? "No se ha podido guardar el cliente." : "No se ha podido guardar el vehículo.";
      try{ const j = await r.json(); if (j.mensaje) m = j.mensaje; }catch(_){}
      return {error: m};
    }
    return {objeto: await r.json()};
  }catch(e){
    _altaDemo = true;                                   // sin servidor -> simular
    return {objeto: _simular(tipo, modo, id, payload)};
  }
}

function _simular(tipo, modo, id, payload){
  if (tipo === "cliente"){
    return modo === "crear"
      ? {idCliente: _nid(), fechaAlta: new Date().toISOString(), ...payload}
      : {idCliente: id, ...payload};
  }
  const nombreCliente = _nombreDe(payload.idCliente);
  return modo === "crear"
    ? {idVehiculo: _nid(), ...payload, nombreCliente}
    : {idVehiculo: id, ...payload, nombreCliente};
}

// ============ CLIENTE ============
function abrirAltaCliente(onGuardado, dato){
  _ctx = {tipo:"cliente", modo: dato ? "editar" : "crear", dato: dato || {}, onGuardado};
  const c = _ctx.dato;
  $("drawer").innerHTML = `${_cabecera(_ctx.modo==="crear" ? "Nuevo cliente" : "Editar cliente")}
    <div class="bloque">
      <div class="campo"><label for="cNombre">Nombre</label><input id="cNombre" value="${_val(c.nombre)}"></div>
      <div class="fila2">
        <div class="campo"><label for="cTel">Teléfono</label><input id="cTel" value="${_val(c.telefono)}"></div>
        <div class="campo"><label for="cEmail">Email</label><input id="cEmail" type="email" value="${_val(c.email)}"></div>
      </div>
      <div class="campo"><label for="cNif">NIF/CIF</label><input id="cNif" value="${_val(c.nifCif)}"></div>
    </div>${_pie("Guardar cliente")}`;
  _engancharPie();
  $("altaGuardar").onclick = _guardarCliente;
  _abrir();
}

async function _guardarCliente(){
  $("altaError").classList.remove("on");
  const payload = {
    nombre: $("cNombre").value.trim(),
    telefono: $("cTel").value.trim() || null,
    email: $("cEmail").value.trim() || null,
    nifCif: $("cNif").value.trim() || null
  };
  if (!payload.nombre) return _err("El nombre es obligatorio.");

  const modo = _ctx.modo, id = _ctx.dato.idCliente, cb = _ctx.onGuardado;
  const res = await _persistir("cliente", modo, id, payload);
  if (res.error) return _err(res.error);
  cerrarAlta();
  if (cb) cb(res.objeto, {tipo:"cliente", modo});
}

// ============ VEHÍCULO ============
async function abrirAltaVehiculo(onGuardado, dato){
  _ctx = {tipo:"vehiculo", modo: dato ? "editar" : "crear", dato: dato || {}, onGuardado};
  await _cargarClientes();
  _pintarFormVehiculo();
  _abrir();
}

function _pintarFormVehiculo(){
  const v = _ctx.dato;
  const ops = [..._clientesAlta].sort((a,b) => a.nombre.localeCompare(b.nombre, "es"))
    .map(c => `<option value="${c.idCliente}" ${c.idCliente===v.idCliente?"selected":""}>${c.nombre}${c.nifCif?" ("+c.nifCif+")":""}</option>`).join("");
  const tipos = TIPOS.map(tp => `<option value="${tp.clave}" ${tp.clave===v.tipo?"selected":""}>${tp.etq}</option>`).join("");

  $("drawer").innerHTML = `${_cabecera(_ctx.modo==="crear" ? "Nuevo vehículo" : "Editar vehículo")}
    <div class="bloque">
      <div class="fila2">
        <div class="campo"><label for="vMat">Matrícula</label><input id="vMat" value="${_val(v.matricula)}" placeholder="1234ABC"></div>
        <div class="campo"><label for="vTipo">Tipo</label><select id="vTipo">${tipos}</select></div>
      </div>
      <div class="fila2">
        <div class="campo"><label for="vMarca">Marca</label><input id="vMarca" value="${_val(v.marca)}"></div>
        <div class="campo"><label for="vModelo">Modelo</label><input id="vModelo" value="${_val(v.modelo)}"></div>
      </div>
      <div class="fila2">
        <div class="campo"><label for="vAnio">Año</label><input id="vAnio" type="number" min="1950" max="2100" value="${_val(v.anio)}"></div>
        <div class="campo"><label for="vKm">Kilómetros</label><input id="vKm" type="number" min="0" value="${_val(v.kmActual)}"></div>
      </div>
      <div class="campo">
        <label for="vCliente">Propietario</label>
        <select id="vCliente"><option value="">Elige un propietario…</option>${ops}<option value="__NUEVO__">➕ Crear propietario nuevo</option></select>
      </div>
      <div id="vNuevoCli" style="display:none">
        <div class="fila2">
          <div class="campo"><label for="vNom">Nombre del propietario</label><input id="vNom"></div>
          <div class="campo"><label for="vTel">Teléfono</label><input id="vTel"></div>
        </div>
        <div class="fila2">
          <div class="campo"><label for="vEmail">Email</label><input id="vEmail" type="email"></div>
          <div class="campo"><label for="vNif">NIF/CIF</label><input id="vNif"></div>
        </div>
      </div>
      <div class="campo"><label for="vVin">Bastidor / VIN</label><input id="vVin" value="${_val(v.bastidorVin)}"></div>
    </div>${_pie("Guardar vehículo")}`;
  _engancharPie();
  $("vCliente").onchange = e => { $("vNuevoCli").style.display = (e.target.value === "__NUEVO__") ? "block" : "none"; };
  $("altaGuardar").onclick = _guardarVehiculo;
}

async function _guardarVehiculo(){
  $("altaError").classList.remove("on");
  const matricula = $("vMat").value.trim().toUpperCase();
  const marca = $("vMarca").value.trim();
  const modelo = $("vModelo").value.trim();
  const sel = $("vCliente").value;
  if (!matricula) return _err("La matrícula es obligatoria.");
  if (!marca || !modelo) return _err("Marca y modelo son obligatorios.");

  let clienteNuevoPayload = null;
  if (sel === "__NUEVO__"){
    const nombre = $("vNom").value.trim();
    if (!nombre) return _err("Falta el nombre del nuevo propietario.");
    clienteNuevoPayload = { nombre, telefono: $("vTel").value.trim()||null, email: $("vEmail").value.trim()||null, nifCif: $("vNif").value.trim()||null };
  } else if (!sel){
    return _err("Falta seleccionar un propietario.");
  }

  const modo = _ctx.modo, id = _ctx.dato.idVehiculo, cb = _ctx.onGuardado;
  const vehBase = { matricula, marca, modelo,
    anio: $("vAnio").value ? Number($("vAnio").value) : null,
    kmActual: $("vKm").value ? Number($("vKm").value) : null,
    bastidorVin: $("vVin").value.trim() || null,
    tipo: $("vTipo").value };

  // 1) Propietario al vuelo (si aplica): se crea primero.
  let idCliente, clienteCreado = null;
  if (clienteNuevoPayload){
    const rc = await _persistir("cliente", "crear", null, clienteNuevoPayload);
    if (rc.error) return _err(rc.error);
    clienteCreado = rc.objeto;
    idCliente = clienteCreado.idCliente;
    if (_altaDemo) _clientesAlta.push(clienteCreado);   // para resolver el nombre en la simulación
  } else {
    idCliente = Number(sel);
  }

  // 2) Vehículo.
  const rv = await _persistir("vehiculo", modo, id, {...vehBase, idCliente});
  if (rv.error) return _err(rv.error);

  cerrarAlta();
  if (cb) cb(rv.objeto, {tipo:"vehiculo", modo, clienteNuevo: clienteCreado});
}

// ============ CITA ============
let _vehiculosAlta = [];

async function _cargarVehiculosAlta(){
  try{
    const r = await fetch("/api/vehiculos");
    if (!r.ok) throw new Error("HTTP " + r.status);
    _vehiculosAlta = await r.json();
  }catch(e){
    _vehiculosAlta = [];
  }
}

// "2026-07-10T09:00:00" (o con milisegundos) -> "2026-07-10T09:00", que es lo
// que exige el valor de un <input type="datetime-local">.
function _fechaParaInput(iso){
  return String(iso).slice(0, 16);
}

async function abrirAltaCita(onGuardado, dato){
  // OJO: el modo se decide por si HAY idCita, no por si "dato" existe. Al volver
  // aquí tras crear un vehículo nuevo (ver "+ Nuevo vehículo" más abajo) se pasa
  // un dato parcial (fecha/motivo/vehículo) sin idCita, y sigue siendo una
  // creación, no una edición.
  _ctx = {tipo:"cita", modo: (dato && dato.idCita) ? "editar" : "crear", dato: dato || {}, onGuardado};
  await _cargarVehiculosAlta();
  _pintarFormCita();
  _abrir();
}

function _pintarFormCita(){
  const c = _ctx.dato;
  const ops = [..._vehiculosAlta].sort((a,b) => a.matricula.localeCompare(b.matricula, "es"))
    .map(v => `<option value="${v.idVehiculo}" ${v.idVehiculo===c.idVehiculo?"selected":""}>${v.matricula} · ${v.marca} ${v.modelo} (${v.nombreCliente})</option>`).join("");

  $("drawer").innerHTML = `${_cabecera(_ctx.modo==="crear" ? "Nueva cita" : "Editar cita")}
    <div class="bloque">
      <div class="campo">
        <label for="ktVehiculo">Vehículo</label>
        <select id="ktVehiculo"><option value="">Elige un vehículo…</option>${ops}</select>
        <button class="btn linea" id="ktNuevoVeh" type="button" style="margin-top:8px">+ Nuevo vehículo</button>
      </div>
      <div class="campo"><label for="ktFecha">Fecha y hora</label><input id="ktFecha" type="datetime-local"></div>
      <div class="campo"><label for="ktMotivo">Motivo</label><textarea id="ktMotivo" placeholder="Describe el motivo de la cita"></textarea></div>
    </div>${_pie("Guardar cita")}`;
  if (c.fechaHora) $("ktFecha").value = _fechaParaInput(c.fechaHora);
  $("ktMotivo").value = c.motivo || "";
  _engancharPie();
  $("altaGuardar").onclick = _guardarCita;
  // El formulario de vehículo vive en el mismo #drawer que este: no se puede abrir
  // "encima" sin perder lo ya escrito, así que guardamos fecha/motivo antes de
  // saltar al alta de vehículo y reabrimos la cita con ellos + el vehículo nuevo.
  $("ktNuevoVeh").onclick = () => {
    const onGuardadoCita = _ctx.onGuardado;
    const datosTemp = {
      ..._ctx.dato,
      fechaHora: $("ktFecha").value ? $("ktFecha").value + ":00" : _ctx.dato.fechaHora,
      motivo: $("ktMotivo").value
    };
    abrirAltaVehiculo(veh => abrirAltaCita(onGuardadoCita, {...datosTemp, idVehiculo: veh.idVehiculo}));
  };
}

async function _guardarCita(){
  $("altaError").classList.remove("on");
  const idVehiculo = $("ktVehiculo").value;
  const fecha = $("ktFecha").value;
  if (!idVehiculo) return _err("Falta seleccionar un vehículo.");
  if (!fecha) return _err("Falta la fecha y hora.");

  const payload = {
    idVehiculo: Number(idVehiculo),
    fechaHora: fecha + ":00",
    motivo: $("ktMotivo").value.trim() || null
  };

  const modo = _ctx.modo, id = _ctx.dato.idCita, cb = _ctx.onGuardado;
  try{
    const url = modo === "editar" ? "/api/citas/" + id : "/api/citas";
    const metodo = modo === "editar" ? "PUT" : "POST";
    const r = await fetch(url, {method:metodo, headers:{"Content-Type":"application/json"}, body:JSON.stringify(payload)});
    if (!r.ok){
      let m = "No se ha podido guardar la cita.";
      try{ const j = await r.json(); if (j.mensaje) m = j.mensaje; }catch(_){}
      return _err(m);
    }
    const objeto = await r.json();
    cerrarAlta();
    if (cb) cb(objeto, {tipo:"cita", modo});
  }catch(e){
    _err("No hay conexión con el servidor.");
  }
}

// ============ USUARIO (solo alta; sin edición ni simulación sin backend) ============
function abrirAltaUsuario(onGuardado){
  _ctx = {tipo:"usuario", modo:"crear", dato:{}, onGuardado};
  $("drawer").innerHTML = `${_cabecera("Nuevo usuario")}
    <div class="bloque">
      <div class="campo"><label for="uUser">Usuario</label><input id="uUser"></div>
      <div class="campo"><label for="uPass">Contraseña</label><input id="uPass" type="password"></div>
      <div class="campo">
        <label for="uRol">Rol</label>
        <select id="uRol"><option value="MECANICO">Mecánico</option><option value="ADMIN">Administrador</option></select>
      </div>
    </div>${_pie("Crear usuario")}`;
  _engancharPie();
  $("altaGuardar").onclick = _guardarUsuario;
  _abrir();
}

async function _guardarUsuario(){
  $("altaError").classList.remove("on");
  const payload = {
    username: $("uUser").value.trim(),
    password: $("uPass").value,
    rol: $("uRol").value
  };
  if (!payload.username) return _err("El usuario es obligatorio.");
  if (!payload.password || payload.password.length < 8) return _err("La contraseña debe tener al menos 8 caracteres.");

  const cb = _ctx.onGuardado;
  try{
    const r = await fetch("/api/usuarios", {method:"POST", headers:{"Content-Type":"application/json"}, body:JSON.stringify(payload)});
    if (!r.ok){
      let m = "No se ha podido crear el usuario.";
      try{ const j = await r.json(); if (j.mensaje) m = j.mensaje; }catch(_){}
      return _err(m);
    }
    const objeto = await r.json();
    cerrarAlta();
    if (cb) cb(objeto);
  }catch(e){
    _err("No hay conexión con el servidor.");
  }
}

// ---- Datos de muestra solo para previsualizar sin backend. Borrar al desplegar. ----
function _CLIENTES_ALTA_DEMO(){
  return [
    {idCliente:1, nombre:"Ana Torres Ruiz", nifCif:"12345678Z"},
    {idCliente:2, nombre:"José Martín Gómez", nifCif:"23456789R"},
    {idCliente:3, nombre:"Transportes Vega S.L.", nifCif:"B41222333"},
    {idCliente:4, nombre:"Lucía Ramírez Ortiz", nifCif:"34567890W"},
  ];
}
