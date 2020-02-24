/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model.garcom;

import br.com.egula.capp.model.PedidoItem;

import java.util.List;

/**
 * Model de Pedido Garçom.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class PedidoGarcom {

    private final long lojaId;
    private final long garcomId;
    private final String dispositivoId;
    private final double totalPedido;
    private final int mesa;
    private final int comanda;
    private final List<PedidoItem> produtos;

    public PedidoGarcom(long lojaId, String dispositivoId, long garcomId, double totalPedido, int mesa, int comanda, List<PedidoItem> produtos) {
        this.lojaId = lojaId;
        this.dispositivoId = dispositivoId;
        this.garcomId = garcomId;
        this.totalPedido = totalPedido;
        this.mesa = mesa;
        this.comanda = comanda;
        this.produtos = produtos;
    }

    public long getLojaId() {
        return lojaId;
    }

    public long getGarcomId() {
        return garcomId;
    }

    public String getDispositivoId() {
        return dispositivoId;
    }

    public double getTotalPedido() {
        return totalPedido;
    }

    public int getMesa() {
        return mesa;
    }

    public int getComanda() {
        return comanda;
    }

    public List<PedidoItem> getProdutos() {
        return produtos;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        for (PedidoItem p : produtos) {
            sb.append("ID: ").append(p.getId()).append("\n")
                    .append("Nome: ").append(p.getNome()).append("\n")
                    .append("Quantidade: ").append(p.getQuantidade()).append("\n")
                    .append("Preço Total: ").append(p.getPrecoTotal()).append("\n")
                    .append("Tamanho: ").append(p.getTamanhoId()).append("\n")
                    .append("Opcionais: ").append(p.getOpcionaisId()).append("\n")
                    .append("Sabores: ").append(p.getSaboresId()).append("\n")
                    .append("Ingredientes Adicionados: ").append(p.getIngredientesAdicionadosId()).append("\n")
                    .append("Ingredientes Removidos: ").append(p.getIngredientesRemovidosId())
                    .append(";\n ");
        }

        return "ID da loja: " + lojaId + ";\n " +
                "ID do dispositivo: " + dispositivoId + ";\n " +
                "ID do usuário: " + garcomId + ";\n " +
                "Total do pedido: " + totalPedido + ";\n " +
                "Mesa: " + mesa + ";\n " +
                "Comanda: " + comanda + ";\n " +
                "Produtos: " + sb.toString();
    }
}
