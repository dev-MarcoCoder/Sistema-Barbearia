package br.com.barbearia.service;

/**
 * Erro de regra de negocio (ex.: "horario ja ocupado").
 * A mensagem e escrita para ser mostrada diretamente ao usuario.
 */
public class RegraNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
