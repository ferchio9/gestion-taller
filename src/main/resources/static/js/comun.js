"use strict";
/* Helpers compartidos por todas las páginas. Se carga ANTES que el JS de cada página. */

const $ = id => document.getElementById(id);

const eur = new Intl.NumberFormat("es-ES", {style:"currency", currency:"EUR"});
const fFecha = new Intl.DateTimeFormat("es-ES", {day:"2-digit", month:"short", year:"numeric"});
const fFechaHora = new Intl.DateTimeFormat("es-ES", {day:"2-digit", month:"short", year:"numeric", hour:"2-digit", minute:"2-digit"});

// Estados en orden de flujo, con su color de CSS.
const ESTADOS = [
  {clave:"RECEPCION",     etq:"Recepción",     color:"var(--st-recepcion)"},
  {clave:"DIAGNOSTICO",   etq:"Diagnóstico",   color:"var(--st-diagnostico)"},
  {clave:"EN_REPARACION", etq:"En reparación", color:"var(--st-reparacion)"},
  {clave:"LISTO",         etq:"Listo",         color:"var(--st-listo)"},
  {clave:"ENTREGADO",     etq:"Entregado",     color:"var(--st-entregado)"},
];
const INFO = Object.fromEntries(ESTADOS.map(e => [e.clave, e]));
const ETIQUETA = Object.fromEntries(ESTADOS.map(e => [e.clave, e.etq]));

// Estados de una cita (agenda), usados por citas.js y por la ficha de vehículo/cliente.
const ETIQUETA_CITA = {PENDIENTE:"Pendiente", CONFIRMADA:"Confirmada", CANCELADA:"Cancelada", COMPLETADA:"Completada"};
const COLOR_CITA = {PENDIENTE:"var(--ink-soft)", CONFIRMADA:"var(--brand)", CANCELADA:"var(--danger)", COMPLETADA:"var(--st-listo)"};

// Importe de una línea y total de una orden (datos calculados, no guardados).
const importeLinea = l => (Number(l.cantidad)||0) * (Number(l.precioUnitario)||0);
const totalOrden = o => (o.lineas||[]).reduce((s,l) => s + importeLinea(l), 0);

// Tipos de vehículo (valor interno -> etiqueta corta para la interfaz).
const TIPOS = [
  {clave:"COCHE",       etq:"Coche"},
  {clave:"FURGONETA",   etq:"Furgoneta"},
  {clave:"MOTOCICLETA", etq:"Moto"},
  {clave:"CAMION",      etq:"Camión"},
];
const TIPO_ETQ = Object.fromEntries(TIPOS.map(t => [t.clave, t.etq]));

