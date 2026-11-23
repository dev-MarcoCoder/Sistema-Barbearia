# BarberAgenda: agendamento online para barbearias (SaaS)

O BarberAgenda é um **SaaS** (software oferecido pela internet, por assinatura mensal) que permite que
barbearias recebam agendamentos de cortes de cabelo pela internet. Este repositório traz o **MVP**,
a primeira versão com as funcionalidades essenciais para demonstrar a proposta.

**O problema:** em muitas barbearias o agendamento ainda é feito por WhatsApp, ligação ou caderno.
O barbeiro precisa parar o atendimento para responder mensagens, acontecem horários marcados em dobro,
clientes faltam sem avisar e o dono não sabe quanto faturou no mês.

**A solução:** cada barbearia cadastrada ganha um link próprio de agendamento. O cliente escolhe o
serviço, o barbeiro, o dia e um horário que realmente está livre, sem precisar criar conta.
O dono acompanha tudo em um painel com a agenda do dia, controle de faltas e relatórios.

📄 Documentação completa do trabalho: [docs/BarberAgenda-Documentacao.pdf](docs/BarberAgenda-Documentacao.pdf)

## Integrantes

- Marco Aurélio Orlandini Mendonça
- _(adicionar os demais integrantes do grupo)_

## Público-alvo

- **Assinante (quem paga):** donos de barbearias pequenas e médias.
- **Usuário final:** clientes da barbearia, que agendam pelo celular sem precisar criar conta.

## Funcionalidades

**Área pública (cliente da barbearia)**
- Página inicial do SaaS com os planos e a lista de barbearias.
- Página de agendamento de cada barbearia (`agendar.jsp?b=nome-da-barbearia`), com uma pergunta por vez:
  serviço → barbeiro → dia → horário livre → nome e celular.
