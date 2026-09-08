package com.example.http_server_andoid.logic;

import static com.example.http_server_andoid.MainActivity.SEPARADOR_JUGADAS;
import static com.example.http_server_andoid.util.Constantes.DOS_DECIMALES_FORMAT;
import static com.example.http_server_andoid.util.Constantes.SALTO_LINEA;
import static com.example.http_server_andoid.util.Constantes.TABULADOR;

import androidx.annotation.NonNull;

import com.example.http_server_andoid.MainActivity;
import com.example.http_server_andoid.model.Estadistica;
import com.example.http_server_andoid.model.Secciones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import kotlin.Pair;

public class Ruleta {
    public static String NUMEROS_VALIDOS_ORDENADOS = "00,1,13,36,24,3,15,34,22,5,17,32,20,7,11,30,26,9,28,0,2,14,35,23,4,16,33,21,6,18,31,19,8,12,29,25,10,27";
    private static String[] numerosRuletaArray = NUMEROS_VALIDOS_ORDENADOS.split(SEPARADOR_JUGADAS);

    private static Map<String, Pair<String, String>> mapaNumerosPuertas = inicializarMapaNumerosPuertas();
    private long ganancias;
    private int cantidadNumerosJugar;
    private final List<String> numerosJugar = new ArrayList<>();
    private double porcentajeAcumulado = 0;

    private int bolasRojas;
    private int bolasNegras;

    private int numeroPares;
    private int numerosImpares;

    private int numerosPrimerDocena;
    private int numerosSegundaDocena;
    private int numerosTerceraDocena;

    private String ultimoNumero;
    private String penultimoNumero;
    private String antepenultimoNumero;

    /** Full transition matrix: number X → {number Y → count} across all sessions. */
    private final Map<String, Map<String, Integer>> transiciones = new HashMap<>();
    private final Map<String, Map<String, Integer>> mapaJugadasDosAnteriores = new HashMap<>();
    private final Map<String, Map<String, Integer>> mapaJugadasTresAnteriores = new HashMap<>();

    /** Snapshot captured just before processing today's session (set during calcularResultado). */
    private RuletaSnapshot lastHistoricalSnapshot = null;
    private final Map<String, List<String>> mapaJugadasAnterioresDiaHoy = new HashMap<>();
    private final Map<String, AtomicInteger> mapaNumerosEstadisticasJugadasHoy = new HashMap<>();
    private final Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosConPuertas = inicializarMapaModulos();
    private final Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosSinPuertas = inicializarMapaModulos();
    private final Map<String, List<String>> mapaNumeroModulosPuertas = inicializarMapaModulosPuertas(mapaEstadisticasModulosConPuertas);
    private final Set<String> setNumerosAJugar = new LinkedHashSet<>();

    // Prediction Confidence Leaderboard
    private final Map<String, Integer> puntuacionConfianza = new HashMap<>();


