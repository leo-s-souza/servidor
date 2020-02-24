/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de Pagamento. Trata dos tipos de pagamento das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Pagamento {

    private int id;
    private String nome;
    private boolean troco;
    private boolean bonus;

    /**
     * @param id    Id da forma de pagamento.
     * @param nome  Nome de exibição do tipo de pagamento.
     * @param troco Se possibilita informar troco.
     */
    public Pagamento(int id, String nome, boolean troco, boolean bonus) {
        this.id = id;
        this.nome = nome;
        this.troco = troco;
        this.bonus = bonus;
    }

    /**
     * Retorna o ID da forma de pagamento.
     *
     * @return ID da forma de pagamento.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nome de exibição da forma de pagamento.
     *
     * @return Nome da forma de pagamento.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna se a forma de pagamento permite informar troco.
     *
     * @return Se permite informar troco.
     */
    public boolean isTroco() {
        return troco;
    }

    /**
     * Retorna se a forma de pagamento é do tipo Bônus
     *
     * @return Se é um pagamento do tipo Bônus.
     */
    public boolean isBonus() {
        return bonus;
    }
}

