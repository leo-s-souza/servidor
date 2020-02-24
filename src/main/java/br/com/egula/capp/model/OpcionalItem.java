/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.List;

/**
 * Model de OpcionalItem. Trata dos itens de grupos de opcionais dos produtos.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class OpcionalItem {

    private long id;
    private String nome;
    private String imagem;
    private double valor;
    private List<ValorEspecial> valorEspecial;
    private boolean padrao;
    private int appTela;
    private int produtoId;

    /**
     * Construtor da entidade OpcionalItem.
     *
     * @param id            - Id do item.
     * @param nome          - Nome do item.
     * @param imagem        - Caminho da imagem no CAPP.
     * @param valor         - Preço do item.
     * @param valorEspecial - Relação de valores do opcional com o tamanho da pizza.
     * @param padrao        - Se é o opcional default (selecionado por padrão).
     */
    public OpcionalItem(long id, String nome, String imagem, Double valor, List<ValorEspecial> valorEspecial, boolean padrao, int appTela, int produtoId) {
        this.id = id;
        this.nome = nome;
        this.imagem = imagem;
        this.valor = valor;
        this.valorEspecial = valorEspecial;
        this.padrao = padrao;
        this.appTela = appTela;
        this.produtoId = produtoId;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getImagem() {
        return imagem;
    }

    public double getValor() {
        return valor;
    }

    public List<ValorEspecial> getValorEspecial() {
        return valorEspecial;
    }

    public boolean isPadrao() {
        return padrao;
    }

    public int appTela() {
        return appTela;
    }

    public int produto() {
        return produtoId;
    }
}
