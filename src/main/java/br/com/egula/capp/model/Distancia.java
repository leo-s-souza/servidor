/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

public class Distancia {

    private int id;
    private int enderecoOrigemID;
    private int enderecoDestinoID;
    private double distancia;
    private long tempo;

    public Distancia(int id, int enderecoOrigemID, int enderecoDestinoID, double distancia, long tempo) {
        this.id = id;
        this.enderecoOrigemID = enderecoOrigemID;
        this.enderecoDestinoID = enderecoDestinoID;
        this.distancia = distancia;
        this.tempo = tempo;
    }

    public int getId() {
        return id;
    }

    public int getEnderecoOrigemID() {
        return enderecoOrigemID;
    }

    public int getEnderecoDestinoID() {
        return enderecoDestinoID;
    }

    public double getDistancia() {
        return distancia;
    }

    public long getTempo() {
        return tempo;
    }
}
