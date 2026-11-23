package br.com.barbearia.service;

import br.com.barbearia.dao.BarbeariaDAO;
import br.com.barbearia.dao.BarbeiroDAO;
import br.com.barbearia.dao.ServicoDAO;
import br.com.barbearia.model.Barbearia;
import br.com.barbearia.model.Barbeiro;
import br.com.barbearia.model.Plano;
import br.com.barbearia.model.Servico;
import br.com.barbearia.util.Param;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

/** Regras do painel administrativo: barbeiros, servicos, configuracoes e plano. */
public class GestaoService {

    private static final Set<Integer> INTERVALOS_VALIDOS = Set.of(10, 15, 20, 30, 45, 60);

    private final BarbeariaDAO barbeariaDAO = new BarbeariaDAO();
    private final BarbeiroDAO barbeiroDAO = new BarbeiroDAO();
    private final ServicoDAO servicoDAO = new ServicoDAO();

    // ---------- Barbeiros ----------

    public void adicionarBarbeiro(Barbearia b, String nome) throws RegraNegocioException, SQLException {
        String n = validarNomeBarbeiro(nome);
        verificarLimitePlano(b);
        Barbeiro barbeiro = new Barbeiro();
        barbeiro.setBarbeariaId(b.getId());
        barbeiro.setNome(n);
        barbeiroDAO.inserir(barbeiro);
    }

    public void renomearBarbeiro(Barbearia b, int id, String nome) throws RegraNegocioException, SQLException {
        String n = validarNomeBarbeiro(nome);
        exigirBarbeiro(b, id);
        barbeiroDAO.renomear(id, b.getId(), n);
    }

    public void ativarBarbeiro(Barbearia b, int id) throws RegraNegocioException, SQLException {
        Barbeiro barbeiro = exigirBarbeiro(b, id);
        if (!barbeiro.isAtivo()) {
            verificarLimitePlano(b);
            barbeiroDAO.definirAtivo(id, b.getId(), true);
        }
    }

    public void desativarBarbeiro(Barbearia b, int id) throws RegraNegocioException, SQLException {
        Barbeiro barbeiro = exigirBarbeiro(b, id);
        if (barbeiro.isAtivo()) {
            if (barbeiroDAO.contarAtivos(b.getId()) <= 1) {
                throw new RegraNegocioException("A barbearia precisa ter pelo menos um barbeiro ativo.");
            }
            barbeiroDAO.definirAtivo(id, b.getId(), false);
        }
    }

    private void verificarLimitePlano(Barbearia b) throws RegraNegocioException, SQLException {
        Plano plano = b.getPlano();
        if (barbeiroDAO.contarAtivos(b.getId()) >= plano.getLimiteBarbeiros()) {
            throw new RegraNegocioException("O plano " + plano.getNome() + " permite até " + plano.getLimiteBarbeiros()
                    + " barbeiros ativos. Mude para o plano Profissional para adicionar mais.");
        }
    }

    private String validarNomeBarbeiro(String nome) throws RegraNegocioException {
        String n = Param.texto(nome);
        if (n.length() < 2 || n.length() > 100) {
            throw new RegraNegocioException("Informe o nome do barbeiro.");
        }
        return n;
    }

    private Barbeiro exigirBarbeiro(Barbearia b, int id) throws RegraNegocioException, SQLException {
        Barbeiro barbeiro = barbeiroDAO.buscar(id, b.getId());
        if (barbeiro == null) {
            throw new RegraNegocioException("Barbeiro não encontrado.");
        }
        return barbeiro;
    }

    // ---------- Servicos ----------

    public void salvarServico(Barbearia b, int id, String nome, String descricao, String duracao, String preco)
            throws RegraNegocioException, SQLException {

        String n = Param.texto(nome);
        String d = Param.texto(descricao);
        int minutos = Param.inteiro(duracao, -1);
        BigDecimal valor = Param.decimal(preco);

        if (n.length() < 2 || n.length() > 80) {
            throw new RegraNegocioException("Informe o nome do serviço.");
        }
        if (d.length() > 200) {
            throw new RegraNegocioException("A descrição pode ter no máximo 200 caracteres.");
        }
        if (minutos < 5 || minutos > 480) {
            throw new RegraNegocioException("A duração deve ficar entre 5 e 480 minutos.");
        }
        if (valor == null || valor.signum() < 0 || valor.compareTo(new BigDecimal("10000")) > 0) {
            throw new RegraNegocioException("Informe um preço válido, por exemplo 45,00.");
        }

        Servico s = new Servico();
        s.setId(id);
        s.setBarbeariaId(b.getId());
        s.setNome(n);
        s.setDescricao(d);
        s.setDuracaoMin(minutos);
        s.setPreco(valor);

        if (id > 0) {
            exigirServico(b, id);
            servicoDAO.atualizar(s);
        } else {
            servicoDAO.inserir(s);
        }
    }

