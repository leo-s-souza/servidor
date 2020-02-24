/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

public class CupomDesconto {
    private int id;
    private int lojaId;
    private int usuarioId;
    private int tipoCupomId;
    private String chaveUtilizacao;
    private double valorDesconto;
    private boolean isAtivo;

    /**
     * @param id - id do cupom de desconto.
     * @param lojaId - id da loja que está ligada ao cupom.
     * @param usuarioId - id do usuário que está ligado ao cupom.
     * @param tipoCupomId - id do tipo de cupom de desconto.
     * @param chaveUtilizacao - chave de utilização do cupom de desconto.
     * @param valorDesconto - valor do desconto do cupom.
     * @param isAtivo - cupom ativo ou não.
     */
    public CupomDesconto(int id, int lojaId, int usuarioId, int tipoCupomId, String chaveUtilizacao, double valorDesconto, boolean isAtivo) {
        this.id = id;
        this.lojaId = lojaId;
        this.usuarioId = usuarioId;
        this.tipoCupomId = tipoCupomId;
        this.chaveUtilizacao = chaveUtilizacao;
        this.valorDesconto = valorDesconto;
        this.isAtivo = isAtivo;
    }

    /**
     * Retorna o ID da forma de pagamento.
     *
     * @return ID da forma de pagamento.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o ID da loja ao qual o cupom está ligada.
     *
     * @return ID da loja ao qual o cupom está ligada.
     */
    public int getLojaId() {
        return lojaId;
    }

    /**
     * Retorna o ID do usuario que está ligado ao cupom.
     *
     * @return ID do usuario que está ligado ao cupom.
     */
    public int getUsuarioId() {
        return usuarioId;
    }

    /**
     * retorna o ID do tipo do cupom.
     *
     * @return ID do tipo do cupom.
     */
    public int getTipoCupomId() {
        return tipoCupomId;
    }

    /**
     * retorna a chave de utilização do cupom.
     *
     * @return chave de utilização do cupom.
     */
    public String getChaveUtilizacao() {
        return chaveUtilizacao;
    }

    /**
     * retorna o valor do desconto do cupom.
     *
     * @return valor do desconto do cupom.
     */
    public double getValorDesconto() {
        return valorDesconto;
    }

    /**
     * retorna se o cupom está ou não ativo.
     *
     * @return cupom está ou não ativo.
     */
    public boolean isAtivo() {
        return isAtivo;
    }
}
