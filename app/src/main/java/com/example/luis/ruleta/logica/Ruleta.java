package com.example.luis.ruleta.logica;

import com.example.luis.ruleta.modelo.Estadistica;
import com.example.luis.ruleta.modelo.Secciones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.luis.ruleta.utilitario.Constantes.DOS_DECIMALES_FORMAT;
import static com.example.luis.ruleta.utilitario.Constantes.SALTO_LINEA;

public class Ruleta {
    public static String NUMEROS_VALIDOS_ORDENADOS = "00,1,13,36,24,3,15,34,22,5,17,32,20,7,11,30,26,9,28,0,2,14,35,23,4,16,33,21,6,18,31,19,8,12,29,25,10,27";
    public static String NUMEROS_ROJOS = "1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36";
    public static String NUMEROS_NEGROS = "2,4,6,8,10,11,13,15,17,20,22,24,26,28,29,31,33,35";

    private int ganancias;
    private int cantidadNumerosJugar;
    private List<String> numerosJugar = new ArrayList<>();
    private double porcentajeAcumulado = 0;

    private int bolasRojas;
    private int bolasNegras;

    private int numeroPares;
    private int numerosImpares;

    private int numerosPrimerDocena;
    private int numerosSegundaDocena;
    private int numerosTerceraDocena;

    private String ultimoNumero;
    private Map<String, Integer> numerosAnteriores = llenarMapaOrdenRuleta();
    private Map<String, Integer> numerosDespues = llenarMapaOrdenRuleta();

