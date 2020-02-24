/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import java.util.List;
import java.util.Map;

/**
 * Model de Lojas. Armazena as informações úteis das lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class Loja {

    private int id;
    private String nome;
    private String cnpj;
    private String imagem;
    private int tipo;
    private Endereco endereco;
    private List<Horario> horarios;
    private Map<String, Boolean> garcom;

    /**
     * Construtor da entidade Loja.
     *
     * @param id       int - Id da loja.
     * @param nome     String - Nome da loja.
     * @param cnpj     String - CNPJ da loja.
     * @param imagem   String - Nome da imagem de exibição da loja no CAPP.
     * @param tipo     int - Tipo da loja. 1 = Loja livre; 2 = Loja de Testes;
     * @param endereco Endereco - Endereco da loja.
     */
    public Loja(int id, String nome, String cnpj, String imagem, int tipo, Endereco endereco, List<Horario> horarios, Map<String, Boolean> garcom) {
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
        this.imagem = imagem;
        this.tipo = tipo;
        this.endereco = endereco;
        this.horarios = horarios;
        this.garcom = garcom;
    }

    /**
     * Retorna o ID da loja.
     *
     * @return ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o nome da loja.
     *
     * @return Nome de exibição da loja.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o CNPJ da loja.
     *
     * @return CNPJ da loja.
     */
    public String getCnpj() {
        return cnpj;
    }

    /**
     * Retorna o caminho da imagem da loja.
     *
     * @return Caminho (do CAPP) da imagem de exibição da loja.
     */
    public String getImagem() {
        return imagem;
    }

    /**
     * Retorna o tipo da loja.
     *
     * @return 1 = Loja livre; 2 = Loja de Testes;
     */
    public int getTipo() {
        return tipo;
    }

    /**
     * Retorna o endereço da loja.
     *
     * @return Endereco da loja.
     */
    public Endereco getEndereco() {
        return endereco;
    }

    /**
     * Retorna os horários de atendimento da loja.
     *
     * @return Lista de horários de atendimento.
     */
    public List<Horario> getHorarios() {
        return horarios;
    }

    public Map<String, Boolean> getGarcom() {
        return garcom;
    }
}
