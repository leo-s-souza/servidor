/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model de Sabor. Trata dos sabores de pizza.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Sabor {

    private int id;
    private String nome;
    private List<Ingrediente> ingredientesBase;
    private List<Ingrediente> ingredientesAdicionais;
    private List<ValorEspecial> valorEspecial;
    private int produtoId;
    private int quantidade;

    public Sabor(int id, String nome, List<Ingrediente> ingredientesBase, List<Ingrediente> ingredientesAdicionais, List<ValorEspecial> valorEspecial, int produtoId) {
        this.id = id;
        this.nome = nome;
        this.ingredientesBase = ingredientesBase;
        this.ingredientesAdicionais = ingredientesAdicionais;
        this.valorEspecial = valorEspecial;
        this.quantidade = 0;
        this.produtoId = produtoId;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<Ingrediente> getIngredientesBase() {
        if (ingredientesBase != null) {
            return ingredientesBase;

        } else {
            return new ArrayList<>();
        }
    }

    public List<Ingrediente> getIngredientesAdicionais() {
        if (ingredientesAdicionais != null) {
            return ingredientesAdicionais;

        } else {
            return new ArrayList<>();
        }
    }

    public List<Long> getIngredientesRemovidos() {
        List<Long> ingredientes = new ArrayList<>();
        for (Ingrediente ingrediente : ingredientesBase) {
            if (ingrediente.getModificado() == 1) {
                ingredientes.add(ingrediente.getId());
            }
        }
        return ingredientes;
    }

    public List<Long> getIngredientesAdicionados() {
        List<Long> ingredientes = new ArrayList<>();
        for (Ingrediente ingrediente : ingredientesAdicionais) {
            if (ingrediente.getModificado() == 2) {
                ingredientes.add(ingrediente.getId());
            }
        }
        return ingredientes;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public List<ValorEspecial> getValorEspecial() {
        return valorEspecial;
    }

    public int produto() {
        return produtoId;
    }
}