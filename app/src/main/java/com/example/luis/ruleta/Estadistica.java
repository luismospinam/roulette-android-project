package com.example.luis.ruleta;

import java.text.DecimalFormat;

public class Estadistica {

    private String numero;
    private int vecesCayo;
    private double porcentaje;

    public Estadistica(String numero) {
        this.numero = numero;
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


    public int aumentarVecesCayo() {
        this.vecesCayo += 1;
        return vecesCayo;
    }

    public String toString() {
        DecimalFormat df2 = new DecimalFormat(".##");

        StringBuilder builder = new StringBuilder();
        builder.append("Numero: ").append(String.format("%1$2s", numero)).append(" - ")
                .append("Cayó: ").append(String.format("%1$2s", vecesCayo)).append(" veces - ")
                .append("Porcentaje: ").append(df2.format(porcentaje)).append("%");

        return builder.toString();
    }


}
