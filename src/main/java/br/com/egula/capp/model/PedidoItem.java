/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.List;
import java.util.Map;

/**
 * Model de Item do Pedido.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class PedidoItem {

    private final long id;
    private final String nome;
    private final int quantidade;
    private final double precoTotal;
    private final long tamanhoId;
    private final List<Long> opcionaisId;
    private final List<Long> saboresId;
    private final Map<Long, Double> valorAplicadoSabor;
    private final Map<Long, Double> valorAplicadoOpcionais;
    private final List<Long> ingredientesAdicionadosId;
    private final List<Long> ingredientesRemovidosId;
    private final List<Sabor> sabores;
    private final List<Long> ingredientesCortesiaId;

    public PedidoItem(long id, String nome, int quantidade, double precoTotal, long tamanhoId, List<Long> opcionaisId, List<Long> saboresId, Map<Long, Double> valorAplicadoSabor, Map<Long, Double> valorAplicadoOpcionais, List<Long> ingredientesAdicionadosId, List<Long> ingredientesRemovidosId, List<Sabor> sabores, List<Long> ingredientesCortesiaId) {
        this.id = id;
        this.nome = nome;
        this.quantidade = quantidade;
        this.precoTotal = precoTotal;
        this.tamanhoId = tamanhoId;
        this.opcionaisId = opcionaisId;
        this.saboresId = saboresId;
        this.valorAplicadoSabor = valorAplicadoSabor;
        this.valorAplicadoOpcionais = valorAplicadoOpcionais;
        this.ingredientesAdicionadosId = ingredientesAdicionadosId;
        this.ingredientesRemovidosId = ingredientesRemovidosId;
        this.sabores = sabores;
        this.ingredientesCortesiaId = ingredientesCortesiaId;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getPrecoTotal() {
        return precoTotal;
    }

    public long getTamanhoId() {
        return tamanhoId;
    }

    public List<Long> getOpcionaisId() {
        return opcionaisId;
    }

    public List<Long> getSaboresId() {
        return saboresId;
    }

    public Map<Long, Double> getValorAplicadoSabor() {
        return valorAplicadoSabor;
    }

    public Map<Long, Double> getValorAplicadoOpcionais() {
        return valorAplicadoOpcionais;
    }

    public List<Long> getIngredientesAdicionadosId() {
        return ingredientesAdicionadosId;
    }

    public List<Long> getIngredientesRemovidosId() {
        return ingredientesRemovidosId;
    }

    public List<Sabor> getSabores() {
        return sabores;
    }

    public List<Long> getIngredientesCortesiaId() {
        return ingredientesCortesiaId;
    }
}