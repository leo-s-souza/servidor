/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.List;

/**
 * Model de Opcional. Trata dos opcionais dos produtos das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Opcional {

    private long id;
    private String nome;
    private boolean massa;
    private List<OpcionalItem> itens;

    /**
     * Construtor.
     *
     * @param id    Id do opcional.
     * @param nome  Nome do opcional.
     * @param massa Se é um grupo de opcionais do tipo Massa.
     * @param itens Lista de itens do opcional.
     */
    public Opcional(long id, String nome, boolean massa, List<OpcionalItem> itens) {
        this.id = id;
        this.nome = nome;
        this.massa = massa;
        this.itens = itens;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public boolean isMassa() {
        return massa;
    }

    public List<OpcionalItem> getItens() {
        return itens;
    }
}
