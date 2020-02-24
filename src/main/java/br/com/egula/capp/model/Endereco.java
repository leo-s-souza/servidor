/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

/**
 * Model de Endereço. Utilizado para endereços de usuário ou lojas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class Endereco {
    private int id;
    private String logradouro;
    private String numero;
    private String complemento;
    private String referencia;
    private String bairro;
    private String cidade;
    private String estado;
    private String pais;
    private String cep;
    private double latitude;
    private double longitude;

    /**
     * Classe de Endereço.
     *
     * @param id          Id do Endereço.
     * @param logradouro  Logradouro do Endereço.
     * @param numero      Numero do Endereço.
     * @param complemento Complemento do Endereço.
     * @param referencia  Ponto de Referência do Endereço.
     * @param bairro      Bairro do Endereço.
     * @param cidade      Cidade do Endereço.
     * @param estado      Estado do Endereço.
     * @param pais        País do Endereço.
     * @param cep         CEP do Endereço.
     */
    public Endereco(int id, String logradouro, String numero, String complemento, String referencia, String bairro, String cidade, String estado, String pais, String cep, double latitude, double longitude) {
        this.id = id;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.referencia = referencia;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.pais = pais;
        this.cep = cep;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Retorna o ID do endereço.
     *
     * @return ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Retorna o Logradouro (Nome da rua).
     *
     * @return Logradouro do endereço.
     */
    public String getLogradouro() {
        return logradouro;
    }

    /**
     * Retorna o número.
     *
     * @return Número do endereço.
     */
    public String getNumero() {
        return numero;
    }

    /**
     * Retorna o complemento.
     *
     * @return Complemento do endereço. (Pode ser null)
     */
    public String getComplemento() {
        return complemento;
    }

    /**
     * Retorna a referência.
     *
     * @return Referência do endereço. (Pode ser null)
     */
    public String getReferencia() {
        return referencia;
    }

    /**
     * Retorna o bairro.
     *
     * @return Bairro do endereço.
     */
    public String getBairro() {
        return bairro;
    }

    /**
     * Retorna a cidade.
     *
     * @return Cidade do endereço.
     */
    public String getCidade() {
        return cidade;
    }

    /**
     * Retorna o estado.
     *
     * @return Estado do endereço.
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Retorna o pais.
     *
     * @return Pais do endereço.
     */
    public String getPais() {
        return pais;
    }

    /**
     * Retorna o CEP.
     *
     * @return CEP do endereço. (Pode ser null)
     */
    public String getCep() {
        return cep;
    }

    /**
     * Retorna a latitude.
     *
     * @return Latitude do endereço.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Retorna a longitude.
     *
     * @return Longitude do Endereço.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Esse set é utilizado durante um cadastro de novo endereço.
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Retorna o endereço tratado para consulta no Google Maps API.
     *
     * @return String - Endereço tratado.
     */
    public String getEnderecoParaConsulta() {
        StringBuilder sb = new StringBuilder();

        sb.append(this.logradouro != null && !this.logradouro.isEmpty() ? this.logradouro + ", " : "");
        sb.append(this.numero != null && !this.numero.isEmpty() ? this.numero + ", " : "");
        sb.append(this.bairro != null && !this.bairro.isEmpty() ? this.bairro + ", " : "");
        sb.append(this.cidade != null && !this.cidade.isEmpty() ? this.cidade + " - " : "");
        sb.append(this.estado != null && !this.estado.isEmpty() ? this.estado + ", " : "");
        sb.append(this.cep != null && !this.cep.isEmpty() ? this.cep + ", " : "");
        sb.append(this.pais != null && !this.pais.isEmpty() ? this.pais : "");

        return sb.toString().trim();
    }
}