- **Assistente com IA** no chat da página: o cliente marca, consulta ou cancela conversando
  (ver [Assistente com IA](#assistente-com-ia)).
- Os horários livres são calculados levando em conta o expediente, a duração do serviço
  e os horários já ocupados do barbeiro.
- Tela de confirmação com um **código do agendamento**.
- Consulta e cancelamento do agendamento pelo código.

**Painel da barbearia (dono)**
- Cadastro da barbearia (*onboarding*) e login.
- **Agenda do dia**: filtro por barbeiro, totais do dia, ações *Concluir*, *Faltou*, *Cancelar*
  e link para enviar lembrete ao cliente pelo WhatsApp.
- Cadastro de **serviços** (nome, duração, preço) e de **barbeiros**, com desativação que preserva o histórico.
- **Configurações**: dados da barbearia, horário de funcionamento, intervalo entre horários e dias de funcionamento.
- **Relatórios do mês** (plano Profissional): faturamento, ticket médio, taxa de faltas,
  faturamento por dia, serviços mais vendidos e desempenho por barbeiro.
- **Meu plano**: troca entre os planos Básico e Profissional (cobrança simulada).

## Modelo de cobrança (SaaS)

| Plano        | Preço/mês | Barbeiros  | Relatórios |
|--------------|-----------|------------|------------|
| Básico       | R$ 49,90  | até 2      | não        |
| Profissional | R$ 89,90  | ilimitados | sim        |

Os limites de cada plano são aplicados pelo próprio sistema. Por exemplo, o plano Básico bloqueia o
3º barbeiro ativo e não libera os relatórios. Nesta versão a troca de plano não faz cobrança real;
na versão comercial a mensalidade seria cobrada por cartão ou Pix recorrente através de um
gateway de pagamento.

## Tecnologias

- **Java 21** com **JSP** e Servlets (Jakarta EE 10): filtros e *listener*
- **Apache Tomcat 10.1**
- **MySQL 8** com JDBC (driver MySQL Connector/J, incluído em `web/WEB-INF/lib`)
- **API da Anthropic (Managed Agents)** com o SDK Java oficial `anthropic-java` 3.12.0, incluído em `web/WEB-INF/lib`
  com as dependências. O assistente usa o modelo Claude Opus 5.5.
- **HTML, CSS e JavaScript** puros, com layout responsivo para celular
- **NetBeans** com Ant (`build.xml`)

## Arquitetura

O sistema é dividido em três camadas:

```
Navegador (HTML/CSS/JS)
      │  HTTP (GET/POST)
      ▼
Interface: páginas JSP (web/*.jsp, web/admin/*.jsp)
      │  chamam
      ▼
Lógica: classes Java (src/java/br/com/barbearia)
   service/  regras de negócio (horários livres, limites do plano, validações)
   dao/      acesso ao banco com PreparedStatement
   model/    entidades (Barbearia, Barbeiro, Servico, Agendamento...)
   web/      filtros de login/UTF-8 e leitura da configuração do banco
      │  JDBC
      ▼
Armazenamento: MySQL (database/barbearia.sql)
```

- **Multi-inquilino (multi-tenant):** todas as barbearias usam o mesmo sistema e o mesmo banco.
  Toda consulta filtra por `barbearia_id`, então uma barbearia nunca vê os dados de outra.
- **Sem agendamento duplicado:** a confirmação roda em uma transação que trava o barbeiro
  (`SELECT ... FOR UPDATE`) e confere de novo se o horário está livre antes de gravar.

### Segurança

- Senhas guardadas com hash **PBKDF2 + salt**, nunca em texto puro.
- Todas as consultas usam `PreparedStatement`, o que protege contra SQL injection.
- Todo texto digitado pelo usuário é escapado antes de ir para a página, o que protege contra XSS.
- Login controlado por um filtro em `/admin/*`, com token **CSRF** em todos os formulários do painel.
- A sessão é recriada no login, o que evita fixação de sessão.
- Usuário e senha do banco ficam em `web/WEB-INF/db.properties`, que **não vai para o Git**.

## Como executar

### 1. Pré-requisitos
- JDK 21 ou superior
- Apache Tomcat 10.1
- MySQL 8
- NetBeans, opcional mas recomendado

### 2. Criar o banco de dados
Execute o script `database/barbearia.sql` no MySQL Workbench (abra o arquivo e clique no raio)
ou pelo terminal:

```bash
mysql -u root -p < database/barbearia.sql
```

O script cria o banco `barbearia`, as tabelas e dados de demonstração: duas barbearias,
um mês de histórico e agendamentos para hoje e amanhã.

> **Atenção:** o script apaga e recria o banco `barbearia` sempre que é executado.

### 3. Configurar a conexão
Copie o modelo e preencha com o usuário e a senha do seu MySQL:

```bash
cp web/WEB-INF/db.properties.example web/WEB-INF/db.properties
```

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/barbearia
db.usuario=seu_usuario
db.senha=sua_senha
```

### 4. Ativar o assistente com IA (opcional)
Crie uma chave em [console.anthropic.com](https://console.anthropic.com), no menu *API Keys*. Depois copie o modelo
e cole a chave:

```bash
cp web/WEB-INF/ia.properties.example web/WEB-INF/ia.properties
```

```properties
anthropic.api_key=sua_chave
```

O `ia.properties` também está no `.gitignore`. Sem ele, o site funciona normalmente e o chat mostra um aviso.
Na primeira conversa o sistema cria sozinho o agente e o ambiente na Anthropic e guarda os IDs na tabela `config_ia`.

### 5. Rodar
No NetBeans: **File → Open Project**, escolha esta pasta, depois **Clean and Build** e **Run**.
Se o NetBeans avisar que falta o servidor (*Resolve Missing Server Problem*), selecione o Tomcat 10.1.

Outra opção é copiar o `dist/SistemaBarbearia.war` gerado pelo *Clean and Build* para a pasta
`webapps` do Tomcat.

Acesse: **http://localhost:8080/SistemaBarbearia/**

### Contas de demonstração (apenas para teste local)

| Barbearia                 | Plano        | E-mail                     | Senha      |
|---------------------------|--------------|----------------------------|------------|
| Navalha de Ouro Barbearia | Profissional | `admin@navalhadeouro.demo` | `demo1234` |
| Barbearia do Bairro       | Básico       | `contato@dobairro.demo`    | `demo1234` |

Página de agendamento de demonstração: `http://localhost:8080/SistemaBarbearia/agendar.jsp?b=navalha-de-ouro`

## Assistente com IA

O chat da página de agendamento conversa com um agente do **Managed Agents** da Anthropic.

1. O **agente** (modelo, instruções e ferramentas) é criado uma única vez; se as instruções mudarem no código,
   ele é atualizado e ganha uma nova versão.
2. Cada conversa do site abre uma **sessão** desse agente.
3. O `ChatServlet` envia a mensagem e acompanha os eventos da sessão. Quando a IA pede uma ferramenta
   (`agent.custom_tool_use`), o servidor executa no MySQL e devolve o resultado (`user.custom_tool_result`).

Ferramentas disponíveis (`FerramentasIA`): `ver_barbearia`, `listar_servicos`, `listar_barbeiros`,
`ver_horarios_livres`, `criar_agendamento`, `consultar_agendamento` e `cancelar_agendamento`.

A IA só *pede* as ferramentas. Quem executa é o servidor, com as mesmas regras da página (`AgendaService`),
e a barbearia vem da sessão do site, nunca do texto da conversa. Cada conversa aceita no máximo 40 mensagens.
O uso da API é cobrado por tokens e por tempo de sessão; acompanhe o custo no Console da Anthropic.

## Estrutura de pastas

```
database/barbearia.sql           criação do banco + dados de demonstração
src/java/br/com/barbearia/
  model/                         entidades e enums (Plano, StatusAgendamento)
  dao/                           acesso a dados (JDBC)
  service/                       regras de negócio
  ia/                            assistente com IA (Managed Agents) e as ferramentas que ele usa
  util/                          formatação, segurança, CSRF, mensagens
  web/                           filtros (login, UTF-8), listener de inicialização e ChatServlet
web/
  index.jsp                      página do SaaS (planos, barbearias)
  cadastro.jsp, login.jsp        onboarding e login do dono
  agendar.jsp                    fluxo de agendamento do cliente
  confirmacao.jsp, consultar.jsp confirmação, consulta e cancelamento
  admin/                         painel: agenda, serviços, barbeiros, relatórios, configurações, plano
  WEB-INF/jspf/                  trechos de layout reaproveitados (cabeçalho, menu, chat)
  css/, js/                      estilos e scripts
```

## Próximos passos

- Lembrete automático por WhatsApp/e-mail na véspera do horário.
- Levar o assistente para o WhatsApp da barbearia, usando as mesmas ferramentas.
- Cobrança real da assinatura com gateway de pagamento (cartão e Pix recorrente).
- Agenda própria por barbeiro (folgas, horário de almoço).
- Publicação em nuvem com domínio próprio.
