package ve.guiafarmaceutica.app.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PresentacionParser {

    public static class DatosPresentacion {
        public double concentracionMg = 250.0;
        public double volumenMl = 5.0;
        public boolean encontrado = false;

        public DatosPresentacion(double concentracionMg, double volumenMl, boolean encontrado) {
            this.concentracionMg = concentracionMg;
            this.volumenMl = volumenMl;
            this.encontrado = encontrado;
        }
    }

    private static final Pattern PATRON_MG_ML = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*mg\\s*[/|en\\s]+\\s*(\\d+(?:[.,]\\d+)?)\\s*ml");
    private static final Pattern PATRON_MG_POR_ML = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*mg\\s*[/]\\s*ml");
    private static final Pattern PATRON_SOLO_MG = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*mg");

    public static DatosPresentacion extraer(String nombreMedicamento) {
        if (nombreMedicamento == null || nombreMedicamento.trim().isEmpty()) {
            return new DatosPresentacion(250.0, 5.0, false);
        }

        String texto = nombreMedicamento.trim().replace(',', '.');

        // 1. Coincidencia patrón "250 mg / 5 ml" o "100mg/5ml" o "120 mg en 5 ml"
        Matcher matcherMgMl = PATRON_MG_ML.matcher(texto);
        if (matcherMgMl.find()) {
            try {
                double conc = Double.parseDouble(matcherMgMl.group(1));
                double vol = Double.parseDouble(matcherMgMl.group(2));
                if (conc > 0 && vol > 0) {
                    return new DatosPresentacion(conc, vol, true);
                }
            } catch (Exception ignored) {}
        }

        // 2. Coincidencia patrón "100 mg / ml" (implícito 1 ml)
        Matcher matcherPorMl = PATRON_MG_POR_ML.matcher(texto);
        if (matcherPorMl.find()) {
            try {
                double conc = Double.parseDouble(matcherPorMl.group(1));
                if (conc > 0) {
                    return new DatosPresentacion(conc, 1.0, true);
                }
            } catch (Exception ignored) {}
        }

        // 3. Coincidencia solo mg (ej: "500 mg")
        Matcher matcherMg = PATRON_SOLO_MG.matcher(texto);
        if (matcherMg.find()) {
            try {
                double conc = Double.parseDouble(matcherMg.group(1));
                if (conc > 0) {
                    return new DatosPresentacion(conc, 5.0, true);
                }
            } catch (Exception ignored) {}
        }

        return new DatosPresentacion(250.0, 5.0, false);
    }
}
