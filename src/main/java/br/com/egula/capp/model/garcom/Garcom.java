/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model.garcom;

import java.util.List;

public class Garcom {


    private final long id;
    private final String cpf;
    private final String nome;
    private final String sobrenome;
    private final String email;
    private final List<Integer> lojas;

    public Garcom(long id, String cpf, String nome, String sobrenome, String email, List<Integer> lojas) {
        this.id = id;
        this.cpf = cpf;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.lojas = lojas;
    }


    public long getId() {
        return id;
    }

    public String getCpf() {
        return cpf;
    }

    public String getNome() {
        return nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public String getEmail() {
        return email;
    }

    public List<Integer> getLojas() {
        return lojas;
    }
}
