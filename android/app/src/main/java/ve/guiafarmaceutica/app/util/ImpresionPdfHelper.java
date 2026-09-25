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
        canvas.drawText("PESO: " + (impresion.paciente_peso != null ? impresion.paciente_peso : "S/I"), 200, y, paint);
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
            String indicacion = "    Dosis: " + d.dosificacion + " - " + d.frecuencia_instrucciones + " (Duración: " + d.duracion_dias + ")";
            canvas.drawText(indicacion, 50, y, paint);

            y += 22;
            num++;
        }

        // Observaciones adicionales
        if (impresion.observaciones != null && !impresion.observaciones.isEmpty()) {
            y += 10;
            paint.setColor(Color.parseColor("#616161"));
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("RECOMENDACIONES CLÍNICAS ADICIONALES:", 40, y, paint);

            y += 16;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            canvas.drawText(impresion.observaciones, 40, y, paint);
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
