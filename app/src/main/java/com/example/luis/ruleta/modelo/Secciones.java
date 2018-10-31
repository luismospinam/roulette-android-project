package com.example.luis.ruleta.modelo;

import java.util.Arrays;
import java.util.List;

public class Secciones {
    public static final List<String> NUMEROS_ROJOS = Arrays.asList("1", "3", "5", "7", "9", "12", "14", "16", "18", "19", "21", "23", "25", "27", "30", "32", "34", "36");
    public static final List<String> NUMEROS_NEGROS = Arrays.asList("2", "4", "6", "8", "10", "11", "13", "15", "17", "20", "22", "24", "26", "28", "29", "31", "33", "35");

    public static final List<String> PRIMER_DOCENA = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
    public static final List<String> SEGUNDA_DOCENA = Arrays.asList("13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24");
    public static final List<String> TERCER_DOCENA = Arrays.asList("25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36");

    public enum Color {
        ROJO,
        NEGRO;

        public static Color calcularColor(String numero) {
            if (NUMEROS_NEGROS.contains(numero)) {
                return NEGRO;
            } else if (NUMEROS_ROJOS.contains(numero)) {
                return ROJO;
            }

            return null;
        }
    }

    public enum Paridad {
        PAR,
        IMPAR;

        public static Paridad calcularParidad(String numero) {
            if (Integer.valueOf(numero) % 2 == 0 && !"0".equals(numero) && !"00".equals(numero)) {
                return PAR;
            } else if (Integer.valueOf(numero) % 2 != 0 && !"0".equals(numero) && !"00".equals(numero)) {
                return IMPAR;
            }

            return null;
        }
    }

    public enum Docena {
        PRIMERA,
        SEGUNDA,
        TERCERA;

        public static Docena calcularDocena(String numero) {
            if (PRIMER_DOCENA.contains(numero)) {
                return PRIMERA;
            } else if (SEGUNDA_DOCENA.contains(numero)) {
                return SEGUNDA;
            } else if (TERCER_DOCENA.contains(numero)) {
                return TERCERA;
            }

            return null;
        }
    }
}