    public void definirServicoAtivo(Barbearia b, int id, boolean ativo) throws RegraNegocioException, SQLException {
        Servico s = exigirServico(b, id);
        if (!ativo && s.isAtivo() && servicoDAO.listar(b.getId(), true).size() <= 1) {
            throw new RegraNegocioException("A barbearia precisa ter pelo menos um serviço ativo.");
        }
        servicoDAO.definirAtivo(id, b.getId(), ativo);
    }

    private Servico exigirServico(Barbearia b, int id) throws RegraNegocioException, SQLException {
        Servico s = servicoDAO.buscar(id, b.getId());
        if (s == null) {
            throw new RegraNegocioException("Serviço não encontrado.");
        }
        return s;
    }

    // ---------- Configuracoes ----------

    public void salvarDados(Barbearia b, String nome, String telefone, String endereco)
            throws RegraNegocioException, SQLException {
        String n = Param.texto(nome);
        String fone = Param.somenteDigitos(telefone);
        String end = Param.texto(endereco);
        if (n.length() < 3 || n.length() > 100) {
            throw new RegraNegocioException("Informe o nome da barbearia.");
        }
        if (fone.length() < 10 || fone.length() > 11) {
            throw new RegraNegocioException("Informe o telefone com DDD.");
        }
        if (end.length() > 200) {
            throw new RegraNegocioException("Endereço muito longo.");
        }
        b.setNome(n);
        b.setTelefone(fone);
        b.setEndereco(end);
        barbeariaDAO.atualizarDados(b);
    }

    public void salvarHorarios(Barbearia b, String abertura, String fechamento, String intervalo, String[] dias)
            throws RegraNegocioException, SQLException {
        LocalTime abre = Param.hora(abertura);
        LocalTime fecha = Param.hora(fechamento);
        int passo = Param.inteiro(intervalo, -1);

        if (abre == null || fecha == null || !abre.isBefore(fecha)) {
            throw new RegraNegocioException("O horário de abertura precisa ser antes do fechamento.");
        }
        if (!INTERVALOS_VALIDOS.contains(passo)) {
            throw new RegraNegocioException("Escolha um intervalo válido entre os horários.");
        }
        Set<Integer> diasSelecionados = new TreeSet<>();
        if (dias != null) {
            for (String dia : dias) {
                int d = Param.inteiro(dia, 0);
                if (d >= 1 && d <= 7) {
                    diasSelecionados.add(d);
                }
            }
        }
        if (diasSelecionados.isEmpty()) {
            throw new RegraNegocioException("Selecione pelo menos um dia de funcionamento.");
        }

        b.setHoraAbertura(abre);
        b.setHoraFechamento(fecha);
        b.setIntervaloMin(passo);
        b.setDiasFuncionamento(diasSelecionados);
        barbeariaDAO.atualizarHorarios(b);
    }

    // ---------- Plano (assinatura) ----------

    public void alterarPlano(Barbearia b, String novoPlano) throws RegraNegocioException, SQLException {
        Plano plano = Plano.deTexto(novoPlano);
        if (plano == null) {
            throw new RegraNegocioException("Plano inválido.");
        }
        int ativos = barbeiroDAO.contarAtivos(b.getId());
        if (ativos > plano.getLimiteBarbeiros()) {
            throw new RegraNegocioException("Você tem " + ativos + " barbeiros ativos e o plano " + plano.getNome()
                    + " permite " + plano.getLimiteBarbeiros() + ". Desative barbeiros antes de mudar.");
        }
        barbeariaDAO.atualizarPlano(b.getId(), plano);
        b.setPlano(plano);
    }
}
