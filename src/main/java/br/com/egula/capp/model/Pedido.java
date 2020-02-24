/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.List;

/**
 * Model de Pedido.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class Pedido {

    private final long lojaId;
    private final long usuarioId;
    private final String dispositivoId;
    private final long enderecoId;
    private final long entregaId;
    private final long pagamentoId;
    private final double totalPedido;
    private final double troco;
    private final double valorEntrega;
    private final Distancia distancia;
    private final List<PedidoItem> produtos;

    public Pedido(long lojaId, String dispositivoId, long usuarioId, long enderecoId, double totalPedido, long entregaId, long pagamentoId, double troco, double valorEntrega, Distancia distancia, List<PedidoItem> produtos) {
        this.lojaId = lojaId;
        this.dispositivoId = dispositivoId;
        this.usuarioId = usuarioId;
        this.enderecoId = enderecoId;
        this.totalPedido = totalPedido;
        this.entregaId = entregaId;
        this.pagamentoId = pagamentoId;
        this.troco = troco;
        this.valorEntrega = valorEntrega;
        this.distancia = distancia;
        this.produtos = produtos;
    }

    public long getLojaId() {
        return lojaId;
    }

    public String getDispositivoId() {
        return dispositivoId;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    public long getEnderecoId() {
        return enderecoId;
    }

    public double getTotalPedido() {
        return totalPedido;
    }

    public long getEntregaId() {
        return entregaId;
    }

    public long getPagamentoId() {
        return pagamentoId;
    }

    public double getTroco() {
        return troco;
    }

    public List<PedidoItem> getProdutos() {
        return produtos;
    }

    public double getValorEntrega() {
        return valorEntrega;
    }

    public Distancia getDistancia() {
        return distancia;
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
                "ID do usuário: " + usuarioId + ";\n " +
                "ID da forma de entrega: " + entregaId + ";\n " +
                "ID do endereco: " + enderecoId + ";\n " +
                "ID da forma de pagamento: " + pagamentoId + ";\n " +
                "Total do pedido: " + totalPedido + ";\n " +
                "Troco do pedido: " + troco + ";\n " +
                "Valor da entrega: " + valorEntrega + ";\n " +
                "Produtos: " + sb.toString();
    }
}
