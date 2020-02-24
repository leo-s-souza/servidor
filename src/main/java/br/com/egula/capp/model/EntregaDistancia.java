/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de Distância de Entrega. Trata dos das distâncias possíveis de entrega, bem como o valor destas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class EntregaDistancia {

    private int id;
    private int entrega;
    private double distanciaMinimo;
    private double distanciaMaximo;
    private double valor;

    /**
     * @param id              ID da distância de entrega.
     * @param entrega         ID de entrega.
     * @param distanciaMinimo Distância mínima.
     * @param distanciaMaximo Distância máxima.
     * @param valor           Valor deste intervalo de entrega.
     */
    public EntregaDistancia(int id, int entrega, double distanciaMinimo, double distanciaMaximo, double valor) {
        this.id = id;
        this.entrega = entrega;
        this.distanciaMinimo = distanciaMinimo;
        this.distanciaMaximo = distanciaMaximo;
        this.valor = valor;
    }

    /**
     * Retorna o ID da distância de entrega.
     *
     * @return ID da distância de entrega.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o ID da entrega vinculada à distância de entrega.
     *
     * @return ID de entrega.
     */
    public int getEntrega() {
        return entrega;
    }

    /**
     * Retorna a distância minima de entrega.
     *
     * @return Distância mínima.
     */
    public double getDistanciaMinimo() {
        return distanciaMinimo;
    }

    /**
     * Retorna a distância máxima de entrega.
     *
     * @return Distância máxima.
     */
    public double getDistanciaMaximo() {
        return distanciaMaximo;
    }

    /**
     * Retorna o valor da distância de entrega.
     *
     * @return Valor deste intervalo de entrega.
     */
    public double getValor() {
        return valor;
    }
}
