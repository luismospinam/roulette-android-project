package com.example.luis.ruleta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Ruleta {

    public static DecimalFormat df2 = new DecimalFormat(".##");
    private int ganancias;
    private int cantidadNumerosJugar;
    private List<String> numerosJugar = new ArrayList<>();
    private double porcentajeAcumulado = 0;
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
            if (numeroActual.equals(ultimoNumero) && i > 0)
                numerosAnteriores.merge(numeros.get(i - 1), 1, Integer::sum);
            if (numeroActual.equals(ultimoNumero) && i < numeros.size() - 1)
                numerosDespues.merge(numeros.get(i + 1), 1, Integer::sum);
        }

        mensaje += "Numero total de bolas: " + cantidad + "\n";
        LinkedHashMap orderMap = mapa.entrySet().stream()
                .map(entry -> entry.getValue())
                .sorted(Comparator.comparingInt(Estadistica::getVecesCayo).reversed())
                .collect(Collectors.toMap(Estadistica::getNumero, m -> m,
                        (oldValue, newValue) -> {
                            throw new RuntimeException("Duplicates");
                        }, LinkedHashMap::new));

        StringBuilder resultadoEstadistica = new StringBuilder();
        orderMap.forEach((k, v) -> resultadoEstadistica.append(v).append("\n"));
        mensaje += resultadoEstadistica.toString();

        //System.out.println("ANTES del: " + ultimoNumero + " son: " + numerosAnteriores);
        mensaje += "DESP. del: " + ultimoNumero + ": " + imprimirMapaNumerosAnterioresDespues(numerosDespues);

        calcularCuantoJugar(orderMap, cantidad);

        mensaje += new StringBuilder()
                .append("\n\nPara maximizar las ganacias se deben jugar: " + cantidadNumerosJugar
                        + " numeros ")
                .append(" los cuales son: \n" + numerosJugar + "\n")
                .append("con una ganancia de " + ganancias + " fichas en total ")
                .append("con un porcentaje por turno de " + df2.format(porcentajeAcumulado) + "%")
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
        String orden = "00,1,13,36,24,3,15,34,22,5,17,32,20,7,11,30,26,9,28,0,2,14,35,23,4,16,33,21,6,18,31,19,8,12,29,25,10,27";
        for (String numero : orden.split(",")) {
            mapa.put(numero, 0);
        }
        return mapa;
    }

    private String imprimirMapaNumerosAnterioresDespues(Map<String, Integer> mapa) {
        String mensaje = "{";
        for (Map.Entry<String, Integer> entrada : mapa.entrySet()) {
            if (entrada.getValue() > 0) {
                mensaje += entrada.getKey() + "=" + entrada.getValue() + " ";
            } else {
                mensaje += entrada.getKey() + "X ";
            }
        }
        mensaje += "}";

        return mensaje;
    }


}
