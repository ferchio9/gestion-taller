"use strict";
/* Nueva/Editar orden: ?id=N -> editar (PUT); sin id -> crear (POST). */

const parametros = new URLSearchParams(location.search);
const idOrden = parametros.get("id");
const idCitaOrigen = parametros.get("idCita"); // llegada desde "Convertir en orden" de una cita
let modoDemo = false;
let vehiculos = [];          // todos, ordenados por matrícula
let lineas = [];             // {descripcion, tipo, cantidad, precioUnitario}

// ---- estados ----
function pintarEstados(sel){
  $("estado").innerHTML = ESTADOS.map(e =>
    `<option value="${e.clave}" ${e.clave===sel?"selected":""}>${e.etq}</option>`).join("");
}

// ---- vehículos: cargar, ordenar y filtrar ----
async function cargarVehiculos(){
  let lista;
  try{
    const r = await fetch("/api/vehiculos");
    if (!r.ok) throw new Error("HTTP "+r.status);
    lista = await r.json();
  }catch(e){
    modoDemo = true;
    $("avisoDemo").classList.add("on");
    lista = VEHICULOS_DEMO;
  }
  // Orden alfabético por matrícula (criterio predecible para el taller).
  vehiculos = lista.sort((a,b) => a.matricula.localeCompare(b.matricula, "es"));
  pintarOpciones("");
}

function textoVehiculo(v){
  return `${v.matricula} · ${v.marca} ${v.modelo} (${v.nombreCliente})`;
}

// Reconstruye las opciones del desplegable aplicando el texto de búsqueda.
function pintarOpciones(q){
  const sel = $("vehiculo");
  const previa = sel.value;
  const t = q.trim().toLowerCase();
  const filtrados = !t ? vehiculos : vehiculos.filter(v =>
    textoVehiculo(v).toLowerCase().includes(t));

  sel.innerHTML = `<option value="">Elige un vehículo…</option>` +
    filtrados.map(v => `<option value="${v.idVehiculo}">${textoVehiculo(v)}</option>`).join("");

  // Conserva la selección si el vehículo sigue visible tras filtrar.
  if (previa && filtrados.some(v => String(v.idVehiculo) === previa)) sel.value = previa;
}

async function cargarOrden(id){
  try{
    const r = await fetch("/api/ordenes/" + id);
    if (!r.ok) throw new Error("HTTP "+r.status);
    return await r.json();
  }catch(e){
    return modoDemo ? (ORDENES_DEMO.find(o => String(o.idOrden) === String(id)) || null) : null;
  }
}

// ---- líneas dinámicas ----
function pintarLineas(){
  const tb = $("cuerpoLineas");
  if (lineas.length === 0){
    tb.innerHTML = `<tr><td colspan="6" class="sin">Aún no hay conceptos. Se puede guardar la orden de reparación así o añadir alguno.</td></tr>`;
    calcularTotal();
    return;
  }
  tb.innerHTML = lineas.map((l,i) => `
    <tr>
      <td><input data-i="${i}" data-c="descripcion" value="${(l.descripcion||"").replace(/"/g,'&quot;')}" placeholder="Descripción"></td>
      <td>
        <select data-i="${i}" data-c="tipo">
          <option value="MANO_OBRA" ${l.tipo==="MANO_OBRA"?"selected":""}>Mano de obra</option>
          <option value="PIEZA" ${l.tipo==="PIEZA"?"selected":""}>Pieza</option>
        </select>
      </td>
      <td class="n"><input data-i="${i}" data-c="cantidad" type="number" min="0" step="0.5" value="${l.cantidad}" style="max-width:80px"></td>
      <td class="n"><input data-i="${i}" data-c="precioUnitario" type="number" min="0" step="0.01" value="${l.precioUnitario}" style="max-width:100px"></td>
      <td class="n imp" id="imp-${i}"></td>
      <td class="n"><button class="quitar" type="button" data-quitar="${i}" aria-label="Quitar línea">×</button></td>
    </tr>`).join("");
  lineas.forEach((_,i) => pintarImporte(i));
  calcularTotal();
}

function pintarImporte(i){
  const celda = $("imp-"+i);
  if (celda) celda.textContent = eur.format(importeLinea(lineas[i]));
}

function calcularTotal(){
  const t = lineas.reduce((s,l) => s + importeLinea(l), 0);
  $("total").textContent = eur.format(t);
}

$("cuerpoLineas").addEventListener("input", e => {
  const el = e.target, i = el.dataset.i, c = el.dataset.c;
  if (i === undefined) return;
  lineas[i][c] = el.value;
  pintarImporte(i);
  calcularTotal();
});
$("cuerpoLineas").addEventListener("click", e => {
  const q = e.target.dataset.quitar;
  if (q === undefined) return;
  lineas.splice(Number(q), 1);
  pintarLineas();
});
$("btnAnadir").onclick = () => {
  lineas.push({descripcion:"", tipo:"MANO_OBRA", cantidad:1, precioUnitario:0});
  pintarLineas();
};

// ---- guardar ----
function validar(){
  $("avisoError").classList.remove("on");
  if (!$("vehiculo").value) return "Falta seleccionar un vehículo.";
  for (const l of lineas){
    if (!String(l.descripcion).trim()) return "Hay un concepto sin descripción.";
    if (!(Number(l.cantidad) > 0)) return "La cantidad debe ser mayor que 0.";
    if (Number(l.precioUnitario) < 0) return "El precio no puede ser negativo.";
  }
  return null;
}

