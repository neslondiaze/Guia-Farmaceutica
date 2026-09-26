package ve.guiafarmaceutica.app.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import ve.guiafarmaceutica.app.data.ImpresionDiagnostica;
import ve.guiafarmaceutica.app.data.ImpresionDetalle;
import ve.guiafarmaceutica.app.data.IndicacionNoFarmacologica;
import ve.guiafarmaceutica.app.data.Paciente;
import ve.guiafarmaceutica.app.repository.SettingsRepository;

public class ImpresionPdfHelper {

    private static String obtenerIndicacionAsociada(int num, ImpresionDetalle d, String observaciones) {
        String base = "    Dosis: " + (d.dosificacion != null && !d.dosificacion.equals("-") && !d.dosificacion.isEmpty() ? d.dosificacion : "Según indicación médica");
        if (d.frecuencia_instrucciones != null && !d.frecuencia_instrucciones.isEmpty()) {
            base += " - " + d.frecuencia_instrucciones;
        }

        if (observaciones != null && !observaciones.isEmpty()) {
            String[] lineas = observaciones.split("\n");

            String prefix1 = num + ".";
            String prefix2 = num + ")";
            String prefix3 = num + ":";
            String prefix4 = num + "-";
            for (String linea : lineas) {
                linea = linea.trim();
                String encontrada = null;
                if (linea.startsWith(prefix1)) encontrada = linea.substring(prefix1.length()).trim();
                else if (linea.startsWith(prefix2)) encontrada = linea.substring(prefix2.length()).trim();
                else if (linea.startsWith(prefix3)) encontrada = linea.substring(prefix3.length()).trim();
                else if (linea.startsWith(prefix4)) encontrada = linea.substring(prefix4.length()).trim();

                if (encontrada != null && !encontrada.isEmpty()) {
                    return "    " + encontrada;
                }
            }

            int index = num - 1;
            if (index >= 0 && index < lineas.length) {
                String lineaPos = lineas[index].trim();
                if (!lineaPos.isEmpty()) {
                    if (lineaPos.length() > 2 && Character.isDigit(lineaPos.charAt(0))) {
                        int spaceIdx = lineaPos.indexOf(' ');
                        if (spaceIdx != -1 && spaceIdx < 4) {
                            lineaPos = lineaPos.substring(spaceIdx + 1).trim();
                        }
                    }
                    if (!lineaPos.isEmpty()) {
                        return "    " + lineaPos;
                    }
                }
            }
        }

        return base;
    }

