package com.example.luis.ruleta.logica;

import java.util.Arrays;
import java.util.List;

public class GananciasCorrida {

    public static int calcularGanancias(List<String> numerosJugados, String numerosCayo){
        String jugadas = numerosCayo;
        List<String> jugada = Arrays.asList(jugadas.split(","));

        int ganancia = 0;
        for(String numeroCayo : jugada){
            if(numerosJugados.contains(numeroCayo)){
                ganancia+= (36 - numerosJugados.size());
            }else{
                ganancia-= numerosJugados.size();
            }
        }

        return ganancia;
    }
}
