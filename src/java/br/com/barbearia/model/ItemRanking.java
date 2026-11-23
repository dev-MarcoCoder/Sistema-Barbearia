package br.com.barbearia.model;

import java.math.BigDecimal;

/** Linha de ranking (servico mais vendido, barbeiro que mais atendeu...). */
public class ItemRanking {

    private final String nome;
    private final int quantidade;
    private final BigDecimal valor;

    public ItemRanking(String nome, int quantidade, BigDecimal valor) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.valor = valor == null ? BigDecimal.ZERO : valor;
    }

    public String getNome() { return nome; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getValor() { return valor; }
}
