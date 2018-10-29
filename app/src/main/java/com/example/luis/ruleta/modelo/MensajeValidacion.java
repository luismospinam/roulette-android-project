package com.example.luis.ruleta.modelo;

public class MensajeValidacion {
    private boolean isValido;
    private String mensaje;

    public boolean isValido() {
        return isValido;
    }

    public void setValido(boolean valido) {
        isValido = valido;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

}
