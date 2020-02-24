/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de Entrega. Trata dos tipos de entrega das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Entrega {

    private int id;
    private String nome;
    private double valor;
    private boolean local;

    /**
     * @param id    ID da forma de entrega.
     * @param nome  Nome da forma de entrega.
     * @param valor Valor base da forma de entrega.
     */
    public Entrega(int id, String nome, double valor, boolean local) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.local = local;
    }

    /**
     * Retorna o ID da forma de entrega.
     *
     * @return ID da forma de entrega.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nome de exibição da forma de entrega.
     *
     * @return Nome da forma de entrega.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o valor base da entrega.
     *
     * @return Valor base.
     */
    public double getValor() {
        return valor;
    }

    /**
     * Retorna se o produto vai ser retirado no local da loja.
     *
     * @return Retirar na loja
     */
    public boolean isLocal() {
        return local;
    }
}
