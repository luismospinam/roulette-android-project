package com.example.http_server_andoid.logic;

import com.example.http_server_andoid.MainActivity;
import com.example.http_server_andoid.model.MensajeValidacion;

public class RuletaValidador {

    public static MensajeValidacion validarStringJugadas(String jugadas) {
        MensajeValidacion respuesta = new MensajeValidacion();
        respuesta.setValido(true);

        int diaCount = 0;
        String[] diasArray = jugadas.split(MainActivity.SEPARADOR_DIA);
        String jugadaUltimoDia =  diasArray[diasArray.length - 1];
        diaCount++;
        String[] jugadasArray = jugadaUltimoDia.split(MainActivity.SEPARADOR_JUGADAS);

        int posicion = 1;
        for (String jugada : jugadasArray) {
            if (!Ruleta.NUMEROS_VALIDOS_ORDENADOS.contains(jugada)) {
                respuesta.setValido(false);
                respuesta.setMensaje("Numero invalido: " + jugada + "en el dia " + diaCount + " en la posicion: " + posicion + ", por favor corrijalo.");
                break;
            }
            posicion++;
        }


        return respuesta;
    }
}
