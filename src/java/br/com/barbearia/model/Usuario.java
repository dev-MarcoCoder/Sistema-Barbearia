package br.com.barbearia.model;

import java.io.Serializable;

/** Dono/administrador de uma barbearia, que acessa o painel. Fica guardado na sessao. */
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int barbeariaId;
    private String nome;
    private String email;
    private transient String senhaHash;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBarbeariaId() { return barbeariaId; }
    public void setBarbeariaId(int barbeariaId) { this.barbeariaId = barbeariaId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public String getPrimeiroNome() {
        if (nome == null) {
            return "";
        }
        int espaco = nome.indexOf(' ');
        return espaco > 0 ? nome.substring(0, espaco) : nome;
    }
}
