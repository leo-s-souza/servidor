/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.Date;

/**
 * Model de Horário. Trata dos horários de atendimento das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Horario {

    private long id;
    private int diaSemana;
    private Date horaInicio;
    private Date horaFim;
    private double valor;
    private int tipo;
    private Date dataExata;

    /**
     * @param id         ID do horário.
     * @param diaSemana  Dia da semana em que é aplicado.
     * @param horaInicio Horário de início.
     * @param horaFim    Horário de fim.
     * @param valor      Valor de acréscimo cobrado nesse horário.
     * @param tipo       Tipo de atendimento disponível.
     */
    public Horario(long id, int diaSemana, Date horaInicio, Date horaFim, double valor, int tipo, Date dataExata) {
        this.id = id;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.valor = valor;
        this.tipo = tipo;
        this.dataExata = dataExata;
    }

    /**
     * Retorna o ID do horário.
     *
     * @return ID do horário.
     */
    public long getId() {
        return id;
    }

    /**
     * Retorna o dia da semana em que o horário é aplicado.
     *
     * @return Dia da semana em que é aplicado.
     */
    public int getDiaSemana() {
        return diaSemana;
    }

    /**
     * Retorna a hora inicial deste horário de funcionamento.
     *
     * @return Horário de início.
     */
    public Date getHoraInicio() {
        return horaInicio;
    }

    /**
     * Retorna a hora final deste horário de funcionamento.
     *
     * @return Horário de fim.
     */
    public Date getHoraFim() {
        return horaFim;
    }

    /**
     * Retorna o valor acrescido no pedido caso seja feito dentro deste horário de funcionamento.
     *
     * @return Valor de acréscimo cobrado nesse horário.
     */
    public double getValor() {
        return valor;
    }

    /**
     * Retorna o tipo de atendimento disponível neste horário de funcionamento.
     *
     * @return Tipo de atendimento disponível.
     */
    public int getTipo() {
        return tipo;
    }

    /**
     * Retora a data exata em que esse horario vai ser utilizado.
     *
     * @return
     */
    public Date getDataExata() {
        return dataExata;
    }
}