// Iconos de tipo de vehículo (Font Awesome Free, licencia CC BY 4.0).
// fill="currentColor" -> heredan el color de texto del elemento que los contiene.
const ICONO_TIPO = {
  COCHE: '<svg viewBox="0 0 640 512" fill="currentColor"><path d="M544 192h-16L419.22 56.02A64.025 64.025 0 0 0 369.24 32H155.33c-26.17 0-49.7 15.93-59.42 40.23L48 194.26C20.44 201.4 0 226.21 0 256v112c0 8.84 7.16 16 16 16h48c0 53.02 42.98 96 96 96s96-42.98 96-96h128c0 53.02 42.98 96 96 96s96-42.98 96-96h48c8.84 0 16-7.16 16-16v-80c0-53.02-42.98-96-96-96zM160 432c-26.47 0-48-21.53-48-48s21.53-48 48-48 48 21.53 48 48-21.53 48-48 48zm72-240H116.93l38.4-96H232v96zm48 0V96h89.24l76.8 96H280zm200 240c-26.47 0-48-21.53-48-48s21.53-48 48-48 48 21.53 48 48-21.53 48-48 48z"/></svg>',
  FURGONETA: '<svg viewBox="0 0 640 512" fill="currentColor"><path d="M628.88 210.65L494.39 49.27A48.01 48.01 0 0 0 457.52 32H32C14.33 32 0 46.33 0 64v288c0 17.67 14.33 32 32 32h32c0 53.02 42.98 96 96 96s96-42.98 96-96h128c0 53.02 42.98 96 96 96s96-42.98 96-96h32c17.67 0 32-14.33 32-32V241.38c0-11.23-3.94-22.1-11.12-30.73zM64 192V96h96v96H64zm96 240c-26.51 0-48-21.49-48-48s21.49-48 48-48 48 21.49 48 48-21.49 48-48 48zm160-240h-96V96h96v96zm160 240c-26.51 0-48-21.49-48-48s21.49-48 48-48 48 21.49 48 48-21.49 48-48 48zm-96-240V96h66.02l80 96H384z"/></svg>',
  MOTOCICLETA: '<svg viewBox="0 0 640 512" fill="currentColor"><path d="M512.9 192c-14.9-.1-29.1 2.3-42.4 6.9L437.6 144H520c13.3 0 24-10.7 24-24V88c0-13.3-10.7-24-24-24h-45.3c-6.8 0-13.3 2.9-17.8 7.9l-37.5 41.7-22.8-38C392.2 68.4 384.4 64 376 64h-80c-8.8 0-16 7.2-16 16v16c0 8.8 7.2 16 16 16h66.4l19.2 32H227.9c-17.7-23.1-44.9-40-99.9-40H72.5C59 104 47.7 115 48 128.5c.2 13 10.9 23.5 24 23.5h56c24.5 0 38.7 10.9 47.8 24.8l-11.3 20.5c-13-3.9-26.9-5.7-41.3-5.2C55.9 194.5 1.6 249.6 0 317c-1.6 72.1 56.3 131 128 131 59.6 0 109.7-40.8 124-96h84.2c13.7 0 24.6-11.4 24-25.1-2.1-47.1 17.5-93.7 56.2-125l12.5 20.8c-27.6 23.7-45.1 58.9-44.8 98.2.5 69.6 57.2 126.5 126.8 127.1 71.6.7 129.8-57.5 129.2-129.1-.7-69.6-57.6-126.4-127.2-126.9zM128 400c-44.1 0-80-35.9-80-80s35.9-80 80-80c4.2 0 8.4.3 12.5 1L99 316.4c-8.8 16 2.8 35.6 21 35.6h81.3c-12.4 28.2-40.6 48-73.3 48zm463.9-75.6c-2.2 40.6-35 73.4-75.5 75.5-46.1 2.5-84.4-34.3-84.4-79.9 0-21.4 8.4-40.8 22.1-55.1l49.4 82.4c4.5 7.6 14.4 10 22 5.5l13.7-8.2c7.6-4.5 10-14.4 5.5-22l-48.6-80.9c5.2-1.1 10.5-1.6 15.9-1.6 45.6-.1 82.3 38.2 79.9 84.3z"/></svg>',
  CAMION: '<svg viewBox="0 0 640 512" fill="currentColor"><path d="M624 352h-16V243.9c0-12.7-5.1-24.9-14.1-33.9L494 110.1c-9-9-21.2-14.1-33.9-14.1H416V48c0-26.5-21.5-48-48-48H48C21.5 0 0 21.5 0 48v320c0 26.5 21.5 48 48 48h16c0 53 43 96 96 96s96-43 96-96h128c0 53 43 96 96 96s96-43 96-96h48c8.8 0 16-7.2 16-16v-32c0-8.8-7.2-16-16-16zM160 464c-26.5 0-48-21.5-48-48s21.5-48 48-48 48 21.5 48 48-21.5 48-48 48zm320 0c-26.5 0-48-21.5-48-48s21.5-48 48-48 48 21.5 48 48-21.5 48-48 48zm80-208H416V144h44.1l99.9 99.9V256z"/></svg>'
};

// Placa española. La moto lleva placa cuadrada de dos líneas; el resto, rectangular.
function placaHtml(matricula, tipo){
  const clave = (tipo || "").toString().trim().toUpperCase();
  const svg = ICONO_TIPO[clave] || ICONO_TIPO.COCHE;
  const ico = `<span class="tipo-ico">${svg}</span>`;
  const m = (matricula || "").toUpperCase();
  const p = m.match(/^([0-9]{4})([A-Z]{3})$/);
  if (clave === "MOTOCICLETA" && p){
    return `<span class="placa-fila">${ico}<span class="placa moto"><span class="ue">E</span>` +
           `<span class="mm"><span class="mnum">${p[1]}</span><span class="mlet">${p[2]}</span></span></span></span>`;
  }
  const t = p ? p[1] + " " + p[2] : (m || "—");
  return `<span class="placa-fila">${ico}<span class="placa"><span class="ue">E</span><span class="mat">${t}</span></span></span>`;
}

