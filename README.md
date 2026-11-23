# BarberAgenda: agendamento online para barbearias (SaaS)

O BarberAgenda é um **SaaS** (software oferecido pela internet), **gratuito e com um único plano**, que
permite que barbearias recebam agendamentos de cortes de cabelo pela internet. Este repositório traz o **MVP**,
a primeira versão com as funcionalidades essenciais para demonstrar a proposta.

**O problema:** em muitas barbearias o agendamento ainda é feito por WhatsApp, ligação ou caderno.
O barbeiro precisa parar o atendimento para responder mensagens, acontecem horários marcados em dobro,
clientes faltam sem avisar e o dono não sabe quanto faturou no mês.

**A solução:** cada barbearia cadastrada ganha um link próprio de agendamento. O cliente escolhe o
serviço, o barbeiro, o dia e um horário que realmente está livre, sem precisar criar conta.
O dono acompanha tudo em um painel com a agenda do dia, controle de faltas e relatórios.

📄 Documentação completa do trabalho: [docs/BarberAgenda-Documentacao.pdf](docs/BarberAgenda-Documentacao.pdf)

## Integrantes

- Marco Aurélio
- Júlio Augusto
- André Zerger

## Público-alvo

- **Quem cadastra a barbearia:** donos de barbearias pequenas e médias.
- **Usuário final:** clientes da barbearia, que agendam pelo celular sem precisar criar conta.

## Funcionalidades

**Área pública (cliente da barbearia)**
- Página inicial com a lista de barbearias.
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
- **Relatórios do mês**: faturamento, ticket médio, taxa de faltas, faturamento por dia,
  serviços mais vendidos e desempenho por barbeiro.

## Como o sistema é oferecido

O BarberAgenda é **gratuito** e tem **um único plano**, igual para todas as barbearias:

- **Para a barbearia:** R$ 0. Não tem mensalidade, cartão ou limite de barbeiros, e tudo vem incluso
  (link de agendamento, agenda, assistente com IA e relatórios).
- **Para o cliente:** agenda de graça, sem criar conta, e paga o corte na barbearia.

**Possibilidade de cobrança no futuro:** se for preciso cobrir os custos (servidor, banco e uso da API
de IA), a ideia é manter o plano único e o cadastro grátis e cobrar uma taxa pequena por agendamento concluído,
por exemplo R$ 0,50, paga por Pix no fim do mês. No MVP, nenhuma cobrança é feita.

## Tecnologias

- **Java 21** com **JSP** e Servlets (Jakarta EE 10): filtros e *listener*
- **Apache Tomcat 10.1**
- **MySQL 8** com JDBC (driver MySQL Connector/J, incluído em `web/WEB-INF/lib`)
- **OpenRouter** (API no formato Chat Completions), chamado com o `HttpClient` do próprio Java e Jackson.
  O assistente usa o modelo Claude Haiku 5.5 (`anthropic/claude-haiku-5.5`), o mais barato da Anthropic.
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
   service/  regras de negócio (horários livres, validações)
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

