package com.example.http_server_andoid.logic;

import java.util.Map;

/**
 * Immutable snapshot of all state that depends only on historical sessions
 * (every session except the current/last one). Stored in MainActivity so that
 * the expensive historical re-processing is skipped on subsequent requests
 * that only differ in the last session.
 */
public class RuletaSnapshot {

    /** Raw hit counts per number (porcentaje is NOT stored — depends on final total). */
    public final Map<String, Integer> vecesCayoHistorico;
    public final long historicalSpinCount;

    // Color / parity / dozen tallies
    public final int bolasRojas;
    public final int bolasNegras;
    public final int numeroPares;
    public final int numerosImpares;
    public final int numerosPrimerDocena;
    public final int numerosSegundaDocena;
    public final int numerosTerceraDocena;

    /** Full transition matrix: what number followed X across all historical sessions. */
    public final Map<String, Map<String, Integer>> transiciones;

    /** 2-gram matrix: what followed the pair "X-Y". */
    public final Map<String, Map<String, Integer>> dosGramas;

    /** 3-gram matrix: what followed the triple "X-Y-Z". */
    public final Map<String, Map<String, Integer>> tresGramas;

    /**
     * Modulo analysis stored as int[2] = {hits, total} to avoid mutable AtomicInteger
     * leaking outside the snapshot.
     */
    public final Map<String, int[]> moduloSinPuertas;
    public final Map<String, int[]> moduloConPuertas;

    public RuletaSnapshot(
            Map<String, Integer> vecesCayoHistorico,
            long historicalSpinCount,
            int bolasRojas, int bolasNegras,
            int numeroPares, int numerosImpares,
            int numerosPrimerDocena, int numerosSegundaDocena, int numerosTerceraDocena,
            Map<String, Map<String, Integer>> transiciones,
            Map<String, Map<String, Integer>> dosGramas,
            Map<String, Map<String, Integer>> tresGramas,
            Map<String, int[]> moduloSinPuertas,
            Map<String, int[]> moduloConPuertas) {

        this.vecesCayoHistorico = vecesCayoHistorico;
        this.historicalSpinCount = historicalSpinCount;
        this.bolasRojas = bolasRojas;
        this.bolasNegras = bolasNegras;
        this.numeroPares = numeroPares;
        this.numerosImpares = numerosImpares;
        this.numerosPrimerDocena = numerosPrimerDocena;
        this.numerosSegundaDocena = numerosSegundaDocena;
        this.numerosTerceraDocena = numerosTerceraDocena;
        this.transiciones = transiciones;
        this.dosGramas = dosGramas;
        this.tresGramas = tresGramas;
        this.moduloSinPuertas = moduloSinPuertas;
        this.moduloConPuertas = moduloConPuertas;
    }
}