// Pinta una fila de chips para filtrar por tipo de vehículo (reutilizable).
function pintarFiltroTipos(contId, actual, onSel){
  const cont = document.getElementById(contId);
  const ops = [{clave:"TODOS", etq:"Todos"}, ...TIPOS];
  cont.innerHTML = ops.map(o => {
    const ico = ICONO_TIPO[o.clave] ? `<span class="filtro-ico">${ICONO_TIPO[o.clave]}</span>` : "";
    return `<button class="filtro ${actual===o.clave?"activo":""}" data-t="${o.clave}">${ico}${o.etq}</button>`;
  }).join("");
  cont.querySelectorAll("button").forEach(b => b.onclick = () => onSel(b.dataset.t));
}

// ---- sesión: CSRF y cierre de sesión ----
// Envuelve el fetch global una vez: añade la cabecera CSRF a las peticiones que
// modifican datos (Spring Security la exige) y, si la sesión ha caducado (401),
// manda al login. Así el resto de ficheros (ordenes.js, registros.js, alta.js,
// crear.js, panel.js) siguen usando fetch() tal cual, sin tocar cada llamada.
function leerCookie(nombre){
  const m = document.cookie.match(new RegExp("(^| )" + nombre + "=([^;]+)"));
  return m ? decodeURIComponent(m[2]) : null;
}
(function(){
  const fetchOriginal = window.fetch.bind(window);
  window.fetch = function(input, opciones){
    opciones = opciones || {};
    const metodo = (opciones.method || "GET").toUpperCase();
    if (["POST","PUT","PATCH","DELETE"].includes(metodo)){
      const token = leerCookie("XSRF-TOKEN");
      if (token){
        opciones = {...opciones, headers: new Headers(opciones.headers || {})};
        opciones.headers.set("X-XSRF-TOKEN", token);
      }
    }
    return fetchOriginal(input, opciones).then(r => {
      if (r.status === 401) location.href = "/login.html";
      return r;
    });
  };
})();

document.addEventListener("DOMContentLoaded", () => {
  const btn = document.getElementById("btnLogout");
  if (!btn) return;
  btn.onclick = e => {
    e.preventDefault();
    fetch("/logout", {method:"POST"}).finally(() => { location.href = "/login.html"; });
  };
});

// ---- sesión actual (rol) ----
// Bajo demanda: solo la llaman las páginas protegidas desde su init(). NUNCA se
// llama desde login.html ni seguimiento.html (son anónimas; un 401 aquí
// dispararía el redirect a login del wrapper de fetch de arriba).
let SESION = null;
async function cargarSesion(){
  if (SESION) return SESION;
  try{
    const r = await fetch("/api/sesion");
    if (!r.ok) throw new Error("HTTP " + r.status);
    SESION = await r.json();
  }catch(e){
    SESION = {username:null, rol:"ADMIN"}; // sin backend (modo demo): no ocultar nada
  }
  return SESION;
}

// ---- aviso de revisión próxima por kilometraje ----
// Intervalos orientativos de mantenimiento habituales por tipo de vehículo.
const INTERVALO_KM = {COCHE:15000, FURGONETA:15000, MOTOCICLETA:6000, CAMION:20000};

// ultimoKmEntrada: km de la última orden registrada para ese vehículo (o null si no tiene ninguna).
function proximaRevision(vehiculo, ultimoKmEntrada){
  const intervalo = INTERVALO_KM[vehiculo.tipo] || INTERVALO_KM.COCHE;
  const kmActual = Number(vehiculo.kmActual) || 0;
  const base = ultimoKmEntrada != null ? Number(ultimoKmEntrada) : 0;
  const recorridos = kmActual - base;
  return {
    toca: recorridos >= intervalo,
    kmRestantes: Math.max(0, intervalo - recorridos)
  };
}