    public static File generarPdfImpresion(Context context, ImpresionDiagnostica impresion, List<ImpresionDetalle> detalles) throws IOException {
        SettingsRepository settings = new SettingsRepository(context);

        int pageWidth = 595;  // A4 width in points
        int pageHeight = 842; // A4 height in points

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int y = 50;

        // 1. Membrete del Médico (Encabezado)
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(settings.getMedicoNombre(), 40, y, paint);

        y += 20;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(settings.getMedicoEspecialidad() + " | " + settings.getMedicoMpps(), 40, y, paint);

        y += 18;
        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#616161"));
        canvas.drawText(settings.getMedicoClinica() + " - " + settings.getMedicoDireccion(), 40, y, paint);

        y += 16;
        canvas.drawText("Telf: " + settings.getMedicoTelefono() + " | Email: " + settings.getMedicoEmail(), 40, y, paint);

        // Línea divisoria
        y += 15;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setStrokeWidth(2);
        canvas.drawLine(40, y, pageWidth - 40, y, paint);

        // 2. Título de Impresión Diagnóstica y Paciente
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("INFORME DE IMPRESIÓN DIAGNÓSTICA", 40, y, paint);

        paint.setColor(Color.parseColor("#616161"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Fecha: " + impresion.fecha_creacion, pageWidth - 160, y, paint);

        y += 25;
        paint.setColor(Color.parseColor("#212121"));
        paint.setTextSize(11);
        canvas.drawText("PACIENTE: " + impresion.paciente_nombre, 40, y, paint);
        canvas.drawText("C.I. / ID: " + (impresion.paciente_cedula != null ? impresion.paciente_cedula : "S/I"), 320, y, paint);

        y += 18;
        canvas.drawText("EDAD: " + (impresion.paciente_edad != null ? impresion.paciente_edad : "S/I"), 40, y, paint);
        canvas.drawText("IMPRESIÓN DIAGNÓSTICA: " + (impresion.diagnostico != null ? impresion.diagnostico : "Consulta General"), 320, y, paint);

        // Línea divisoria
        y += 15;
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(40, y, pageWidth - 40, y, paint);

        // 3. Medicamentos de Referencia
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(13);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("MEDICAMENTOS DE REFERENCIA DIAGNÓSTICA", 40, y, paint);

        y += 20;
        int num = 1;
        for (ImpresionDetalle d : detalles) {
            paint.setColor(Color.parseColor("#1565C0"));
            paint.setTextSize(11);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(num + ". " + d.medicamento_nombre, 50, y, paint);

            y += 16;
            paint.setColor(Color.parseColor("#333333"));
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            String indicacion = obtenerIndicacionAsociada(num, d, impresion.observaciones);
            canvas.drawText(indicacion, 50, y, paint);

            y += 22;
            num++;
        }

        // 4. Firma y Sello
        y = pageHeight - 120;
        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setStrokeWidth(1);
        canvas.drawLine(pageWidth - 220, y, pageWidth - 60, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Firma y Sello del Médico", pageWidth - 200, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("Informe de Impresión Diagnóstica emitido mediante Guía Farmacéutica Venezuela", 40, pageHeight - 30, paint);

        document.finishPage(page);

        File pdfDir = new File(context.getCacheDir(), "impresiones");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        File file = new File(pdfDir, "Impresion_" + impresion.id + "_" + System.currentTimeMillis() + ".pdf");
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();

        return file;
    }

    public static File generarPdfRecipeEIndicaciones(Context context, ImpresionDiagnostica impresion, List<ImpresionDetalle> detalles) throws IOException {
        SettingsRepository settings = new SettingsRepository(context);

        int pageWidth = 595;  // A4 width in points
        int pageHeight = 842; // A4 height in points

        PdfDocument document = new PdfDocument();

        // ==========================================
        // PÁGINA 1: RÉCIPE MÉDICO
        // ==========================================
        PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page1 = document.startPage(pageInfo1);
        Canvas canvas1 = page1.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int y = 50;

        // Membrete del Médico
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas1.drawText(settings.getMedicoNombre(), 40, y, paint);

        y += 20;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas1.drawText(settings.getMedicoEspecialidad() + " | " + settings.getMedicoMpps(), 40, y, paint);

        y += 18;
        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#616161"));
        canvas1.drawText(settings.getMedicoClinica() + " - " + settings.getMedicoDireccion(), 40, y, paint);

        y += 16;
        canvas1.drawText("Telf: " + settings.getMedicoTelefono() + " | Email: " + settings.getMedicoEmail(), 40, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setStrokeWidth(2);
        canvas1.drawLine(40, y, pageWidth - 40, y, paint);

        // Título: RÉCIPE MÉDICO
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas1.drawText("RÉCIPE MÉDICO", 40, y, paint);

        paint.setColor(Color.parseColor("#616161"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas1.drawText("Fecha: " + impresion.fecha_creacion, pageWidth - 160, y, paint);

        y += 25;
        paint.setColor(Color.parseColor("#212121"));
        paint.setTextSize(11);
        canvas1.drawText("PACIENTE: " + impresion.paciente_nombre, 40, y, paint);
        canvas1.drawText("C.I. / ID: " + (impresion.paciente_cedula != null ? impresion.paciente_cedula : "S/I"), 320, y, paint);

        y += 18;
        canvas1.drawText("EDAD: " + (impresion.paciente_edad != null ? impresion.paciente_edad : "S/I"), 40, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStrokeWidth(1);
        canvas1.drawLine(40, y, pageWidth - 40, y, paint);

        // Medicamentos indicados o seleccionados
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(13);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas1.drawText("MEDICAMENTOS INDICADOS", 40, y, paint);

        y += 20;
        int num = 1;
        for (ImpresionDetalle d : detalles) {
            paint.setColor(Color.parseColor("#1565C0"));
            paint.setTextSize(12);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas1.drawText(num + ". " + d.medicamento_nombre, 50, y, paint);

            y += 24;
            num++;
        }

        // Pie de página: Firma, Sello y "Lugar del sello"
        y = pageHeight - 120;
        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setStrokeWidth(1);
        canvas1.drawLine(pageWidth - 220, y, pageWidth - 60, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas1.drawText("Firma y Sello del Médico", pageWidth - 200, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#D32F2F"));
        paint.setTextSize(9);
        canvas1.drawText("[ Lugar del Sello ]", pageWidth - 170, y, paint);

        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas1.drawText("Récipe Médico emitido mediante Guía Farmacéutica Venezuela", 40, pageHeight - 30, paint);

        document.finishPage(page1);

        // ==========================================
        // PÁGINA 2: INDICACIONES MÉDICAS
        // ==========================================
        PdfDocument.PageInfo pageInfo2 = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create();
        PdfDocument.Page page2 = document.startPage(pageInfo2);
        Canvas canvas2 = page2.getCanvas();

        y = 50;

        // Membrete del Médico
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas2.drawText(settings.getMedicoNombre(), 40, y, paint);

        y += 20;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas2.drawText(settings.getMedicoEspecialidad() + " | " + settings.getMedicoMpps(), 40, y, paint);

        y += 18;
        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#616161"));
        canvas2.drawText(settings.getMedicoClinica() + " - " + settings.getMedicoDireccion(), 40, y, paint);

        y += 16;
        canvas2.drawText("Telf: " + settings.getMedicoTelefono() + " | Email: " + settings.getMedicoEmail(), 40, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setStrokeWidth(2);
        canvas2.drawLine(40, y, pageWidth - 40, y, paint);

        // Título: INDICACIONES MÉDICAS
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas2.drawText("INDICACIONES MÉDICAS", 40, y, paint);

        paint.setColor(Color.parseColor("#616161"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas2.drawText("Fecha: " + impresion.fecha_creacion, pageWidth - 160, y, paint);

        y += 25;
        paint.setColor(Color.parseColor("#212121"));
        paint.setTextSize(11);
        canvas2.drawText("PACIENTE: " + impresion.paciente_nombre, 40, y, paint);
        canvas2.drawText("C.I. / ID: " + (impresion.paciente_cedula != null ? impresion.paciente_cedula : "S/I"), 320, y, paint);

        y += 18;
        canvas2.drawText("EDAD: " + (impresion.paciente_edad != null ? impresion.paciente_edad : "S/I"), 40, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStrokeWidth(1);
        canvas2.drawLine(40, y, pageWidth - 40, y, paint);

        // Contenido de Indicaciones Médicas / Observaciones y Dosificación detallada
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(13);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas2.drawText("DETALLE DE DOSIFICACIÓN Y POSOLOGÍA", 40, y, paint);

        y += 20;
        int numInd = 1;
        for (ImpresionDetalle d : detalles) {
            paint.setColor(Color.parseColor("#1565C0"));
            paint.setTextSize(11);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas2.drawText(numInd + ". " + d.medicamento_nombre, 50, y, paint);

            y += 16;
            paint.setColor(Color.parseColor("#333333"));
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            String indicacion = obtenerIndicacionAsociada(numInd, d, impresion.observaciones);
            canvas2.drawText(indicacion, 50, y, paint);

            y += 22;
            numInd++;
        }

        // Pie de página: Firma, Sello y "Lugar del sello"
        y = pageHeight - 120;
        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setStrokeWidth(1);
        canvas2.drawLine(pageWidth - 220, y, pageWidth - 60, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas2.drawText("Firma y Sello del Médico", pageWidth - 200, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#D32F2F"));
        paint.setTextSize(9);
        canvas2.drawText("[ Lugar del Sello ]", pageWidth - 170, y, paint);

        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas2.drawText("Indicaciones Médicas emitidas mediante Guía Farmacéutica Venezuela", 40, pageHeight - 30, paint);

        document.finishPage(page2);

        File pdfDir = new File(context.getCacheDir(), "impresiones");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        File file = new File(pdfDir, "Recipe_e_Indicaciones_" + impresion.id + "_" + System.currentTimeMillis() + ".pdf");
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();

        return file;
    }

    public static File generarPdfIndicacion(Context context, IndicacionNoFarmacologica ind, Paciente paciente) throws IOException {
        SettingsRepository settings = new SettingsRepository(context);

        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int y = 50;

        // 1. Encabezado Médico
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(settings.getMedicoNombre(), 40, y, paint);

        y += 20;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(settings.getMedicoEspecialidad() + " | " + settings.getMedicoMpps(), 40, y, paint);

        y += 18;
        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#616161"));
        canvas.drawText(settings.getMedicoClinica() + " - " + settings.getMedicoDireccion(), 40, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setStrokeWidth(2);
        canvas.drawLine(40, y, pageWidth - 40, y, paint);

        // 2. Título e Identificación
        y += 30;
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PLAN DE CUIDADOS E INDICACIONES MÉDICAS", 40, y, paint);

        paint.setColor(Color.parseColor("#616161"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Fecha: " + (ind.fecha_creacion != null ? ind.fecha_creacion : "05/09/2026"), pageWidth - 160, y, paint);

        y += 25;
        paint.setColor(Color.parseColor("#212121"));
        paint.setTextSize(11);
        String nomPac = paciente != null ? paciente.nombre_completo : ind.paciente_nombre;
        String idPac = paciente != null ? paciente.identificacion : "DEMO-10482";
        canvas.drawText("PACIENTE: " + nomPac, 40, y, paint);
        canvas.drawText("C.I. / ID: " + idPac, 320, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#E0E0E0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(40, y, pageWidth - 40, y, paint);

        // 3. Secciones de Cuidados
        y += 25;
        drawSeccionIndicacion(canvas, paint, "ACTIVIDAD O REPOSO:", ind.actividad_reposo, 40, y);
        y += 35;
        drawSeccionIndicacion(canvas, paint, "ALIMENTACIÓN Y DIETA:", ind.alimentacion, 40, y);
        y += 35;
        drawSeccionIndicacion(canvas, paint, "HIDRATACIÓN:", ind.hidratacion, 40, y);
        y += 35;
        drawSeccionIndicacion(canvas, paint, "CUIDADOS GENERALES:", ind.cuidados_generales, 40, y);
        y += 35;
        drawSeccionIndicacion(canvas, paint, "ESTUDIOS SOLICITADOS:", ind.estudios_solicitados, 40, y);
        y += 35;
        drawSeccionIndicacion(canvas, paint, "SEÑALES DE ALARMA:", ind.senales_alarma, 40, y);
        y += 35;
        drawSeccionIndicacion(canvas, paint, "FECHA DE SEGUIMIENTO:", ind.fecha_seguimiento, 40, y);

        // 4. Firma
        y = pageHeight - 120;
        paint.setColor(Color.parseColor("#9E9E9E"));
        paint.setStrokeWidth(1);
        canvas.drawLine(pageWidth - 220, y, pageWidth - 60, y, paint);

        y += 15;
        paint.setColor(Color.parseColor("#424242"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Firma y Sello del Médico", pageWidth - 200, y, paint);

        document.finishPage(page);

        File pdfDir = new File(context.getCacheDir(), "impresiones");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        File file = new File(pdfDir, "Indicacion_" + ind.id + "_" + System.currentTimeMillis() + ".pdf");
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();

        return file;
    }

    private static void drawSeccionIndicacion(Canvas canvas, Paint paint, String titulo, String contenido, int x, int y) {
        paint.setColor(Color.parseColor("#0B4F9C"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(titulo, x, y, paint);

        paint.setColor(Color.parseColor("#333333"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        String text = (contenido != null && !contenido.isEmpty()) ? contenido : "Sin especificación particular.";
        canvas.drawText(text, x + 160, y, paint);
    }
}
