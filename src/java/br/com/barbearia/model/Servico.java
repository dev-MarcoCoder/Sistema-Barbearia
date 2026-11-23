package br.com.barbearia.model;

import java.math.BigDecimal;

public class Servico {

    private int id;
    private int barbeariaId;
    private String nome;
    private String descricao;
    private int duracaoMin;
    private BigDecimal preco;
    private boolean ativo = true;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBarbeariaId() { return barbeariaId; }
    public void setBarbeariaId(int barbeariaId) { this.barbeariaId = barbeariaId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getDuracaoMin() { return duracaoMin; }
    public void setDuracaoMin(int duracaoMin) { this.duracaoMin = duracaoMin; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
