/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Model de Usuário.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class Usuario {

    private int id;
    private String nome;
    private String sobrenome;
    private String telefone;
    private String email;
    private String cpf;
    private int tipoUsuario;
    private double bonusLivre;
    private double bonusBloqueado;
    private List<Endereco> enderecos;
    private List<Distancia> distancias;

    /**
     * Construtor da entidade Usuario.
     *
     * @param id          Id do usuário.
     * @param nome        Nome do usuário.
     * @param sobrenome   Sobrenome do usuário.
     * @param telefone    Telefone do usuário.
     * @param enderecos   Lista de endereços do usuário.
     * @param tipoUsuario Tipo do usuário para identificar se é um usuário normal ou de testes.
     */
    public Usuario(int id, String nome, String sobrenome, String telefone, String email, String cpf, int tipoUsuario, double bonusLivre, double bonusBloqueado, List<Endereco> enderecos, List<Distancia> distancias) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.telefone = telefone;
        this.email = email;
        this.cpf = cpf;
        this.tipoUsuario = tipoUsuario;
        this.bonusLivre = bonusLivre;
        this.bonusBloqueado = bonusBloqueado;
        this.enderecos = enderecos;
        this.distancias = distancias;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public double getBonusLivre() {
        return bonusLivre;
    }

    public void setBonusLivre(double bonusLivre) {
        this.bonusLivre = bonusLivre;
    }

    public double getBonusBloqueado() {
        return bonusBloqueado;
    }

    public void setBonusBloqueado(double bonusBloqueado) {
        this.bonusBloqueado = bonusBloqueado;
    }

    public List<Endereco> getEnderecos() {
        return enderecos;
    }

    public void setEnderecos(List<Endereco> enderecos) {
        this.enderecos = enderecos;
    }

    public int getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(int tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public List<Distancia> getDistancias() {
        return distancias;
    }

    public void setDistancias(List<Distancia> distancias) {
        this.distancias = distancias;
    }

    public static int getIdUsuarioCpf(String cpf, CSPInstrucoesSQLBase conn) {

        try {
            final ResultSet user = conn.selectOneRow((StringBuilder s) -> {
                s.append("SELECT ");
                s.append("  a.ID ");
                s.append("FROM ");
                s.append("  USUARIO a ");
                s.append("WHERE ");
                s.append("  a.CPF = ?");
            }, cpf);

            return user.getInt("ID");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}