    public String calcularResultado(String numerosComaSeparados) {
        String mensaje = "";
        String jugada = numerosComaSeparados;
        List<String> numeros = Arrays.asList(jugada.split(","));
        int cantidad = numeros.size();
        ultimoNumero = numeros.get(numeros.size() - 1);

        Map<String, Estadistica> mapa = inicializarMapa();
        for (int i = 0; i < numeros.size(); i++) {
            String numeroActual = numeros.get(i);
            Estadistica estadistica = calcularEstadistica(numeroActual, cantidad, mapa);
            mapa.put(numeroActual, estadistica);

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

            if (numeroActual.equals(ultimoNumero) && i > 0)
                numerosAnteriores.merge(numeros.get(i - 1), 1, Integer::sum);
            if (numeroActual.equals(ultimoNumero) && i < numeros.size() - 1)
                numerosDespues.merge(numeros.get(i + 1), 1, Integer::sum);
        }

        mensaje += "Numero total de bolas: " + cantidad + SALTO_LINEA;
        LinkedHashMap orderMap = mapa.entrySet().stream()
                .map(entry -> entry.getValue())
                .sorted(Comparator.comparingInt(Estadistica::getVecesCayo).reversed())
                .collect(Collectors.toMap(Estadistica::getNumero, m -> m,
                        (oldValue, newValue) -> {
                            throw new RuntimeException("Duplicates");
                        }, LinkedHashMap::new));

        StringBuilder resultadoEstadistica = new StringBuilder();
        orderMap.forEach((k, v) -> resultadoEstadistica.append(v).append(SALTO_LINEA));
        mensaje += resultadoEstadistica.toString();

        mensaje += SALTO_LINEA + "DESP. del: " + ultimoNumero + ": " + imprimirMapaNumerosAnterioresDespues(numerosDespues);

        mensaje += SALTO_LINEA;
        mensaje += SALTO_LINEA + "ROJO: " + bolasRojas + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) bolasRojas * 100 / cantidad) + "%";
        mensaje += SALTO_LINEA + "NEGRO: " + bolasNegras + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) bolasNegras * 100 / cantidad) + "%";
        mensaje += SALTO_LINEA;

        mensaje += SALTO_LINEA + "IMPARES: " + numerosImpares + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosImpares * 100 / cantidad) + "%";
        mensaje += SALTO_LINEA + "PARES: " + numeroPares + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numeroPares * 100 / cantidad) + "%";
        mensaje += SALTO_LINEA;

        mensaje += SALTO_LINEA + "PRIMER DOCENA: " + numerosPrimerDocena + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosPrimerDocena * 100 / cantidad) + "%";
        mensaje += SALTO_LINEA + "SEGUNDA DOCENA: " + numerosSegundaDocena + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosSegundaDocena * 100 / cantidad) + "%";
        mensaje += SALTO_LINEA + "TERCER DOCENA: " + numerosTerceraDocena + " porcentaje: " + DOS_DECIMALES_FORMAT.format((double) numerosTerceraDocena * 100 / cantidad) + "%";


        calcularCuantoJugar(orderMap, cantidad);

        mensaje += new StringBuilder()
                .append(SALTO_LINEA + SALTO_LINEA + "Para maximizar las ganacias se deben jugar: " + cantidadNumerosJugar
                        + " numeros ")
                .append(" los cuales son: " + SALTO_LINEA + numerosJugar + SALTO_LINEA)
                .append("con una ganancia de " + ganancias + " fichas en total ")
                .append("con un porcentaje por turno de " + DOS_DECIMALES_FORMAT.format(porcentajeAcumulado) + "%")
                .toString();

        return mensaje;
    }

    private Estadistica calcularEstadistica(String numeroString, int cantidad,
                                            Map<String, Estadistica> mapa) {
        Estadistica estadistica = mapa.get(numeroString);
        double vecesCayo = estadistica.aumentarVecesCayo();

        estadistica.setPorcentaje(vecesCayo * 100 / cantidad);

        return estadistica;
    }

    private Map<String, Estadistica> inicializarMapa() {
        Map<String, Estadistica> map;

        map = IntStream.rangeClosed(0, 36)
                .mapToObj(num -> new Estadistica(String.valueOf(num)))
                .collect(Collectors.toMap(Estadistica::getNumero, est -> est));

        map.put("00", new Estadistica("00"));

        return map;
    }

    private void calcularCuantoJugar(LinkedHashMap<String, Estadistica> mapa, int cuantos) {
        for (int i = 1; i <= 38; i++) {
            Estadistica actual = mapa.remove(mapa.keySet().iterator().next());
            int gananciasTemporal = 0;
            gananciasTemporal = ganancias + (actual.getVecesCayo() * 36) - (cuantos);

            if (gananciasTemporal > ganancias) {
                ganancias = gananciasTemporal;
                numerosJugar.add(actual.getNumero());
                cantidadNumerosJugar = i;
                porcentajeAcumulado += actual.getPorcentaje();
            }
        }
    }

    private LinkedHashMap<String, Integer> llenarMapaOrdenRuleta() {
        LinkedHashMap<String, Integer> mapa = new LinkedHashMap<>();

        for (String numero : NUMEROS_VALIDOS_ORDENADOS.split(",")) {
            mapa.put(numero, 0);
        }
        return mapa;
    }

    private String imprimirMapaNumerosAnterioresDespues(Map<String, Integer> mapa) {
        String mensaje = "{";
        for (Map.Entry<String, Integer> entrada : mapa.entrySet()) {
            if (entrada.getValue() == 1) {
                mensaje += "<font color=\"#d2d2d2\">" + entrada.getKey() + "</font>";
            } else if (entrada.getValue() == 2) {
                mensaje += "<font color=\"#b1afaf\">" + entrada.getKey() + "</font>";
            } else if (entrada.getValue() == 3) {
                mensaje += "<font color=\"#8a8989\">" + entrada.getKey() + "</font>";
            } else if (entrada.getValue() == 4) {
                mensaje += "<font color=\"#676767\">" + entrada.getKey() + "</font>";
            } else if (entrada.getValue() > 4) {
                mensaje += "<font color=\"#000000\">" + entrada.getKey() + "</font>";
            } else {
                mensaje += "-";
            }
        }
        mensaje += "}";

        return mensaje;
    }


}