O script cria o banco `barbearia` com as tabelas **vazias**. A primeira barbearia é cadastrada pelo próprio
site, em **Sou barbeiro → Cadastre sua barbearia**.

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
Crie uma chave em [openrouter.ai/settings/keys](https://openrouter.ai/settings/keys). Depois copie o modelo
e cole a chave:

```bash
cp web/WEB-INF/ia.properties.example web/WEB-INF/ia.properties
```

```properties
openrouter.api_key=sua_chave
```

O `ia.properties` também está no `.gitignore`. Sem ele, o site funciona normalmente e o chat mostra um aviso.
Para trocar de modelo, mude `openrouter.modelo` (ou a variável `OPENROUTER_MODEL` na nuvem).

### 5. Rodar
No NetBeans: **File → Open Project**, escolha esta pasta, depois **Clean and Build** e **Run**.
Se o NetBeans avisar que falta o servidor (*Resolve Missing Server Problem*), selecione o Tomcat 10.1.

Outra opção é copiar o `dist/SistemaBarbearia.war` gerado pelo *Clean and Build* para a pasta
`webapps` do Tomcat.

Acesse: **http://localhost:8080/SistemaBarbearia/**

### Dados de demonstração (opcional)

Para testar sem cadastrar nada, rode também `database/dados-demonstracao.sql` **depois** do `barbearia.sql`.
Ele cria duas barbearias fictícias, um mês de histórico e agendamentos para hoje e amanhã.
Os logins de teste estão no começo do arquivo. Para começar do zero, não rode esse arquivo.

### Publicar na nuvem (AWS)

O projeto tem `Dockerfile` e `docker-compose.yml` (site + MySQL + Caddy para HTTPS). Na nuvem, o banco é configurado pelas variáveis
`DB_URL`, `DB_USUARIO` e `DB_SENHA` e a IA por `OPENROUTER_API_KEY`, todas no arquivo `.env` do servidor.
O sistema está no ar em **https://meubarberagenda.duckdns.org**. Servidor, custos e como publicar versões novas (`bash deploy/atualizar.sh`): [deploy/DEPLOY-AWS.md](deploy/DEPLOY-AWS.md).

## Assistente com IA

O chat da página de agendamento conversa com o Claude Haiku 5.5 pelo **OpenRouter** (formato Chat Completions).

1. O histórico da conversa fica na sessão do navegador (`HttpSession`), separado por barbearia.
2. A cada mensagem, o `ChatServlet` chama o `AssistenteIA`, que envia o histórico e as ferramentas para o modelo.
3. Quando a IA pede uma ferramenta (`tool_calls`), o servidor executa no MySQL, devolve o resultado
   (mensagem `role: "tool"`) e chama o modelo de novo, até ele responder com texto.

Ferramentas disponíveis (`FerramentasIA`): `ver_barbearia`, `listar_servicos`, `listar_barbeiros`,
`ver_horarios_livres`, `criar_agendamento`, `consultar_agendamento` e `cancelar_agendamento`.

A IA só *pede* as ferramentas. Quem executa é o servidor, com as mesmas regras da página (`AgendaService`),
e a barbearia vem da sessão do site, nunca do texto da conversa. Cada conversa aceita no máximo 40 mensagens.
O uso da API é cobrado por tokens; acompanhe o custo em [openrouter.ai/activity](https://openrouter.ai/activity).

## Estrutura de pastas

```
database/barbearia.sql           criação do banco (tabelas vazias)
database/dados-demonstracao.sql  dados fictícios para teste (opcional)
src/java/br/com/barbearia/
  model/                         entidades e o enum StatusAgendamento
  dao/                           acesso a dados (JDBC)
  service/                       regras de negócio
  ia/                            assistente com IA (OpenRouter) e as ferramentas que ele usa
  util/                          formatação, segurança, CSRF, mensagens
  web/                           filtros (login, UTF-8), listener de inicialização e ChatServlet
web/
  index.jsp                      página inicial (lista de barbearias)
  cadastro.jsp, login.jsp        onboarding e login do dono
  agendar.jsp                    fluxo de agendamento do cliente
  confirmacao.jsp, consultar.jsp confirmação, consulta e cancelamento
  admin/                         painel: agenda, serviços, barbeiros, relatórios, configurações
  WEB-INF/jspf/                  trechos de layout reaproveitados (cabeçalho, menu, chat)
  css/, js/                      estilos e scripts
```

## Próximos passos

- Lembrete automático por WhatsApp/e-mail na véspera do horário.
- Levar o assistente para o WhatsApp da barbearia, usando as mesmas ferramentas.
- Se for preciso cobrir os custos, testar a taxa por agendamento concluído.
- Agenda própria por barbeiro (folgas, horário de almoço).
- Publicação em nuvem com domínio próprio.