    public String calcularResultado(String jugada) {
        Map<String, Estadistica> mapaEstadisticaPorNumero = inicializarMapa();

        List<String> diasJugadas = Arrays.asList(jugada.split(MainActivity.SEPARADOR_DIA));
        long cantidadTotalJugadas = diasJugadas.stream()
                .flatMap(dia -> Stream.of(dia.split(SEPARADOR_JUGADAS)))
                .count();
        String[] jugadaUltimoDia = diasJugadas.get(diasJugadas.size() - 1).split(SEPARADOR_JUGADAS);
        ultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 1];
        penultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 2];
        antepenultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 3];

        long spinsProcessed = 0;
        for (int j = 0; j < diasJugadas.size(); j++) {
            String diaActual = diasJugadas.get(j);
            List<String> numeros = Arrays.asList(diaActual.split(SEPARADOR_JUGADAS));

            // Capture snapshot just before processing today's session
            if (j == diasJugadas.size() - 1 && diasJugadas.size() > 1) {
                lastHistoricalSnapshot = crearSnapshotInterno(mapaEstadisticaPorNumero, spinsProcessed);
            }

            for (int i = 0; i < numeros.size(); i++) {
                String numeroActual = numeros.get(i);
                Estadistica estadistica = calcularEstadistica(numeroActual, cantidadTotalJugadas, mapaEstadisticaPorNumero);
                mapaEstadisticaPorNumero.put(numeroActual, estadistica);

                incrementarCountColorParDocena(estadistica);

                if (i < numeros.size() - 1) {
                    transiciones.computeIfAbsent(numeroActual, k -> new HashMap<>())
                            .merge(numeros.get(i + 1), 1, Integer::sum);
                }

                calcularDosAndTresJugadasAnterioresMapa(numeroActual, i, numeros);
                calcularJugadasModuloDiez(numeroActual, i, numeros);

                if (j == (diasJugadas.size() - 1) && i <= numeros.size() - 1) {
                    calcularJugadasAnterioresDiaDeHoyMapa(numeroActual, i, numeros);
                    incrementarNumeroJugadasHoy(mapaNumerosEstadisticasJugadasHoy, numeroActual);
                }
            }
            spinsProcessed += numeros.size();
        }

        LinkedHashMap<String, Estadistica> orderMap = getMapaOrderVecesCayo(mapaEstadisticaPorNumero);
        String mensaje = generarMensajeEstadisticas(cantidadTotalJugadas, orderMap);
        mensaje += calcularMensajeCuantoJugar(orderMap, cantidadTotalJugadas);
        calcularPuntuacionConfianza();
        mensaje += imprimirPuntuacionConfianza();

        return mensaje;
    }

    /**
     * Fast path: restore state from a pre-computed historical snapshot, then process only
     * today's session (the last '-'-separated segment of jugadaCompleta) before rendering.
     * Avoids re-processing all historical spins on every request.
     */
    public String calcularResultadoConHistorial(RuletaSnapshot snap, String jugadaCompleta) {
        // ── Restore historical state ──────────────────────────────────────────
        Map<String, Estadistica> mapaEstadisticaPorNumero = inicializarMapa();
        snap.vecesCayoHistorico.forEach((num, count) -> {
            Estadistica e = mapaEstadisticaPorNumero.get(num);
            if (e != null) e.setVecesCayo(count);
        });

        bolasRojas          = snap.bolasRojas;
        bolasNegras         = snap.bolasNegras;
        numeroPares         = snap.numeroPares;
        numerosImpares      = snap.numerosImpares;
        numerosPrimerDocena  = snap.numerosPrimerDocena;
        numerosSegundaDocena = snap.numerosSegundaDocena;
        numerosTerceraDocena = snap.numerosTerceraDocena;

        snap.transiciones.forEach((k, v) -> transiciones.put(k, new HashMap<>(v)));
        snap.dosGramas.forEach((k, v) -> mapaJugadasDosAnteriores.put(k, new HashMap<>(v)));
        snap.tresGramas.forEach((k, v) -> mapaJugadasTresAnteriores.put(k, new HashMap<>(v)));
        snap.moduloSinPuertas.forEach((k, v) ->
                mapaEstadisticasModulosSinPuertas.put(k,
                        new Pair<>(new AtomicInteger(v[0]),
                                   new AtomicInteger(v[1]))));
        snap.moduloConPuertas.forEach((k, v) ->
                mapaEstadisticasModulosConPuertas.put(k,
                        new Pair<>(new AtomicInteger(v[0]),
                                   new AtomicInteger(v[1]))));

        // ── Parse full input ──────────────────────────────────────────────────
        List<String> diasJugadas = Arrays.asList(jugadaCompleta.split(MainActivity.SEPARADOR_DIA));
        long cantidadTotalJugadas = diasJugadas.stream()
                .flatMap(dia -> Stream.of(dia.split(SEPARADOR_JUGADAS)))
                .count();
        String[] jugadaUltimoDia = diasJugadas.get(diasJugadas.size() - 1).split(SEPARADOR_JUGADAS);
        ultimoNumero       = jugadaUltimoDia[jugadaUltimoDia.length - 1];
        penultimoNumero    = jugadaUltimoDia[jugadaUltimoDia.length - 2];
        antepenultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 3];

        // ── Process only today's session ──────────────────────────────────────
        List<String> numeros = Arrays.asList(
                diasJugadas.get(diasJugadas.size() - 1).split(SEPARADOR_JUGADAS));
        for (int i = 0; i < numeros.size(); i++) {
            String n = numeros.get(i);
            Estadistica estadistica = calcularEstadistica(n, cantidadTotalJugadas, mapaEstadisticaPorNumero);
            mapaEstadisticaPorNumero.put(n, estadistica);
            incrementarCountColorParDocena(estadistica);

            if (i < numeros.size() - 1) {
                transiciones.computeIfAbsent(n, k -> new HashMap<>())
                        .merge(numeros.get(i + 1), 1, Integer::sum);
            }
            calcularDosAndTresJugadasAnterioresMapa(n, i, numeros);
            calcularJugadasModuloDiez(n, i, numeros);
            calcularJugadasAnterioresDiaDeHoyMapa(n, i, numeros);
            incrementarNumeroJugadasHoy(mapaNumerosEstadisticasJugadasHoy, n);
        }

        // Fix percentages for numbers that appeared only in history (porcentaje was 0 after restore)
        mapaEstadisticaPorNumero.values().forEach(e ->
                e.setPorcentaje(e.getVecesCayo() * 100.0 / cantidadTotalJugadas));

        // ── Render ────────────────────────────────────────────────────────────
        LinkedHashMap<String, Estadistica> orderMap = getMapaOrderVecesCayo(mapaEstadisticaPorNumero);
        String mensaje = generarMensajeEstadisticas(cantidadTotalJugadas, orderMap);
        mensaje += calcularMensajeCuantoJugar(orderMap, cantidadTotalJugadas);
        calcularPuntuacionConfianza();
        mensaje += imprimirPuntuacionConfianza();

        return mensaje;
    }

    public RuletaSnapshot getLastHistoricalSnapshot() {
        return lastHistoricalSnapshot;
    }

    private void incrementarNumeroJugadasHoy(Map<String, AtomicInteger> mapaEstadisticasJugadasHoy, String numeroActual) {
        mapaEstadisticasJugadasHoy.computeIfAbsent(numeroActual, key -> new AtomicInteger(0));
        mapaEstadisticasJugadasHoy.get(numeroActual).incrementAndGet();
    }

    private void calcularJugadasModuloDiez(String numeroActual, int i, List<String> numeros) {
        if (numeros.size() > i + 1) {
            String numeroSiguiente = numeros.get(i + 1);
            Integer numeroActualModulo = numeroActual.equals("00") ? -1 : Integer.parseInt(numeroActual) % 10;
            Integer numeroSiguienteModulo = numeroSiguiente.equals("00") ? -1 : Integer.parseInt(numeroSiguiente) % 10;
            if (numeroActualModulo == -1 || numeroSiguienteModulo == -1) {
                return;
            }

            mapaEstadisticasModulosSinPuertas.get(numeroActualModulo.toString()).component2().incrementAndGet();
            mapaEstadisticasModulosConPuertas.get(numeroActualModulo.toString()).component2().incrementAndGet();
            if (numeroActualModulo.equals(numeroSiguienteModulo)) {
                System.out.println("Modulo del " + numeroActualModulo + " SIN PUERTAS encontrado:" + numeroActual + " " + numeroSiguiente);
                mapaEstadisticasModulosSinPuertas.get(numeroActualModulo.toString()).component1().incrementAndGet();
                mapaEstadisticasModulosConPuertas.get(numeroActualModulo.toString()).component1().incrementAndGet();
            } else if (mapaNumeroModulosPuertas.get(numeroActualModulo.toString()).contains(numeroSiguiente)) {
                System.out.println("Modulo del " + numeroActualModulo + " CON PUERTAS encontrado:" + numeroActual + " " + numeroSiguiente);
                mapaEstadisticasModulosConPuertas.get(numeroActualModulo.toString()).component1().incrementAndGet();
            }
        }
    }

    private void calcularJugadasAnterioresDiaDeHoyMapa(String numeroActual, int i, List<String> numeros) {
        if (numeroActual.equals(ultimoNumero) && i < numeros.size() - 1) {
            String numeroSiguio = numeros.get(i + 1);
            List<String> ListSiguio = mapaJugadasAnterioresDiaHoy.computeIfAbsent(ultimoNumero, k -> new ArrayList<>());
            ListSiguio.add(numeroSiguio);
        }
    }

    private void calcularDosAndTresJugadasAnterioresMapa(String numeroActual, int i, List<String> numeros) {
        if (i > 0 && i < numeros.size() - 1) {
            String anteriorUltimo = numeros.get(i - 1);
            String numeroSiguio = numeros.get(i + 1);

            mapaJugadasDosAnteriores
                    .computeIfAbsent(anteriorUltimo + "-" + numeroActual, k -> new HashMap<>())
                    .merge(numeroSiguio, 1, Integer::sum);

            if (i > 1) {
                String anteriorAnteriorUltimo = numeros.get(i - 2);
                mapaJugadasTresAnteriores
                        .computeIfAbsent(anteriorAnteriorUltimo + "-" + anteriorUltimo + "-" + numeroActual, k -> new HashMap<>())
                        .merge(numeroSiguio, 1, Integer::sum);
            }
        }
    }

    @NonNull
    private static LinkedHashMap<String, Estadistica> getMapaOrderVecesCayo(Map<String, Estadistica> mapaEstadisticaPorNumero) {
        return mapaEstadisticaPorNumero.values().stream()
                .sorted(Comparator.comparingInt(Estadistica::getVecesCayo).reversed())
                .collect(Collectors.toMap(Estadistica::getNumero, m -> m,
                        (oldValue, newValue) -> {
                            throw new RuntimeException("Duplicates");
                        }, LinkedHashMap::new));
    }

    private String generarMensajeEstadisticas(long cantidadTotalJugadas, LinkedHashMap<String, Estadistica> orderMap) {
        StringBuilder mensaje = new StringBuilder();

        // Card 1: total balls
        mensaje.append("<div class=\"card\">")
               .append("<div class=\"total-bolas\">Numero total de bolas: <span>").append(cantidadTotalJugadas).append("</span></div>")
               .append("</div>");

        // Card 2: after last number
        mensaje.append(imprimirMapaNumerosDespuesCuantasVeces(
                transiciones.getOrDefault(ultimoNumero, Collections.emptyMap())));

        // Card 3: 2 and 3 number sequence patterns
        mensaje.append(imprimirMapaDosAndTresJugadasAnteriores(mapaJugadasDosAnteriores, mapaJugadasTresAnteriores));

        // Card 4: today's plays
        mensaje.append(imprimirMensajeJugadasHoy(mapaJugadasAnterioresDiaHoy, mapaNumerosEstadisticasJugadasHoy));

        // Resumen highlight box
        StringBuilder chips = new StringBuilder();
        setNumerosAJugar.forEach(n -> chips.append("<span class=\"resumen-chip\">").append(n).append("</span>"));
        mensaje.append("<div class=\"resumen-box\">")
               .append("<div class=\"card-header\">Resumen de numeros a jugar</div>")
               .append("<div class=\"resumen-numbers\">").append(chips).append("</div>")
               .append("</div>");

        // Card 5: per-number stats table
        mensaje.append("<div class=\"card\">")
               .append("<div class=\"card-header\">Estadisticas por numero</div>")
               .append("<table class=\"stats-table\"><thead><tr>")
               .append("<th>No</th><th>Cayo</th><th>Porcentaje</th>")
               .append("</tr></thead><tbody>");
        orderMap.forEach((k, v) ->
            mensaje.append("<tr>")
                   .append("<td class=\"num-cell\">").append(v.getNumero()).append("</td>")
                   .append("<td>").append(v.getVecesCayo()).append("</td>")
                   .append("<td>").append(DOS_DECIMALES_FORMAT.format(v.getPorcentaje())).append("%</td>")
                   .append("</tr>")
        );
        mensaje.append("</tbody></table></div>");

        // Card 6: distribution progress bars
        double pctRojo  = (double) bolasRojas   * 100 / cantidadTotalJugadas;
        double pctNegro = (double) bolasNegras  * 100 / cantidadTotalJugadas;
        double pctImpar = (double) numerosImpares * 100 / cantidadTotalJugadas;
        double pctPar   = (double) numeroPares    * 100 / cantidadTotalJugadas;
        double pctDoc1  = (double) numerosPrimerDocena   * 100 / cantidadTotalJugadas;
        double pctDoc2  = (double) numerosSegundaDocena  * 100 / cantidadTotalJugadas;
        double pctDoc3  = (double) numerosTerceraDocena  * 100 / cantidadTotalJugadas;

        mensaje.append("<div class=\"card\">")
               .append("<div class=\"card-header\">Distribucion</div>")
               .append("<div class=\"stat-grid\">")
               .append(buildProgressBar("ROJO",          bolasRojas,          pctRojo,  "rojo"))
               .append(buildProgressBar("NEGRO",         bolasNegras,         pctNegro, "negro"))
               .append(buildProgressBar("IMPARES",       numerosImpares,      pctImpar, "impar"))
               .append(buildProgressBar("PARES",         numeroPares,         pctPar,   "par"))
               .append(buildProgressBar("PRIMER DOCENA", numerosPrimerDocena, pctDoc1,  ""))
               .append(buildProgressBar("SEGUNDA DOCENA",numerosSegundaDocena,pctDoc2,  ""))
               .append(buildProgressBar("TERCER DOCENA", numerosTerceraDocena,pctDoc3,  ""))
               .append("</div></div>");

        // Card 7: modulo analysis
        mensaje.append(imprimirMensajeModulo(mapaEstadisticasModulosSinPuertas, mapaEstadisticasModulosConPuertas));

        return mensaje.toString();
    }

    private String buildProgressBar(String label, int count, double pct, String colorClass) {
        String fillClass = "progress-fill" + (colorClass.isEmpty() ? "" : " " + colorClass);
        return "<div class=\"stat-item\">"
             + "<div class=\"stat-item-label\">" + label
             + " <span>" + count + " &mdash; " + DOS_DECIMALES_FORMAT.format(pct) + "%</span></div>"
             + "<div class=\"progress-track\"><div class=\"" + fillClass + "\" style=\"width:"
             + DOS_DECIMALES_FORMAT.format(pct) + "%\"></div></div></div>";
    }

    private String imprimirMensajeModulo(Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosSinPuertas, Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosConPuertas) {
        Comparator<Map.Entry<String, Pair<Integer, Integer>>> comparator = Comparator.comparing(a -> a.getValue().component1());

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card\">")
          .append("<div class=\"card-header\">Analisis Modulo 10</div>");

        sb.append("<div class=\"seq-key\" style=\"margin-bottom:8px\">SIN contar puertas</div>")
          .append("<table class=\"modulo-table\"><thead><tr>")
          .append("<th>Modulo</th><th>Cayo</th><th>Posibles</th><th>Porcentaje</th>")
          .append("</tr></thead><tbody>");

        mapaEstadisticasModulosSinPuertas.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), new Pair<Integer, Integer>(entry.getValue().component1().get(), entry.getValue().component2().get())))
                .sorted(comparator.reversed())
                .forEach(entry -> {
                    float pct = (float) entry.getValue().component1() * 100 / entry.getValue().component2();
                    sb.append("<tr>")
                      .append("<td>").append(entry.getKey()).append("</td>")
                      .append("<td>").append(entry.getValue().component1()).append("</td>")
                      .append("<td>").append(entry.getValue().component2()).append("</td>")
                      .append("<td>").append(String.format("%.1f", pct)).append("%</td>")
                      .append("</tr>");
                });

        sb.append("</tbody></table>");

        sb.append("<div class=\"seq-key\" style=\"margin-top:16px;margin-bottom:8px\">CON contar puertas</div>")
          .append("<table class=\"modulo-table\"><thead><tr>")
          .append("<th>Modulo</th><th>Cayo</th><th>Posibles</th><th>Porcentaje</th>")
          .append("</tr></thead><tbody>");

        mapaEstadisticasModulosConPuertas.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), new Pair<Integer, Integer>(entry.getValue().component1().get(), entry.getValue().component2().get())))
                .sorted(comparator.reversed())
                .forEach(entry -> {
                    float pct = (float) entry.getValue().component1() * 100 / entry.getValue().component2();
                    sb.append("<tr>")
                      .append("<td>").append(entry.getKey()).append("</td>")
                      .append("<td>").append(entry.getValue().component1()).append("</td>")
                      .append("<td>").append(entry.getValue().component2()).append("</td>")
                      .append("<td>").append(String.format("%.1f", pct)).append("%</td>")
                      .append("</tr>");
                });

        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private String imprimirMapaDosAndTresJugadasAnteriores(Map<String, Map<String, Integer>> mapaJugadasDosAnteriores, Map<String, Map<String, Integer>> mapaJugadasTresAnteriores) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card\">")
          .append("<div class=\"card-header\">Secuencias anteriores</div>");

        String dosAnterioresKey = penultimoNumero + "-" + ultimoNumero;
        Map<String, Integer> valuesDosAnteriores = mapaJugadasDosAnteriores.get(dosAnterioresKey);
        if (valuesDosAnteriores != null && !valuesDosAnteriores.isEmpty()) {
            sb.append("<div class=\"seq-key\">Despues de ").append(dosAnterioresKey).append(":</div>");
            sb.append("<div class=\"seq-values\">");
            valuesDosAnteriores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    setNumerosAJugar.add(e.getKey());
                    sb.append("<div class=\"freq-row\">")
                      .append("<span class=\"num-highlight\">").append(e.getKey()).append("</span>")
                      .append("<span class=\"freq-label\">").append(e.getValue()).append(" veces</span>")
                      .append("</div>");
                });
            sb.append("</div>");
        }

        String tresAnterioresKey = antepenultimoNumero + "-" + penultimoNumero + "-" + ultimoNumero;
        Map<String, Integer> valuesTresAnteriores = mapaJugadasTresAnteriores.get(tresAnterioresKey);
        if (valuesTresAnteriores != null && !valuesTresAnteriores.isEmpty()) {
            setNumerosAJugar.addAll(valuesTresAnteriores.keySet());
            sb.append("<div class=\"seq-key\">Despues de ").append(tresAnterioresKey).append(":</div>");
            sb.append("<div class=\"seq-values\">");
            valuesTresAnteriores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e ->
                    sb.append("<div class=\"freq-row\">")
                      .append("<span class=\"num-highlight\">").append(e.getKey()).append("</span>")
                      .append("<span class=\"freq-label\">").append(e.getValue()).append(" veces</span>")
                      .append("</div>")
                );
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String imprimirMensajeJugadasHoy(Map<String, List<String>> mapaJugadasAnterioresDiaHoy, Map<String, AtomicInteger> mapaNumerosEstadisticasJugadasHoy) {
        StringBuilder sb = new StringBuilder();
        int contadorJugadasHoy = 0;

        sb.append("<div class=\"card\">")
          .append("<div class=\"card-header\">Jugadas del dia de hoy</div>");

        List<String> valuesSiguientesHoy = mapaJugadasAnterioresDiaHoy.get(ultimoNumero);
        if (valuesSiguientesHoy != null && !valuesSiguientesHoy.isEmpty()) {
            setNumerosAJugar.addAll(valuesSiguientesHoy);
            sb.append("<div class=\"seq-key\">El dia de Hoy despues de ").append(ultimoNumero).append(":</div>");
            sb.append("<div class=\"seq-values\">");
            valuesSiguientesHoy.forEach(num ->
                sb.append("<span class=\"num-highlight\">").append(num).append("</span> ")
            );
            sb.append("</div>");
        }

        Map<Integer, List<String>> mapaAgrupadoPorVeces = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, AtomicInteger> entry : mapaNumerosEstadisticasJugadasHoy.entrySet()) {
            String numero = entry.getKey();
            AtomicInteger veces = entry.getValue();
            contadorJugadasHoy += veces.get();
            mapaAgrupadoPorVeces.computeIfAbsent(veces.get(), ArrayList::new);
            mapaAgrupadoPorVeces.get(veces.get()).add(numero);
        }

        List<String> cerosHoy = Arrays.stream(numerosRuletaArray)
                .filter(n -> !mapaNumerosEstadisticasJugadasHoy.containsKey(n))
                .collect(Collectors.toList());
        if (!cerosHoy.isEmpty()) {
            mapaAgrupadoPorVeces.put(0, cerosHoy);
        }

        sb.append("<div class=\"seq-key\">Numeros que han caido el dia de hoy (").append(contadorJugadasHoy).append("):</div>");

        int countInsertarSetNumerosJugarHoy = 0;
        boolean insertarSetNumerosJugarHoy = true;
        for (Map.Entry<Integer, List<String>> integerListEntry : mapaAgrupadoPorVeces.entrySet()) {
            Integer vecesCayo = integerListEntry.getKey();
            List<String> listaNumeros = integerListEntry.getValue();

            if (listaNumeros.size() <= 7 && countInsertarSetNumerosJugarHoy <= 15 && insertarSetNumerosJugarHoy) {
                setNumerosAJugar.addAll(listaNumeros);
                countInsertarSetNumerosJugarHoy += listaNumeros.size();
                if (countInsertarSetNumerosJugarHoy >= 15) {
                    insertarSetNumerosJugarHoy = false;
                }
            } else {
                insertarSetNumerosJugarHoy = false;
            }

            sb.append("<div class=\"hoy-row\">")
              .append("<span class=\"freq-label\">").append(vecesCayo).append(" veces</span>")
              .append("<span class=\"num-highlight\">").append(listaNumeros).append("</span>")
              .append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private void incrementarCountColorParDocena(Estadistica estadistica) {
        if (Secciones.Color.NEGRO.equals(estadistica.getColor()))
            bolasNegras++;
        if (Secciones.Color.ROJO.equals(estadistica.getColor()))
            bolasRojas++;

        if (Secciones.Paridad.PAR.equals(estadistica.getParidad()))
            numeroPares++;
        if (Secciones.Paridad.IMPAR.equals(estadistica.getParidad()))
            numerosImpares++;

        if (Secciones.Docena.PRIMERA.equals(estadistica.getDocena())) {
            numerosPrimerDocena++;
        }
        if (Secciones.Docena.SEGUNDA.equals(estadistica.getDocena())) {
            numerosSegundaDocena++;
        }
        if (Secciones.Docena.TERCERA.equals(estadistica.getDocena())) {
            numerosTerceraDocena++;
        }
    }

    private Map<String, Estadistica> inicializarMapa() {
        Map<String, Estadistica> map;

        map = IntStream.rangeClosed(0, 36)
                .mapToObj(num -> new Estadistica(String.valueOf(num)))
                .collect(Collectors.toMap(Estadistica::getNumero, est -> est));

        map.put("00", new Estadistica("00"));

        return map;
    }

    private Estadistica calcularEstadistica(String numeroString, long cantidad,
                                            Map<String, Estadistica> mapa) {
        Estadistica estadistica = mapa.get(numeroString);

        try {
            //System.out.println("Estadisticas Numero: " + numeroString);
            double vecesCayo = estadistica.aumentarVecesCayo();

            estadistica.setPorcentaje(vecesCayo * 100 / cantidad);
        } catch (Exception e) {
            throw new RuntimeException("error al calcular numero " + numeroString, e);
        }

        return estadistica;
    }

    private String calcularMensajeCuantoJugar(LinkedHashMap<String, Estadistica> mapa, long cuantos) {
        for (int i = 1; i <= 38; i++) {
            Estadistica actual = mapa.remove(mapa.keySet().iterator().next());
            long gananciasTemporal = ganancias + (actual.getVecesCayo() * 36) - cuantos;

            if (gananciasTemporal > ganancias) {
                ganancias = gananciasTemporal;
                numerosJugar.add(actual.getNumero());
                cantidadNumerosJugar = i;
                porcentajeAcumulado += actual.getPorcentaje();
            }
        }

        return "<div class=\"callout-box\">"
             + "<div class=\"card-header\">Para maximizar las ganancias</div>"
             + "<div class=\"callout-profit\">"
             + "Se deben jugar <span class=\"profit-number\">" + cantidadNumerosJugar + "</span> numeros:"
             + "<br><span class=\"num-highlight\">" + numerosJugar + "</span>"
             + "<br>Ganancia: <span class=\"profit-number\">" + ganancias + "</span> fichas"
             + " &mdash; " + DOS_DECIMALES_FORMAT.format(porcentajeAcumulado) + "% por turno"
             + "</div></div>";
    }

    private LinkedHashMap<String, Integer> llenarMapaOrdenRuleta() {
        LinkedHashMap<String, Integer> mapa = new LinkedHashMap<>();

        for (String numero : numerosRuletaArray) {
            mapa.put(numero, 0);
        }
        return mapa;
    }

    private String imprimirMapaNumerosDespuesCuantasVeces(Map<String, Integer> numerosDespues) {
        int countInsertarSetNumerosJugarHoy = 0;
        boolean insertarSetNumerosJugarHoy = true;

        int totalObservaciones = numerosDespues.values().stream().mapToInt(Integer::intValue).sum();

        Comparator<Map.Entry<String, Integer>> comparator = Map.Entry.comparingByValue();
        LinkedHashMap<Integer, List<String>> orderMap = numerosDespues.entrySet().stream()
                .sorted(comparator.reversed())
                .collect(Collectors.toMap(Map.Entry::getValue, entry -> new ArrayList<>(Collections.singletonList(entry.getKey())), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                }, LinkedHashMap::new));

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card\">")
          .append("<div class=\"card-header\">DESP. del: ").append(ultimoNumero).append("</div>");

        double acumulado = 0;
        for (Map.Entry<Integer, List<String>> entry : orderMap.entrySet()) {
            Integer veces = entry.getKey();
            List<String> numeroList = entry.getValue();
            if (numeroList.size() <= 8 && countInsertarSetNumerosJugarHoy <= 15 && insertarSetNumerosJugarHoy) {
                setNumerosAJugar.addAll(numeroList);
                countInsertarSetNumerosJugarHoy += numeroList.size();
                if (countInsertarSetNumerosJugarHoy >= 15) {
                    insertarSetNumerosJugarHoy = false;
                }
            } else {
                insertarSetNumerosJugarHoy = false;
            }

            String pct = "0%";
            String acumuladoStr = "0%";
            if (totalObservaciones > 0) {
                pct = DOS_DECIMALES_FORMAT.format((double) veces * 100 / totalObservaciones) + "%";
                acumulado += (double) veces * numeroList.size() * 100 / totalObservaciones;
                acumuladoStr = DOS_DECIMALES_FORMAT.format(acumulado) + "%";
            }

            sb.append("<div class=\"freq-row\">")
              .append("<span class=\"freq-label\">").append(veces).append(" veces (").append(pct).append("):</span>")
              .append("<span class=\"num-highlight\">").append(numeroList).append("</span>")
              .append("<span class=\"freq-acum\">").append(acumuladoStr).append("</span>")
              .append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String imprimirMapaNumerosAnterioresDespues(Map<String, Integer> mapa) {
        StringBuilder mensaje = new StringBuilder("{").append(SALTO_LINEA);
        mensaje.append("<div style=font-size:45>");
        for (Map.Entry<String, Integer> entrada : mapa.entrySet()) {
            if (entrada.getKey().equals("0")) {
                mensaje.append(SALTO_LINEA);
            }

            mensaje.append("|");
            if (entrada.getValue() == 1) {
                mensaje.append("<font color=\"#ffe3dc\">").append(entrada.getKey()).append("</font>");
            } else if (entrada.getValue() == 2) {
                mensaje.append("<font color=\"#ffb7a6\">").append(entrada.getKey()).append("</font>");
            } else if (entrada.getValue() == 3) {
                mensaje.append("<font color=\"#ff9a83\">").append(entrada.getKey()).append("</font>");
            } else if (entrada.getValue() == 4) {
                mensaje.append("<font color=\"#ff795a\">").append(entrada.getKey()).append("</font>");
            } else if (entrada.getValue() > 4) {
                mensaje.append("<font color=\"#ff3100\">").append(entrada.getKey()).append("</font>");
            } else {
                if (entrada.getKey().length() == 2) {
                    mensaje.append("--");
                } else {
                    mensaje.append("-");
                }
            }
        }
        mensaje.append("</div>");
        mensaje.append("}");

        return mensaje.toString();
    }

    private Map<String, Pair<AtomicInteger, AtomicInteger>> inicializarMapaModulos() {
        Map<String, Pair<AtomicInteger, AtomicInteger>> mapa = new HashMap<>();
        for (int i = 0; i <= 9; i++) {
            mapa.put(String.valueOf(i), new Pair(new AtomicInteger(0), new AtomicInteger(0)));
        }
        return mapa;
    }

    private Map<String, List<String>> inicializarMapaModulosPuertas(Map<String, ?> mapaModulos) {
        Map<String, List<String>> mapa = new HashMap<>();

        for (String keyModulo : mapaModulos.keySet()) {
            List<String> listaPuertas = new ArrayList<>();

            listaPuertas.add(mapaNumerosPuertas.get(keyModulo).component1());
            listaPuertas.add(mapaNumerosPuertas.get(keyModulo).component2());
            listaPuertas.add(mapaNumerosPuertas.get("1" + keyModulo).component1());
            listaPuertas.add(mapaNumerosPuertas.get("1" + keyModulo).component2());
            listaPuertas.add(mapaNumerosPuertas.get("2" + keyModulo).component1());
            listaPuertas.add(mapaNumerosPuertas.get("2" + keyModulo).component2());
            if (Integer.parseInt(keyModulo) <= 6) {
                listaPuertas.add(mapaNumerosPuertas.get("3" + keyModulo).component1());
                listaPuertas.add(mapaNumerosPuertas.get("3" + keyModulo).component2());
            }

            mapa.put(keyModulo, listaPuertas);
        }

        return mapa;
    }

    private static Map<String, Pair<String, String>> inicializarMapaNumerosPuertas() {
        Map<String, Pair<String, String>> mapa = new HashMap<>();

        mapa.put(numerosRuletaArray[0], new Pair<>(numerosRuletaArray[numerosRuletaArray.length - 1], numerosRuletaArray[1]));
        for (int i = 1; i < numerosRuletaArray.length - 1; i++) {
            mapa.put(numerosRuletaArray[i], new Pair<>(numerosRuletaArray[i - 1], numerosRuletaArray[i + 1]));
        }
        mapa.put(numerosRuletaArray[numerosRuletaArray.length - 1], new Pair<>(numerosRuletaArray[numerosRuletaArray.length - 2], numerosRuletaArray[0]));

        return mapa;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prediction confidence leaderboard
    // ─────────────────────────────────────────────────────────────────────────

    private void calcularPuntuacionConfianza() {
        // 1. After-last-number top-5 → 4 pts
        transiciones.getOrDefault(ultimoNumero, Collections.emptyMap()).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> puntuacionConfianza.merge(e.getKey(), 4, Integer::sum));

        // 2. 2-sequence pattern → 5 pts
        Map<String, Integer> dosResult = mapaJugadasDosAnteriores.get(penultimoNumero + "-" + ultimoNumero);
        if (dosResult != null) {
            dosResult.keySet().forEach(num -> puntuacionConfianza.merge(num, 5, Integer::sum));
        }

        // 3. 3-sequence pattern → 6 pts
        Map<String, Integer> tresResult = mapaJugadasTresAnteriores.get(
                antepenultimoNumero + "-" + penultimoNumero + "-" + ultimoNumero);
        if (tresResult != null) {
            tresResult.keySet().forEach(num -> puntuacionConfianza.merge(num, 6, Integer::sum));
        }

        // 4. Today's session → 5 pts
        List<String> hoyResult = mapaJugadasAnterioresDiaHoy.get(ultimoNumero);
        if (hoyResult != null) {
            hoyResult.forEach(num -> puntuacionConfianza.merge(num, 5, Integer::sum));
        }

        // 5. Profit-optimal set → 4 pts
        numerosJugar.forEach(num -> puntuacionConfianza.merge(num, 4, Integer::sum));
    }

    private String imprimirPuntuacionConfianza() {
        if (puntuacionConfianza.isEmpty()) {
            return "";
        }

        // Pre-compute signal sets for badge rendering
        Set<String> top5Despues = transiciones.getOrDefault(ultimoNumero, Collections.emptyMap())
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Integer> dosResult = mapaJugadasDosAnteriores.get(penultimoNumero + "-" + ultimoNumero);
        Map<String, Integer> tresResult = mapaJugadasTresAnteriores.get(
                antepenultimoNumero + "-" + penultimoNumero + "-" + ultimoNumero);
        List<String> hoyResult = mapaJugadasAnterioresDiaHoy.get(ultimoNumero);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"callout-gold\">")
          .append("<div class=\"card-header\" style=\"color:#b8860b;border-bottom-color:#ffe9a0\">Marcador de Confianza</div>")
          .append("<table class=\"stats-table\"><thead><tr>")
          .append("<th>#</th><th>No</th><th style=\"text-align:right\">Pts</th><th>Senales</th>")
          .append("</tr></thead><tbody>");

        AtomicInteger rank = new AtomicInteger(1);
        puntuacionConfianza.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .forEach(entry -> {
                    String num = entry.getKey();
                    int pts = entry.getValue();

                    List<String> senales = new ArrayList<>();
                    if (top5Despues.contains(num)) senales.add("DESP");
                    if (dosResult != null && dosResult.containsKey(num)) senales.add("2-SEC");
                    if (tresResult != null && tresResult.containsKey(num)) senales.add("3-SEC");
                    if (hoyResult != null && hoyResult.contains(num)) senales.add("HOY");
                    if (numerosJugar.contains(num)) senales.add("OPTIMO");

                    StringBuilder badges = new StringBuilder();
                    senales.forEach(s ->
                        badges.append("<span class=\"signal-badge\">").append(s).append("</span>"));

                    sb.append("<tr>")
                      .append("<td class=\"freq-label\">").append(rank.getAndIncrement()).append("</td>")
                      .append("<td class=\"num-cell\">").append(num).append("</td>")
                      .append("<td style=\"text-align:right\"><span class=\"profit-number\" style=\"font-size:18px\">")
                      .append(pts).append("</span></td>")
                      .append("<td>").append(badges).append("</td>")
                      .append("</tr>");
                });

        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Snapshot creation
    // ─────────────────────────────────────────────────────────────────────────

    private RuletaSnapshot crearSnapshotInterno(Map<String, Estadistica> mapaEstadisticaPorNumero,
                                                long historicalSpinCount) {
        Map<String, Integer> vecesCayoHistorico = new HashMap<>();
        mapaEstadisticaPorNumero.forEach((num, e) -> {
            if (e.getVecesCayo() > 0) vecesCayoHistorico.put(num, e.getVecesCayo());
        });

        Map<String, Map<String, Integer>> transCopy = new HashMap<>();
        transiciones.forEach((k, v) -> transCopy.put(k, new HashMap<>(v)));

        Map<String, Map<String, Integer>> dosCopy = new HashMap<>();
        mapaJugadasDosAnteriores.forEach((k, v) -> dosCopy.put(k, new HashMap<>(v)));

        Map<String, Map<String, Integer>> tresCopy = new HashMap<>();
        mapaJugadasTresAnteriores.forEach((k, v) -> tresCopy.put(k, new HashMap<>(v)));

        Map<String, int[]> moduloSin = new HashMap<>();
        mapaEstadisticasModulosSinPuertas.forEach((k, v) ->
                moduloSin.put(k, new int[]{v.component1().get(), v.component2().get()}));

        Map<String, int[]> moduloCon = new HashMap<>();
        mapaEstadisticasModulosConPuertas.forEach((k, v) ->
                moduloCon.put(k, new int[]{v.component1().get(), v.component2().get()}));

        return new RuletaSnapshot(
                vecesCayoHistorico, historicalSpinCount,
                bolasRojas, bolasNegras,
                numeroPares, numerosImpares,
                numerosPrimerDocena, numerosSegundaDocena, numerosTerceraDocena,
                transCopy, dosCopy, tresCopy,
                moduloSin, moduloCon
        );
    }
}
