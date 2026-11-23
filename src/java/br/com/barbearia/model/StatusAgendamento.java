package br.com.barbearia.model;

public enum StatusAgendamento {

    AGENDADO("Agendado"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado"),
    FALTOU("Não compareceu");

    private final String descricao;

    StatusAgendamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Converte o texto vindo do formulario/banco, devolvendo null se for invalido. */
    public static StatusAgendamento deTexto(String valor) {
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
