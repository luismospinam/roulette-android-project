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
    private final Map<String, Integer> numerosAnteriores = llenarMapaOrdenRuleta();
    private final Map<String, Integer> numerosDespues = llenarMapaOrdenRuleta();

    private final Map<String, Map<String, Integer>> mapaJugadasDosAnteriores = new HashMap<>();
    private final Map<String, List<String>> mapaJugadasTresAnteriores = new HashMap<>();
    private final Map<String, List<String>> mapaJugadasAnterioresDiaHoy = new HashMap<>();
    private final Map<String, AtomicInteger> mapaNumerosEstadisticasJugadasHoy = new HashMap<>();
    private final Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosConPuertas = inicializarMapaModulos();
    private final Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosSinPuertas = inicializarMapaModulos();
    private final Map<String, List<String>> mapaNumeroModulosPuertas = inicializarMapaModulosPuertas(mapaEstadisticasModulosConPuertas);

    private final Set<String> setNumerosAJugar = new LinkedHashSet<>();


    public String calcularResultado(String jugada) {
        String mensaje = "";
        Map<String, Estadistica> mapaEstadisticaPorNumero = inicializarMapa();

        List<String> diasJugadas = Arrays.asList(jugada.split(MainActivity.SEPARADOR_DIA));
        long cantidadTotalJugadas = diasJugadas.stream()
                .flatMap(dia -> Stream.of(dia.split(SEPARADOR_JUGADAS)))
                .count();
        String[] jugadaUltimoDia = diasJugadas.get(diasJugadas.size() - 1).split(SEPARADOR_JUGADAS);
        ultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 1];
        penultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 2];
        antepenultimoNumero = jugadaUltimoDia[jugadaUltimoDia.length - 3];

        for (int j = 0; j < diasJugadas.size(); j++) {
            String diaActual = diasJugadas.get(j);
            List<String> numeros = Arrays.asList(diaActual.split(SEPARADOR_JUGADAS));

            for (int i = 0; i < numeros.size(); i++) {
                String numeroActual = numeros.get(i);
                Estadistica estadistica = calcularEstadistica(numeroActual, cantidadTotalJugadas, mapaEstadisticaPorNumero);
                mapaEstadisticaPorNumero.put(numeroActual, estadistica);

                incrementarCountColorParDocena(estadistica);

                if (numeroActual.equals(ultimoNumero) && i > 0)
                    numerosAnteriores.merge(numeros.get(i - 1), 1, Integer::sum);
                if (numeroActual.equals(ultimoNumero) && i < numeros.size() - 1)
                    numerosDespues.merge(numeros.get(i + 1), 1, Integer::sum);

                calcularDosAndTresJugadasAnterioresMapa(numeroActual, i, numeros);
                calcularJugadasModuloDiez(numeroActual, i, numeros);

                if (j == (diasJugadas.size() - 1) && i <= numeros.size() - 1) {
                    calcularJugadasAnterioresDiaDeHoyMapa(numeroActual, i, numeros);
                    incrementarNumeroJugadasHoy(mapaNumerosEstadisticasJugadasHoy, numeroActual);
                }
            }
        }

        LinkedHashMap<String, Estadistica> orderMap = getMapaOrderVecesCayo(mapaEstadisticaPorNumero);
        mensaje = generarMensajeEstadisticas(cantidadTotalJugadas, orderMap);
        mensaje += calcularMensajeCuantoJugar(orderMap, cantidadTotalJugadas);

        return mensaje;
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
        if (numeroActual.equals(ultimoNumero) && i > 1 && i < numeros.size() - 1) {
            String anteriorUltimo = numeros.get(i - 1);
            String numeroSiguio = numeros.get(i + 1);
            Map<String, Integer> mapaDosAnteriores = mapaJugadasDosAnteriores.computeIfAbsent(anteriorUltimo + "-" + ultimoNumero,
                    k -> new HashMap<>());
            if (!mapaDosAnteriores.containsKey(numeroSiguio)) {
                mapaDosAnteriores.put(numeroSiguio, 1);
            } else {
                Integer veces = mapaDosAnteriores.get(numeroSiguio);
                mapaDosAnteriores.put(numeroSiguio, veces + 1);
            }

            if (i > 2) {
                String anteriorAnteriorUltimo = numeros.get(i - 2);
                List<String> listTresAnteriores = mapaJugadasTresAnteriores.computeIfAbsent(anteriorAnteriorUltimo + "-" + anteriorUltimo + "-" + ultimoNumero,
                        k -> new ArrayList<>());
                listTresAnteriores.add(numeroSiguio);
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
        String mensaje = "";
        mensaje += "Numero total de bolas: " + cantidadTotalJugadas + SALTO_LINEA + SALTO_LINEA;

        //mensaje += SALTO_LINEA + "DESP. del: " + ultimoNumero + ": " + imprimirMapaNumerosAnterioresDespues(numerosDespues);
        mensaje += SALTO_LINEA + "DESP. del: " + ultimoNumero + ": " + imprimirMapaNumerosDespuesCuantasVeces(numerosDespues);
        mensaje += SALTO_LINEA + imprimirMapaDosAndTresJugadasAnteriores(mapaJugadasDosAnteriores, mapaJugadasTresAnteriores) + SALTO_LINEA;
        mensaje += SALTO_LINEA + imprimirMensajeJugadasHoy(mapaJugadasAnterioresDiaHoy, mapaNumerosEstadisticasJugadasHoy) + SALTO_LINEA + SALTO_LINEA;

        mensaje += "<div style=\"font-size: 30px\">";
        mensaje += "Resumen de numeros a jugar: " + SALTO_LINEA;
        mensaje += "<font color=#ff3100>" + setNumerosAJugar + "</font>" + SALTO_LINEA + SALTO_LINEA + SALTO_LINEA;
        mensaje += "</div>";

        StringBuilder resultadoEstadistica = new StringBuilder();
        orderMap.forEach((k, v) -> resultadoEstadistica.append(v).append(SALTO_LINEA));
        mensaje += resultadoEstadistica.toString();

        mensaje += SALTO_LINEA;
        mensaje += SALTO_LINEA + "ROJO: " + bolasRojas + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) bolasRojas * 100 / cantidadTotalJugadas) + "%";
        mensaje += SALTO_LINEA + "NEGRO: " + bolasNegras + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) bolasNegras * 100 / cantidadTotalJugadas) + "%";
        mensaje += SALTO_LINEA;

        mensaje += SALTO_LINEA + "IMPARES: " + numerosImpares + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosImpares * 100 / cantidadTotalJugadas) + "%";
        mensaje += SALTO_LINEA + "PARES: " + numeroPares + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numeroPares * 100 / cantidadTotalJugadas) + "%";
        mensaje += SALTO_LINEA;

        mensaje += SALTO_LINEA + "PRIMER DOCENA: " + numerosPrimerDocena + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosPrimerDocena * 100 / cantidadTotalJugadas) + "%";
        mensaje += SALTO_LINEA + "SEGUNDA DOCENA: " + numerosSegundaDocena + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosSegundaDocena * 100 / cantidadTotalJugadas) + "%";
        mensaje += SALTO_LINEA + "TERCER DOCENA: " + numerosTerceraDocena + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosTerceraDocena * 100 / cantidadTotalJugadas) + "%";

        mensaje += SALTO_LINEA + SALTO_LINEA + imprimirMensajeModulo(mapaEstadisticasModulosSinPuertas, mapaEstadisticasModulosConPuertas) + SALTO_LINEA;


        return mensaje;
    }

    private String imprimirMensajeModulo(Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosSinPuertas, Map<String, Pair<AtomicInteger, AtomicInteger>> mapaEstadisticasModulosConPuertas) {
        StringBuilder mensaje = new StringBuilder();

        Comparator<Map.Entry<String, Pair<Integer, Integer>>> comparator = Comparator.comparing(a -> a.getValue().component1());
        mensaje.append("Modulo 10 SIN contar puertas:");
        mapaEstadisticasModulosSinPuertas.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), new Pair<Integer, Integer>(entry.getValue().component1().get(), entry.getValue().component2().get())))
                .sorted(comparator.reversed())
                .forEach(entry ->
                        mensaje.append(SALTO_LINEA)
                                .append("Modulo de ")
                                .append(entry.getKey())
                                .append(" cayó veces: ")
                                .append(entry.getValue().component1())
                                .append(" de ")
                                .append(entry.getValue().component2())
                                .append(" posibles jugadas, ")
                                .append((float) entry.getValue().component1() * 100 / entry.getValue().component2())
                                .append("%")
                );


        mensaje.append(SALTO_LINEA).append(SALTO_LINEA);
        mensaje.append("Modulo 10 CON contar puertas:");
        mapaEstadisticasModulosConPuertas.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), new Pair<Integer, Integer>(entry.getValue().component1().get(), entry.getValue().component2().get())))
                .sorted(comparator.reversed())
                .forEach(entry ->
                        mensaje.append(SALTO_LINEA)
                                .append("Modulo de ")
                                .append(entry.getKey())
                                .append(" cayó veces: ")
                                .append(entry.getValue().component1())
                                .append(" de ")
                                .append(entry.getValue().component2())
                                .append(" posibles jugadas, ")
                                .append((float) entry.getValue().component1() * 100 / entry.getValue().component2())
                                .append("%")
                );

        return mensaje.toString();
    }

    private String imprimirMapaDosAndTresJugadasAnteriores(Map<String, Map<String, Integer>> mapaJugadasDosAnteriores, Map<String, List<String>> mapaJugadasTresAnteriores) {
        StringBuilder mensaje = new StringBuilder();

        String dosAnterioresKey = penultimoNumero + "-" + ultimoNumero;
        Map<String, Integer> valuesDosAnteriores = mapaJugadasDosAnteriores.get(dosAnterioresKey);
        if (valuesDosAnteriores != null && !valuesDosAnteriores.isEmpty()) {
            mensaje.append(SALTO_LINEA)
                    .append("Despues de ")
                    .append(dosAnterioresKey)
                    .append(": ");
            valuesDosAnteriores.forEach((numero, veces) -> {
                setNumerosAJugar.add(numero);

                mensaje.append(SALTO_LINEA)
                        .append(TABULADOR)
                        .append("<font color=\"#ff3100\">")
                        .append(numero)
                        .append("</font>")
                        .append(" veces: ")
                        .append(veces);
            });
        }
        mensaje.append(SALTO_LINEA);

        String tresAnterioresKey = antepenultimoNumero + "-" + penultimoNumero + "-" + ultimoNumero;
        List<String> valuesTresAnteriores = mapaJugadasTresAnteriores.get(tresAnterioresKey);
        if (valuesTresAnteriores != null && !valuesTresAnteriores.isEmpty()) {
            setNumerosAJugar.addAll(valuesTresAnteriores);

            mensaje.append(SALTO_LINEA)
                    .append("Despues de ")
                    .append(tresAnterioresKey)
                    .append(": ")
                    .append("<font color=\"#ff3100\">")
                    .append(valuesTresAnteriores)
                    .append("</font>");
        }

        return mensaje.toString();
    }

    private String imprimirMensajeJugadasHoy(Map<String, List<String>> mapaJugadasAnterioresDiaHoy, Map<String, AtomicInteger> mapaNumerosEstadisticasJugadasHoy) {
        StringBuilder mensaje = new StringBuilder();
        int contadorJugadasHoy = 0;

        List<String> valuesSiguientesHoy = mapaJugadasAnterioresDiaHoy.get(ultimoNumero);
        if (valuesSiguientesHoy != null && !valuesSiguientesHoy.isEmpty()) {
            setNumerosAJugar.addAll(valuesSiguientesHoy);

            mensaje.append(SALTO_LINEA)
                    .append("El dia de Hoy despues de ")
                    .append(ultimoNumero)
                    .append(": ")
                    .append("<font color=\"#ff3100\">")
                    .append(valuesSiguientesHoy)
                    .append("</font>");
        }

        mensaje.append(SALTO_LINEA);
        Map<Integer, List<String>> mapaAgrupadoPorVeces = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, AtomicInteger> stringAtomicIntegerEntry : mapaNumerosEstadisticasJugadasHoy.entrySet()) {
            String numero = stringAtomicIntegerEntry.getKey();
            AtomicInteger veces = stringAtomicIntegerEntry.getValue();
            contadorJugadasHoy += veces.get();

            mapaAgrupadoPorVeces.computeIfAbsent(veces.get(), ArrayList::new);
            mapaAgrupadoPorVeces.get(veces.get()).add(numero);
        }

        int countInsertarSetNumerosJugarHoy = 0;
        boolean insertarSetNumerosJugarHoy = true;
        mensaje.append(SALTO_LINEA).append("Numeros que han caido el dia de hoy (" + contadorJugadasHoy + "):")
                .append(SALTO_LINEA);
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

            mensaje.append(vecesCayo)
                    .append(" veces: ")
                    .append("<font color=\"#ff3100\">")
                    .append(listaNumeros)
                    .append(SALTO_LINEA)
                    .append("</font>");
        }

        return mensaje.toString();
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
            long gananciasTemporal = 0;
            gananciasTemporal = ganancias + (actual.getVecesCayo() * 36) - (cuantos);

            if (gananciasTemporal > ganancias) {
                ganancias = gananciasTemporal;
                numerosJugar.add(actual.getNumero());
                cantidadNumerosJugar = i;
                porcentajeAcumulado += actual.getPorcentaje();
            }
        }

        return SALTO_LINEA + SALTO_LINEA + "Para maximizar las ganacias se deben jugar: " + cantidadNumerosJugar
                + " numeros " +
                " los cuales son: " + SALTO_LINEA + numerosJugar + SALTO_LINEA +
                "con una ganancia de " + ganancias + " fichas en total " +
                "con un porcentaje por turno de " + DOS_DECIMALES_FORMAT.format(porcentajeAcumulado) + "%";
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
        StringBuilder mensaje = new StringBuilder("{").append(SALTO_LINEA);

        Comparator<Map.Entry<String, Integer>> comparator = Map.Entry.comparingByValue();
        LinkedHashMap<Integer, List<String>> orderMap = numerosDespues.entrySet().stream()
                .sorted(comparator.reversed())
                .collect(Collectors.toMap(Map.Entry::getValue, entry -> new ArrayList<>(Collections.singletonList(entry.getKey())), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                }, LinkedHashMap::new));

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

            mensaje.append(TABULADOR)
                    .append("Cayeron ")
                    .append(veces)
                    .append(" Veces los numeros: ")
                    .append("<font color=\"#ff3100\">")
                    .append(numeroList)
                    .append("</font>")
                    .append(SALTO_LINEA);
        }
        mensaje.append("}");

        return mensaje.toString();
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
}
