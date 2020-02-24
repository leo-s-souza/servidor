/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de Tamanho. Trata dos tamanhos de produtos das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Tamanho {

    private int id;
    private String nome;
    private double valor;
    private int limiteOpcionais;
    private int limiteSabores;
    private int numeroFatias;
    private double tamanhoMassa;
    private int produtoId;

    /**
     * Construtor.
     *
     * @param id              ID do tamanho.
     * @param nome            Nome do tamanho.
     * @param valor           Valor do tamanho.
     * @param limiteOpcionais Limite de opcionais possíves para o tamanho.
     * @param limiteSabores   Limite de sabores possíveis para o tamanho.
     */
    public Tamanho(int id, String nome, double valor, int limiteOpcionais, int limiteSabores, int numeroFatias, double tamanhoMassa, int produtoId) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.limiteOpcionais = limiteOpcionais;
        this.limiteSabores = limiteSabores;
        this.numeroFatias = numeroFatias;
        this.tamanhoMassa = tamanhoMassa;
        this.produtoId = produtoId;
    }

    /**
     * @return ID do tamanho.
     */
    public int getId() {
        return id;
    }

    /**
     * @return Nome do tamanho.
     */
    public String getNome() {
        return nome;
    }

    /**
     * @return Valor do tamanho.
     */
    public double getValor() {
        return valor;
    }

    /**
     * @return Limite de opcionais possíves para o tamanho.
     */
    public int getLimiteOpcionais() {
        return limiteOpcionais;
    }

    /**
     * @return Limite de sabores possíveis para o tamanho.
     */
    public int getLimiteSabores() {
        return limiteSabores;
    }

    public int getNumeroFatias() {
        return numeroFatias;
    }

    public double getTamanhoMassa() {
        return tamanhoMassa;
    }

    /**
     * @return ID do produto em que esse tamanho está relacionado.
     */
    public int produto() {
        return produtoId;
    }
}
