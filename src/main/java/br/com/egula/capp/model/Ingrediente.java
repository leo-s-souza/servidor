/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

public class Ingrediente {

    private int id;
    private String nome;
    private Double valor;
    private int grupoSubs;
    private boolean removivel;
    private int produtoId;
    private int modificado;

    /**
     * Construtor da entidade Ingrediente.
     *
     * @param id         String - Id do ingrediente.
     * @param nome       String - Nome do ingrediente.
     * @param valor      String - Preço do ingrediente.
     * @param grupoSubs  String - ID do item que o ingrediente pode substituir.
     * @param removivel  boolean - Diz se o ingrediente pode ser removido ou não.
     * @param produtoId  int - Id do produto na base.
     * @param modificado int - Diz de o ingrediente foi modificado. 0 = Não modificado; 1 = Removido; 2 = Adicionado.
     */
    public Ingrediente(int id, String nome, Double valor, int grupoSubs, boolean removivel, int produtoId, int modificado) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.grupoSubs = grupoSubs;
        this.removivel = removivel;
        this.produtoId = produtoId;
        this.modificado = modificado;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Double getValor() {
        return valor;
    }

    public long getGrupoSubs() {
        return grupoSubs;
    }

    public boolean isRemovivel() {
        return removivel;
    }

    public int produto() {
        return produtoId;
    }

    public int getModificado() {
        return modificado;
    }
}