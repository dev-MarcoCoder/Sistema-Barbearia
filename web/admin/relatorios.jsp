<%--
    Relatorios do mes: faturamento, ticket medio, faltas e rankings.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jspf/admin-init.jspf" %>
<%@page import="java.math.BigDecimal"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.YearMonth"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="br.com.barbearia.model.ItemRanking"%>
<%@page import="br.com.barbearia.model.ResumoPeriodo"%>
<%@page import="br.com.barbearia.dao.RelatorioDAO"%>
<%@page import="br.com.barbearia.util.Formato"%>
<%@page import="br.com.barbearia.util.Param"%>
<%
    String tituloPagina = "Relatórios";
    String paginaAtiva = "relatorios";

    YearMonth mes = Param.mes(request.getParameter("mes"));
    if (mes == null) {
        mes = YearMonth.now();
    }
    LocalDate inicio = mes.atDay(1);
    LocalDate fim = mes.atEndOfMonth();

    RelatorioDAO dao = new RelatorioDAO();
    ResumoPeriodo resumo = dao.resumo(barbearia.getId(), inicio, fim);
    List<ItemRanking> servicos = dao.rankingServicos(barbearia.getId(), inicio, fim);
    List<ItemRanking> barbeiros = dao.rankingBarbeiros(barbearia.getId(), inicio, fim);
    Map<String, BigDecimal> porDia = new HashMap<>();
    BigDecimal maiorDia = BigDecimal.ONE;
    for (ItemRanking dia : dao.faturamentoPorDia(barbearia.getId(), inicio, fim)) {
        porDia.put(dia.getNome(), dia.getValor());
        if (dia.getValor().compareTo(maiorDia) > 0) {
            maiorDia = dia.getValor();
        }
    }
%>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <%@include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>
<%@include file="/WEB-INF/jspf/admin-menu.jspf" %>

<div class="admin-cabecalho">
    <div>
        <h1>Relatórios</h1>
        <p class="suave sem-margem">Resultado da barbearia em <%= Html.esc(Formato.mesAno(inicio)) %>.</p>
    </div>
    <div class="navegacao-data" style="margin:0">
        <a class="btn btn-secundario btn-pequeno" href="relatorios.jsp?mes=<%= mes.minusMonths(1) %>">← Mês anterior</a>
        <a class="btn btn-secundario btn-pequeno" href="relatorios.jsp">Mês atual</a>
        <a class="btn btn-secundario btn-pequeno" href="relatorios.jsp?mes=<%= mes.plusMonths(1) %>">Próximo →</a>
    </div>
</div>

<div class="stats">
    <div class="stat"><div class="stat-rotulo">Faturamento</div><div class="stat-valor dourado"><%= Formato.moeda(resumo.getFaturado()) %></div></div>
    <div class="stat"><div class="stat-rotulo">Atendimentos concluídos</div><div class="stat-valor"><%= resumo.getConcluidos() %></div></div>
    <div class="stat"><div class="stat-rotulo">Ticket médio</div><div class="stat-valor"><%= Formato.moeda(resumo.getTicketMedio()) %></div></div>
    <div class="stat"><div class="stat-rotulo">Taxa de faltas</div><div class="stat-valor"><%= resumo.getTaxaFaltas() %>%</div>
        <div class="suave pequeno"><%= Formato.plural(resumo.getFaltas(), "falta", "faltas") %> · <%= Formato.plural(resumo.getCancelados(), "cancelamento", "cancelamentos") %></div></div>
</div>

<div class="card">
    <div class="card-titulo">
        <h3>Faturamento por dia</h3>
        <span class="suave pequeno"><%= Formato.plural(resumo.getAgendados(), "atendimento ainda agendado", "atendimentos ainda agendados") %> no mês · previsto <%= Formato.moeda(resumo.getPrevisto()) %></span>
    </div>
    <div class="barras" role="img" aria-label="Gráfico de faturamento por dia do mês">
        <% for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
               BigDecimal valor = porDia.getOrDefault(d.toString(), BigDecimal.ZERO);
               int altura = valor.multiply(BigDecimal.valueOf(100)).divide(maiorDia, 0, java.math.RoundingMode.HALF_UP).intValue();
        %>
        <div class="barra" title="<%= Formato.data(d) %>: <%= Formato.moeda(valor) %>">
            <div class="barra-valor" style="height:<%= altura %>%"></div>
            <span class="barra-rotulo"><%= d.getDayOfMonth() %></span>
        </div>
        <% } %>
    </div>
</div>

<div class="grid grid-2 mt">
    <div class="card">
        <h3>Serviços mais vendidos</h3>
        <% if (servicos.isEmpty()) { %>
            <p class="suave sem-margem">Nenhum atendimento concluído neste mês.</p>
        <% } else { %>
        <table class="tabela" style="min-width:0">
            <thead><tr><th>Serviço</th><th class="num">Qtd.</th><th class="num">Total</th></tr></thead>
            <tbody>
            <% for (ItemRanking item : servicos) { %>
                <tr><td><%= Html.esc(item.getNome()) %></td><td class="num"><%= item.getQuantidade() %></td><td class="num"><%= Formato.moeda(item.getValor()) %></td></tr>
            <% } %>
            </tbody>
        </table>
        <% } %>
    </div>
    <div class="card">
        <h3>Desempenho por barbeiro</h3>
        <% if (barbeiros.isEmpty()) { %>
            <p class="suave sem-margem">Nenhum atendimento concluído neste mês.</p>
        <% } else {
               BigDecimal maior = barbeiros.get(0).getValor().max(BigDecimal.ONE);
               for (ItemRanking item : barbeiros) {
                   int pct = item.getValor().multiply(BigDecimal.valueOf(100)).divide(maior, 0, java.math.RoundingMode.HALF_UP).intValue();
        %>
            <div style="margin-bottom:14px">
                <div style="display:flex;justify-content:space-between;gap:12px">
                    <strong><%= Html.esc(item.getNome()) %></strong>
                    <span><%= Formato.moeda(item.getValor()) %> <span class="suave pequeno">· <%= Formato.plural(item.getQuantidade(), "atendimento", "atendimentos") %></span></span>
                </div>
                <div class="progresso"><div style="width:<%= pct %>%"></div></div>
            </div>
        <%     }
           } %>
    </div>
</div>

<%@include file="/WEB-INF/jspf/admin-fim.jspf" %>
</body>
</html>
