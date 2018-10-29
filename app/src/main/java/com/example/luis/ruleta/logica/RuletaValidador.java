package com.example.luis.ruleta.logica;

import com.example.luis.ruleta.modelo.MensajeValidacion;

public class RuletaValidador {

    public static MensajeValidacion validarStringJugadas(String jugadas) {
        String[] jugadasArray = jugadas.split(",");
        MensajeValidacion respuesta = new MensajeValidacion();
        respuesta.setValido(true);

        int posicion = 1;
        for (String jugada : jugadasArray) {
            if (!Ruleta.NUMEROS_VALIDOS_ORDENADOS.contains(jugada)) {
                respuesta.setValido(false);
                respuesta.setMensaje("Numero invalido: " + jugada + " en la posicion: " + posicion + ", por favor corrijalo.");
                break;
            }
            posicion++;
        }

        return respuesta;
    }
}
