package br.com.barbearia.model;

import java.math.BigDecimal;

/**
 * Planos de assinatura do SaaS. Cada barbearia cliente assina um plano mensal.
 * Os limites de cada plano sao aplicados pelas regras de negocio (GestaoService).
 */
public enum Plano {

    BASICO("Básico", new BigDecimal("49.90"), 2, false,
            "Para barbearias pequenas que estão começando a agenda online."),
    PROFISSIONAL("Profissional", new BigDecimal("89.90"), Integer.MAX_VALUE, true,
            "Para barbearias com equipe maior que querem acompanhar resultados.");

    private final String nome;
    private final BigDecimal precoMensal;
    private final int limiteBarbeiros;
    private final boolean relatorios;
    private final String descricao;

    Plano(String nome, BigDecimal precoMensal, int limiteBarbeiros, boolean relatorios, String descricao) {
        this.nome = nome;
        this.precoMensal = precoMensal;
        this.limiteBarbeiros = limiteBarbeiros;
        this.relatorios = relatorios;
        this.descricao = descricao;
    }

    public String getNome() { return nome; }
    public BigDecimal getPrecoMensal() { return precoMensal; }
    public int getLimiteBarbeiros() { return limiteBarbeiros; }
    public boolean temRelatorios() { return relatorios; }
    public String getDescricao() { return descricao; }

    public boolean barbeirosIlimitados() {
        return limiteBarbeiros == Integer.MAX_VALUE;
    }

    public String getLimiteBarbeirosTexto() {
        return barbeirosIlimitados() ? "Barbeiros ilimitados" : "Até " + limiteBarbeiros + " barbeiros";
    }

    /** Converte o texto vindo do formulario/banco, devolvendo null se for invalido. */
    public static Plano deTexto(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
