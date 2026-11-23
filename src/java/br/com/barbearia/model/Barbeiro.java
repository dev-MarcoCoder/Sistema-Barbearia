package br.com.barbearia.model;

public class Barbeiro {

    private int id;
    private int barbeariaId;
    private String nome;
    private boolean ativo = true;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBarbeariaId() { return barbeariaId; }
    public void setBarbeariaId(int barbeariaId) { this.barbeariaId = barbeariaId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
