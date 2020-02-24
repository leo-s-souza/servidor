/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de ValorEspecial. Trata da relação entre tamanho e sabor para definir o valor de cada sabor adicionado à pizza.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class ValorEspecial {

    private int tamanhoId;
    private double valor;

    public ValorEspecial(int tamanhoId, double valor) {
        this.tamanhoId = tamanhoId;
        this.valor = valor;
    }

    public int getTamanhoId() {
        return tamanhoId;
    }

    public double getValor() {
        return valor;
    }

}
