/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model de Produto. Trata dos produtos das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Produto {

    private int id;
    private String nome;
    private String imagem;
    private double valor;
    private int grupo;
    private List<Ingrediente> ingredientesBase;
    private List<Ingrediente> ingredientesAdicionais;
    private List<Tamanho> tamanhos;
    private List<Opcional> opcionais;
    private List<Sabor> sabores;
    private int quantIngrCortesia;

    /**
     * Construtor da entidade Produto.
     *
     * @param id     Id do produto.
     * @param nome   Nome do produto.
     * @param imagem Nome da imagem de exibição do produto no CAPP.
     * @param valor  Preço do produto.
     * @param grupo  ID do grupo que contém esse produto.
     */
    public Produto(int id, String nome, String imagem, double valor, int grupo, List<Ingrediente> ingredientesBase, List<Ingrediente> ingredientesAdicionais, List<Tamanho> tamanhos, List<Opcional> opcionais, List<Sabor> sabores, int quantIngrCortesia) {
        this.id = id;
        this.nome = nome;
        this.imagem = imagem;
        this.valor = valor;
        this.grupo = grupo;
        this.ingredientesBase = ingredientesBase;
        this.ingredientesAdicionais = ingredientesAdicionais;
        this.tamanhos = tamanhos;
        this.opcionais = opcionais;
        this.sabores = sabores;
        this.quantIngrCortesia = quantIngrCortesia;
    }

    public int getId() {
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

    public int grupo() {
        return grupo;
    }

    public List<Ingrediente> getIngredientesBase() {
        if (ingredientesBase != null) {
            return ingredientesBase;

        } else {
            return new ArrayList<>();
        }
    }

    public void setIngredientesBase(List<Ingrediente> ingredientesBase) {
        this.ingredientesBase = ingredientesBase;
    }

    public List<Ingrediente> getIngredientesAdicionais() {
        if (ingredientesAdicionais != null) {
            return ingredientesAdicionais;

        } else {
            return new ArrayList<>();
        }
    }

    public void setIngredientesAdicionais(List<Ingrediente> ingredientesAdicionais) {
        this.ingredientesAdicionais = ingredientesAdicionais;
    }

    public List<Tamanho> getTamanhos() {
        return tamanhos;
    }

    public void setTamanhos(List<Tamanho> tamanhos) {
        this.tamanhos = tamanhos;
    }

    public List<Opcional> getOpcionais() {
        return opcionais;
    }

    public void setOpcionais(List<Opcional> opcionais) {
        this.opcionais = opcionais;
    }

    public List<Sabor> getSabores() {
        return sabores;
    }

    public void setSabores(List<Sabor> sabores) {
        this.sabores = sabores;
    }

    public int getQuantIngrCortesia() {
        return quantIngrCortesia;
    }

    public void setQuantIngrCortesia(int quantIngrCortesia) {
        this.quantIngrCortesia = quantIngrCortesia;
    }
}
