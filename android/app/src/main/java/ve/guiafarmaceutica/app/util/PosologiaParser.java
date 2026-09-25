package ve.guiafarmaceutica.app.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PosologiaParser {

    public static class RangoPosologico {
        public double dosisPediatricaMgKg = 10.0;
        public double dosisAdultoFijaMg = 500.0;
        public double pesoCorteAdultoKg = 50.0;

        public RangoPosologico(double dosisPediatricaMgKg, double dosisAdultoFijaMg, double pesoCorteAdultoKg) {
            this.dosisPediatricaMgKg = dosisPediatricaMgKg;
            this.dosisAdultoFijaMg = dosisAdultoFijaMg;
            this.pesoCorteAdultoKg = pesoCorteAdultoKg;
        }
    }

    private static final Pattern PATRON_MG_KG = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*mg\\s*[/]\\s*kg");
    private static final Pattern PATRON_DOSIS_MG = Pattern.compile("(?i)(?:dosis|posología)\\s*[^\\d]*(\\d+(?:[.,]\\d+)?)\\s*mg");
    private static final Pattern PATRON_ADULTO_1000 = Pattern.compile("(?i)(?:1000|1\\s*g|1000\\s*mg)");

    public static Double extraerDosisMgKg(String posologiaTexto) {
        RangoPosologico rango = extraerRangoPosologico(posologiaTexto);
        return rango != null ? rango.dosisPediatricaMgKg : null;
    }

    public static RangoPosologico extraerRangoPosologico(String posologiaTexto) {
        if (posologiaTexto == null || posologiaTexto.trim().isEmpty()) {
            return new RangoPosologico(10.0, 500.0, 50.0);
        }

        String texto = posologiaTexto.trim().replace(',', '.');
        double dosisPediatrica = 10.0;
        double dosisAdulto = 500.0;

        // Detectar si la dosis de adulto es 1000 mg (ej: paracetamol)
        if (PATRON_ADULTO_1000.matcher(texto).find()) {
            dosisAdulto = 1000.0;
        }

        // 1. Buscar mg/kg pediátrico
        Matcher matcherMgKg = PATRON_MG_KG.matcher(texto);
        if (matcherMgKg.find()) {
            try {
                double val = Double.parseDouble(matcherMgKg.group(1));
                if (val > 0 && val < 500) {
                    dosisPediatrica = val;
                }
            } catch (Exception ignored) {}
        } else {
            Matcher matcherDosisMg = PATRON_DOSIS_MG.matcher(texto);
            if (matcherDosisMg.find()) {
                try {
                    double val = Double.parseDouble(matcherDosisMg.group(1));
                    if (val > 0 && val < 500) {
                        dosisPediatrica = val;
                    }
                } catch (Exception ignored) {}
            }
        }

        return new RangoPosologico(dosisPediatrica, dosisAdulto, 50.0);
    }
}
