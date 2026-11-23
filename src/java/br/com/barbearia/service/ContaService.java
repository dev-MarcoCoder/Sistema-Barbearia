package br.com.barbearia.service;

import br.com.barbearia.dao.BarbeariaDAO;
import br.com.barbearia.dao.BarbeiroDAO;
import br.com.barbearia.dao.ConnectionFactory;
import br.com.barbearia.dao.ServicoDAO;
import br.com.barbearia.dao.UsuarioDAO;
import br.com.barbearia.model.Barbearia;
import br.com.barbearia.model.Barbeiro;
import br.com.barbearia.model.Servico;
import br.com.barbearia.model.Usuario;
import br.com.barbearia.util.Param;
import br.com.barbearia.util.Seguranca;
import br.com.barbearia.util.Slug;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/** Cadastro de novas barbearias (onboarding do SaaS) e login do dono. */
public class ContaService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    /** Hash qualquer, usado para o login demorar o mesmo tempo quando o e-mail nao existe. */
    private static final String HASH_FALSO = Seguranca.gerarHashSenha("senha-inexistente");

    private final BarbeariaDAO barbeariaDAO = new BarbeariaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final BarbeiroDAO barbeiroDAO = new BarbeiroDAO();
    private final ServicoDAO servicoDAO = new ServicoDAO();

    /**
     * Cria a barbearia, o usuario dono, um barbeiro e dois servicos iniciais
     * em uma unica transacao: ou cria tudo, ou nada.
     */
    public Usuario cadastrar(String nomeBarbearia, String telefone, String endereco, String nomeDono,
            String email, String senha, String confirmacao) throws RegraNegocioException, SQLException {

        String nomeB = Param.texto(nomeBarbearia);
        String fone = Param.somenteDigitos(telefone);
        String end = Param.texto(endereco);
        String dono = Param.texto(nomeDono);
        String mail = Param.texto(email).toLowerCase();

        if (nomeB.length() < 3 || nomeB.length() > 100) {
            throw new RegraNegocioException("Informe o nome da barbearia (mínimo 3 letras).");
        }
        if (Slug.gerar(nomeB).isEmpty()) {
            throw new RegraNegocioException("O nome da barbearia precisa ter letras ou números.");
        }
        if (fone.length() < 10 || fone.length() > 11) {
            throw new RegraNegocioException("Informe o telefone da barbearia com DDD.");
        }
        if (end.length() > 200) {
            throw new RegraNegocioException("Endereço muito longo.");
        }
        if (dono.length() < 2 || dono.length() > 100) {
            throw new RegraNegocioException("Informe o seu nome.");
        }
        if (!EMAIL.matcher(mail).matches() || mail.length() > 150) {
            throw new RegraNegocioException("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < 8) {
            throw new RegraNegocioException("A senha precisa ter pelo menos 8 caracteres.");
        }
        if (!senha.equals(confirmacao)) {
            throw new RegraNegocioException("As senhas não conferem.");
        }

        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (usuarioDAO.existeEmail(c, mail)) {
                    throw new RegraNegocioException("Já existe uma conta com esse e-mail. Faça login.");
                }

                Barbearia b = new Barbearia();
                b.setNome(nomeB);
                b.setSlug(gerarSlugUnico(c, nomeB));
                b.setTelefone(fone);
                b.setEndereco(end);
                barbeariaDAO.inserir(c, b);

                Usuario u = new Usuario();
                u.setBarbeariaId(b.getId());
                u.setNome(dono);
                u.setEmail(mail);
                u.setSenhaHash(Seguranca.gerarHashSenha(senha));
                usuarioDAO.inserir(c, u);

                // dados iniciais para a barbearia ja sair usando
                Barbeiro barbeiro = new Barbeiro();
                barbeiro.setBarbeariaId(b.getId());
                barbeiro.setNome(dono);
                barbeiroDAO.inserir(c, barbeiro);

                servicoDAO.inserir(c, servicoInicial(b.getId(), "Corte de cabelo", "Corte na tesoura ou máquina", 30, "40.00"));
                servicoDAO.inserir(c, servicoInicial(b.getId(), "Barba", "Barba com toalha quente", 30, "30.00"));

                c.commit();
                return u;
            } catch (RegraNegocioException | SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private Servico servicoInicial(int barbeariaId, String nome, String descricao, int duracao, String preco) {
        Servico s = new Servico();
        s.setBarbeariaId(barbeariaId);
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setDuracaoMin(duracao);
        s.setPreco(new BigDecimal(preco));
        return s;
    }

    /** "barbearia-do-ze", "barbearia-do-ze-2", "barbearia-do-ze-3"... */
    private String gerarSlugUnico(Connection c, String nome) throws SQLException {
        String base = Slug.gerar(nome);
        String slug = base;
        int n = 2;
        while (barbeariaDAO.existeSlug(c, slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    /** Retorna o usuario se e-mail e senha conferem, ou null. */
    public Usuario autenticar(String email, String senha) throws SQLException {
        String mail = Param.texto(email).toLowerCase();
        if (mail.isEmpty() || senha == null || senha.isEmpty()) {
            return null;
        }
        Usuario u = usuarioDAO.buscarPorEmail(mail);
        if (u == null) {
            Seguranca.conferirSenha(senha, HASH_FALSO);
            return null;
        }
        return Seguranca.conferirSenha(senha, u.getSenhaHash()) ? u : null;
    }
}
