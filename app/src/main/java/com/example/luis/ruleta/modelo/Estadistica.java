package com.example.luis.ruleta.modelo;

import com.example.luis.ruleta.utilitario.Constantes;

public class Estadistica {

    private String numero;
    private Secciones.Color color;
    private Secciones.Paridad paridad;
    private Secciones.Docena docena;
    private int vecesCayo;
    private double porcentaje;

    public Estadistica(String numero) {
        this.numero = numero;
        this.color = Secciones.Color.calcularColor(numero);
        this.paridad = Secciones.Paridad.calcularParidad(numero);
        this.docena = Secciones.Docena.calcularDocena(numero);
    }

    public String getNumero(){
        return this.numero;
    }

    public void setNumero(String numero){
        this.numero = numero;
    }

    public int getVecesCayo(){
        return this.vecesCayo;
    }

    public void setVecesCayo(int VecesCayo){
        this.vecesCayo = VecesCayo;
    }

    public double getPorcentaje(){
        return this.porcentaje;
    }

    public void setPorcentaje(double porcentaje){
        this.porcentaje = porcentaje;
    }

    public Secciones.Color getColor() {
        return color;
    }

    public Secciones.Paridad getParidad() {
        return paridad;
    }

    public Secciones.Docena getDocena() {
        return docena;
    }

    public int aumentarVecesCayo() {
        this.vecesCayo += 1;
        return vecesCayo;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("No: ").append(String.format("%1$2s", numero)).append(" - ")
                .append("Cayó: ").append(String.format("%1$2s", vecesCayo)).append(" veces - ")
                .append("Porcentaje: ").append(Constantes.DOS_DECIMALES_FORMAT.format(porcentaje)).append("%");

        return builder.toString();
    }


}