function mostrarError(msg){
  const a = $("avisoError");
  a.textContent = msg;
  a.classList.add("on");
  window.scrollTo({top:0, behavior:"smooth"});
}

async function guardar(){
  const err = validar();
  if (err) return mostrarError(err);

  const payload = {
    idVehiculo: Number($("vehiculo").value),
    descripcionProblema: $("problema").value.trim() || null,
    kmEntrada: $("km").value ? Number($("km").value) : null,
    estado: $("estado").value,
    lineas: lineas.map(l => ({
      idServicio: null,
      tipo: l.tipo,
      descripcion: String(l.descripcion).trim(),
      cantidad: Number(l.cantidad),
      precioUnitario: Number(l.precioUnitario)
    }))
  };

  if (modoDemo) return mostrarError("Sin conexión con el servidor: la orden de reparación no se ha guardado.");

  try{
    const url = idOrden ? "/api/ordenes/" + idOrden : "/api/ordenes";
    const metodo = idOrden ? "PUT" : "POST";
    const r = await fetch(url, {method:metodo, headers:{"Content-Type":"application/json"}, body:JSON.stringify(payload)});
    if (!r.ok){
      let detalle = "No se ha podido guardar la orden de reparación.";
      try{ const j = await r.json(); if (j.mensaje) detalle = j.mensaje; }catch(_){}
      return mostrarError(detalle);
    }
    const guardada = await r.json();
    // Orden nueva creada desde "Convertir en orden" de una cita: se enlazan para
    // que la cita quede completada y apunte a la orden que generó.
    if (!idOrden && idCitaOrigen){
      try{
        await fetch(`/api/citas/${idCitaOrigen}/convertir`, {
          method:"PUT", headers:{"Content-Type":"application/json"},
          body:JSON.stringify({idOrden: guardada.idOrden})
        });
      }catch(e){ /* si falla el enlazado no bloqueamos: la orden ya se ha guardado */ }
    }
    location.href = "/ordenes.html";
  }catch(e){
    mostrarError("No hay conexión con el servidor.");
  }
}

$("buscarVeh").addEventListener("input", e => pintarOpciones(e.target.value));
$("btnNuevoVeh").onclick = () => abrirAltaVehiculo(onVehiculoCreado);
$("btnGuardar").onclick = guardar;
$("btnCancelar").onclick = () => location.href = "/ordenes.html";

// Cuando el alta rápida crea un vehículo, lo añadimos al desplegable y lo dejamos elegido.
function onVehiculoCreado(veh){
  vehiculos.push(veh);
  vehiculos.sort((a,b) => a.matricula.localeCompare(b.matricula, "es"));
  pintarOpciones("");
  $("vehiculo").value = veh.idVehiculo;
}

// ---- arranque ----
(async function init(){
  pintarEstados("RECEPCION");
  await cargarVehiculos();

  if (idOrden){
    $("titulo").textContent = "Editar orden de reparación nº " + idOrden;
    document.title = "Editar orden de reparación · Taller";
    const o = await cargarOrden(idOrden);
    if (o){
      $("vehiculo").value = o.idVehiculo;
      pintarEstados(o.estado);
      $("km").value = o.kmEntrada ?? "";
      $("problema").value = o.descripcionProblema ?? "";
      lineas = (o.lineas || []).map(l => ({
        descripcion:l.descripcion, tipo:l.tipo, cantidad:l.cantidad, precioUnitario:l.precioUnitario
      }));
    }else{
      mostrarError("No existe la orden de reparación nº " + idOrden + ".");
    }
  }else{
    // Llegada desde "Convertir en orden" de una cita: precarga vehículo y motivo.
    const idVehiculoParam = parametros.get("idVehiculo");
    const motivoParam = parametros.get("motivo");
    if (idVehiculoParam) $("vehiculo").value = idVehiculoParam;
    if (motivoParam) $("problema").value = motivoParam;
  }
  pintarLineas();
})();

// ---- Datos de muestra solo para previsualizar sin backend. Borrar al desplegar. ----
const VEHICULOS_DEMO = [
  {idVehiculo:1, matricula:"1234ABC", marca:"Seat", modelo:"Ibiza", nombreCliente:"Ana Torres Ruiz"},
  {idVehiculo:2, matricula:"5678DEF", marca:"Renault", modelo:"Clio", nombreCliente:"José Martín Gómez"},
  {idVehiculo:3, matricula:"9012GHI", marca:"Renault", modelo:"Master", nombreCliente:"Transportes Vega S.L."},
  {idVehiculo:5, matricula:"7890MNO", marca:"Volkswagen", modelo:"Golf", nombreCliente:"Lucía Ramírez Ortiz"},
];
const ORDENES_DEMO = [
  {idOrden:3, idVehiculo:3, estado:"EN_REPARACION", kmEntrada:95000, descripcionProblema:"Embrague patina",
   lineas:[{descripcion:"Diagnóstico y sustitución embrague",tipo:"MANO_OBRA",cantidad:4,precioUnitario:45},
           {descripcion:"Kit de embrague",tipo:"PIEZA",cantidad:1,precioUnitario:320}]},
];
