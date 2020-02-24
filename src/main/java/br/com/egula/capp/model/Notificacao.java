/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.Date;

public class Notificacao {

    private String titulo;
    private String mensagem;
    private String imagem;
    private int tipo;
    private Date horario;
    private Date visualizada;

    public Notificacao(String titulo, String mensagem, String imagem, int tipo, Date horario, Date visualizada) {
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.imagem = imagem;
        this.tipo = tipo;
        this.horario = horario;
        this.visualizada = visualizada;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getImagem() {
        return imagem;
    }

    public int getTipo() {
        return tipo;
    }

    public Date getHorario() {
        return horario;
    }

    public Date getVisualizada() {
        return visualizada;
    }
}
