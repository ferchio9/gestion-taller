"use strict";
/* Portal público de seguimiento: sin login, accesible por enlace. Requiere
   comun.js, pero NUNCA llama a cargarSesion() (esta página es anónima). */

const codigo = new URLSearchParams(location.search).get("codigo");

function mostrarError(msg){
  $("avisoError").textContent = msg;
  $("avisoError").classList.add("on");
}

function pintarStepper(estadoActual){
  const idxActual = ESTADOS.findIndex(e => e.clave === estadoActual);
  return `<div class="stepper">${ESTADOS.map((e,i) => {
    const cls = i < idxActual ? "hecho" : (i === idxActual ? "actual" : "");
    return `<div class="paso ${cls}"><div class="bola">${i < idxActual ? "✓" : i+1}</div><div class="etq">${e.etq}</div></div>`;
  }).join("")}</div>`;
}

function pintarPresupuesto(o){
  if (o.presupuestoAprobado === null){
    return `<div class="bloque">
      <h3>Presupuesto</h3>
      <p>Antes de continuar, confirma si aceptas el presupuesto de esta reparación.</p>
      <div class="acciones" style="justify-content:flex-start;margin-top:14px">
        <button class="btn primario" id="btnAceptar" type="button">Aceptar presupuesto</button>
        <button class="btn secundario" id="btnRechazar" type="button">Rechazar</button>
      </div>
    </div>`;
  }
  const cuando = o.presupuestoRespondidoEn ? fFechaHora.format(new Date(o.presupuestoRespondidoEn)) : "";
  return `<div class="bloque">
    <h3>Presupuesto</h3>
    <p>${o.presupuestoAprobado ? "✅ Aprobaste este presupuesto" : "❌ Rechazaste este presupuesto"}${cuando ? " · " + cuando : ""}</p>
  </div>`;
}

function pintarHistorial(historial){
  if (!historial || historial.length === 0) return "";
  const filas = historial.map(c => `
    <div class="item">
      <div class="cuerpo">
        <div class="prob">De ${ETIQUETA[c.estadoAnterior] || c.estadoAnterior} a ${ETIQUETA[c.estadoNuevo] || c.estadoNuevo}</div>
        <div class="meta">${fFechaHora.format(new Date(c.fecha))}</div>
      </div>
    </div>`).join("");
  return `<div class="bloque"><h3>Historial</h3>${filas}</div>`;
}

function pintar(o){
  $("sub").textContent = `${o.marca} ${o.modelo} · ${o.matricula} · ${o.nombreCliente}`;
  const filas = (o.lineas||[]).map(l => `
    <tr>
      <td>${l.descripcion}</td>
      <td class="n">${Number(l.cantidad)}</td>
      <td class="n">${eur.format(Number(l.precioUnitario)||0)}</td>
      <td class="n">${eur.format(Number(l.importe)||0)}</td>
    </tr>`).join("") || `<tr><td colspan="4" style="color:var(--ink-soft)">Todavía no hay conceptos añadidos.</td></tr>`;

  $("contenido").innerHTML = `
    ${pintarStepper(o.estado)}

    <div class="bloque">
      <h3>Datos de la reparación</h3>
      <div class="datos">
        <div><b>Entrada:</b> ${fFecha.format(new Date(o.fechaEntrada))}</div>
        <div><b>Salida:</b> ${o.fechaSalida ? fFecha.format(new Date(o.fechaSalida)) : "—"}</div>
      </div>
      <div style="margin-top:10px"><b>Problema:</b> ${o.descripcionProblema || "—"}</div>
    </div>

    <div class="bloque">
      <h3>Conceptos</h3>
      <table>
        <thead><tr><th>Concepto</th><th class="n">Cant.</th><th class="n">Precio</th><th class="n">Importe</th></tr></thead>
        <tbody>${filas}</tbody>
      </table>
      <div class="total"><span>Total</span><b>${eur.format(Number(o.total)||0)}</b></div>
    </div>

    ${pintarPresupuesto(o)}

    <div class="bloque">
      <a class="pdf" href="/api/seguimiento/${codigo}/pdf" target="_blank" rel="noopener">Descargar PDF</a>
    </div>

    ${pintarHistorial(o.historial)}`;

  if (o.presupuestoAprobado === null){
    $("btnAceptar").onclick = () => responder(true);
    $("btnRechazar").onclick = () => responder(false);
  }
}

async function responder(aprobado){
  try{
    const r = await fetch(`/api/seguimiento/${codigo}/presupuesto`, {
      method:"PUT", headers:{"Content-Type":"application/json"}, body:JSON.stringify({aprobado})
    });
    if (!r.ok) throw new Error("HTTP " + r.status);
    pintar(await r.json());
  }catch(e){
    mostrarError("No se ha podido registrar tu respuesta. Inténtalo de nuevo.");
  }
}

async function cargar(){
  if (!codigo){
    mostrarError("Enlace no válido: falta el código de seguimiento.");
    return;
  }
  try{
    const r = await fetch(`/api/seguimiento/${codigo}`);
    if (!r.ok) throw new Error("HTTP " + r.status);
    pintar(await r.json());
  }catch(e){
    mostrarError("No se ha encontrado ninguna reparación con ese enlace.");
  }
}

cargar();
