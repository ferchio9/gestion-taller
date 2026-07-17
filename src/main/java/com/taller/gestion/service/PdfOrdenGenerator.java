package com.taller.gestion.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.LineaOrden;
import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Vehiculo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Genera el PDF de una orden de reparación. Solo sabe construir el documento;
// no accede a la base de datos (eso lo hace OrdenReparacionService antes de llamar aquí).
@Component
public class PdfOrdenGenerator {

    private static final Logger log = LoggerFactory.getLogger(PdfOrdenGenerator.class);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 76, 129));
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private static final Font SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(22, 32, 43));
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(22, 32, 43));
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(91, 107, 122));
    private static final Font CABECERA_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(22, 32, 43));
    private static final Font AVISO = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    private static final Map<String, String> ESTADO_ETQ = Map.of(
            "RECEPCION", "Recepción",
            "DIAGNOSTICO", "Diagnóstico",
            "EN_REPARACION", "En reparación",
            "LISTO", "Listo",
            "ENTREGADO", "Entregado");

    private static final Map<String, String> TIPO_ETQ = Map.of(
            "COCHE", "Coche",
            "FURGONETA", "Furgoneta",
            "MOTOCICLETA", "Motocicleta",
            "CAMION", "Camión");

    public byte[] generar(OrdenReparacion orden) {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, salida);
            doc.open();

            cabecera(doc, orden);
            datosVehiculoYCliente(doc, orden);
            datosOrden(doc, orden);
            tablaConceptos(doc, orden);
            aviso(doc);

            doc.close();
        } catch (DocumentException e) {
            log.error("Error generando el PDF de la orden id={}", orden.getIdOrden(), e);
            throw new RuntimeException("No se ha podido generar el PDF de la orden", e);
        }
        return salida.toByteArray();
    }

    private void cabecera(Document doc, OrdenReparacion orden) throws DocumentException {
        Paragraph titulo = new Paragraph("Orden de reparación nº " + orden.getIdOrden(), TITULO);
        doc.add(titulo);
        Paragraph generado = new Paragraph(
                "Generado el " + java.time.LocalDateTime.now().format(FECHA_HORA), SUBTITULO);
        generado.setSpacingAfter(16);
        doc.add(generado);
    }

    private void datosVehiculoYCliente(Document doc, OrdenReparacion orden) throws DocumentException {
        Vehiculo v = orden.getVehiculo();
        Cliente c = v.getCliente();

        doc.add(new Paragraph("Vehículo", SECCION));
        PdfPTable t = tablaDatos(2);
        fila(t, "Matrícula", v.getMatricula());
        fila(t, "Tipo", TIPO_ETQ.getOrDefault(v.getTipo(), v.getTipo()));
        fila(t, "Marca y modelo", v.getMarca() + " " + v.getModelo());
        fila(t, "Año", v.getAnio() != null ? String.valueOf(v.getAnio()) : "—");
        t.setSpacingAfter(12);
        doc.add(t);

        doc.add(new Paragraph("Propietario", SECCION));
        PdfPTable tc = tablaDatos(2);
        fila(tc, "Nombre", c.getNombre());
        fila(tc, "Teléfono", c.getTelefono() != null ? c.getTelefono() : "—");
        tc.setSpacingAfter(12);
        doc.add(tc);
    }

    private void datosOrden(Document doc, OrdenReparacion orden) throws DocumentException {
        doc.add(new Paragraph("Datos de la reparación", SECCION));
        PdfPTable t = tablaDatos(2);
        fila(t, "Estado", ESTADO_ETQ.getOrDefault(orden.getEstado(), orden.getEstado()));
        fila(t, "Fecha de entrada", orden.getFechaEntrada() != null ? orden.getFechaEntrada().format(FECHA) : "—");
        fila(t, "Fecha de salida", orden.getFechaSalida() != null ? orden.getFechaSalida().format(FECHA) : "—");
        fila(t, "Kilómetros", orden.getKmEntrada() != null ? orden.getKmEntrada() + " km" : "—");
        t.setSpacingAfter(10);
        doc.add(t);

        Paragraph problema = new Paragraph();
        problema.add(new Chunk("Avería o motivo de entrada: ", ETIQUETA));
        problema.add(new Chunk(
                orden.getDescripcionProblema() != null ? orden.getDescripcionProblema() : "—", NORMAL));
        problema.setSpacingAfter(16);
        doc.add(problema);
    }

    private void tablaConceptos(Document doc, OrdenReparacion orden) throws DocumentException {
        doc.add(new Paragraph("Conceptos", SECCION));

        PdfPTable t = new PdfPTable(new float[]{4, 1, 1.3f, 1.3f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);

        for (String columna : new String[]{"Concepto", "Cant.", "Precio", "Importe"}) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, CABECERA_TABLA));
            celda.setBackgroundColor(new Color(15, 76, 129));
            celda.setPadding(6);
            celda.setHorizontalAlignment(Element.ALIGN_LEFT);
            t.addCell(celda);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<LineaOrden> lineas = orden.getLineas();
        if (lineas == null || lineas.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Esta orden de reparación aún no tiene conceptos.", NORMAL));
            vacio.setColspan(4);
            vacio.setPadding(8);
            vacio.setBorderColor(new Color(221, 227, 233));
            t.addCell(vacio);
        } else {
            for (LineaOrden l : lineas) {
                BigDecimal importe = l.getCantidad().multiply(l.getPrecioUnitario());
                total = total.add(importe);
                celdaTexto(t, l.getDescripcion());
                celdaNumero(t, l.getCantidad().stripTrailingZeros().toPlainString());
                celdaNumero(t, euro(l.getPrecioUnitario()));
                celdaNumero(t, euro(importe));
            }
        }
        doc.add(t);

        Paragraph totalPar = new Paragraph("Total: " + euro(total), TOTAL);
        totalPar.setAlignment(Element.ALIGN_RIGHT);
        totalPar.setSpacingBefore(10);
        totalPar.setSpacingAfter(20);
        doc.add(totalPar);
    }

    private void aviso(Document doc) throws DocumentException {
        Paragraph p = new Paragraph(
                "Este documento es un resguardo informativo de la reparación y no tiene validez como factura.",
                AVISO);
        doc.add(p);
    }

    // ---------- helpers de maquetación ----------

    private PdfPTable tablaDatos(int columnas) {
        PdfPTable t = new PdfPTable(columnas);
        t.setWidthPercentage(100);
        try { t.setWidths(new float[]{1, 2}); } catch (DocumentException ignored) { }
        return t;
    }

    private void fila(PdfPTable t, String etiqueta, String valor) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, ETIQUETA));
        e.setBorder(Rectangle.NO_BORDER);
        e.setPaddingBottom(4);
        PdfPCell v = new PdfPCell(new Phrase(valor, NORMAL));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPaddingBottom(4);
        t.addCell(e);
        t.addCell(v);
    }

    private void celdaTexto(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NORMAL));
        c.setPadding(6);
        c.setBorderColor(new Color(221, 227, 233));
        t.addCell(c);
    }

    private void celdaNumero(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NORMAL));
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBorderColor(new Color(221, 227, 233));
        t.addCell(c);
    }

    private String euro(BigDecimal valor) {
        return String.format(new Locale("es", "ES"), "%,.2f €", valor);
    }
}
