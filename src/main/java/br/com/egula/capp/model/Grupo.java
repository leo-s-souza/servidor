/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de Grupo. Trata dos grupos e subgrupos das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Grupo {

    private int id;
    private String nome;
    private String imagem;
    private boolean pizza;
    private boolean subgrupos;
    private int grupoPai;

    /**
     * Construtor da entidade Grupo.
     *
     * @param id     Id do grupo.
     * @param nome   Nome do grupo.
     * @param imagem Nome da imagem de exibição do grupo no CAPP.
     */
    public Grupo(int id, String nome, String imagem, boolean pizza, boolean subgrupos, int grupoPai) {
        this.id = id;
        this.nome = nome;
        this.imagem = imagem;
        this.pizza = pizza;
        this.subgrupos = subgrupos;
        this.grupoPai = grupoPai;
    }

    /**
     * Retorna o ID do grupo.
     *
     * @return ID do grupo.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nome do grupo.
     *
     * @return Nome do grupo.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o nome da imagem de exibição do grupo.
     *
     * @return Nome da imagem.
     */
    public String getImagem() {
        return imagem;
    }


    /**
     * Retorna a o grupo é uma pizza.
     *
     * @return Se é uma pizza.
     */
    public boolean isPizza() {
        return pizza;
    }

    /**
     * Retorna se o grupo tem subsgrupos vinculados.
     *
     * @return Se tem subgrupos.
     */
    public boolean isSubgrupos() {
        return subgrupos;
    }

    /**
     * Define se o grupo tem subgrupos vinculados.
     */
    public void setSubgrupos(boolean subgrupos) {
        this.subgrupos = subgrupos;
    }

    /**
     * Retorna o ID do grupo pai do subgrupo.
     *
     * @return ID do grupo pai.
     */
    public int getGrupoPai() {
        return grupoPai;
    }
}