# BarberAgenda na AWS

O sistema roda em um servidor **EC2** da AWS, com o site (Tomcat), o banco (MySQL) e o Caddy (HTTPS) em Docker na mesma máquina.

| Item | Valor |
|---|---|
| Endereço do site | **https://meubarberagenda.duckdns.org/** (HTTPS automático, certificado renovado sozinho pelo Caddy) |
| IP fixo (IP elástico) | `18.118.76.238` |
| Nome (DuckDNS) | conta grátis em [duckdns.org](https://www.duckdns.org) (login com o Google do Júlio), apontando para o IP fixo |
| Instância | `barberagenda` (`i-020c0e42cc26c91c1`), região Ohio (us-east-2) |
| Tamanho | t3.micro (2 vCPU, 1 GB de memória + 2 GB de swap), disco de 20 GB, Ubuntu 26.04 LTS |
| Custo | cerca de US$ 13/mês, pago com os créditos do plano gratuito (US$ 100, válidos até 23/05/2027) |
| Chave de acesso (SSH) | `~/.ssh/barberagenda_ed25519` no Mac do Júlio. **Não perca e não compartilhe.** |
| Senhas e chave da IA | arquivo `.env` (cópia local na pasta do projeto, fora do Git; original em `~/barberagenda/.env` no servidor) |
| IA | OpenRouter, modelo `anthropic/claude-haiku-5.5` |

> **Plano gratuito:** quando ele terminar (23/05/2027) ou o crédito acabar, a AWS pede para mudar para o
> plano pago. Sem isso, a conta é encerrada e o servidor é apagado. Faça um backup do banco antes dessa data.

## Publicar uma versão nova

No Mac, dentro da pasta do projeto:

```bash
bash deploy/atualizar.sh
```

O script empacota o projeto (sem senhas), envia, recompila e reinicia o site. Os dados do banco são mantidos.

## Comandos úteis

Entrar no servidor:

```bash
ssh -i ~/.ssh/barberagenda_ed25519 ubuntu@18.118.76.238
```

Dentro dele, na pasta `~/barberagenda`:

| O que fazer | Comando |
|---|---|
| Ver se está rodando | `sudo docker compose ps` |
| Ver erros do site | `sudo docker compose logs --tail 100 app` |
| Ver erros do HTTPS | `sudo docker compose logs --tail 50 caddy` |
| Reiniciar | `sudo docker compose restart` |
| Backup do banco | `sudo docker compose exec db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" barbearia' > backup-$(date +%F).sql` |

**Nunca rode `docker compose down -v`**: o `-v` apaga o banco de dados.

## Se o SSH parar de conectar

Por segurança, o acesso SSH só é liberado para o IP da internet de onde o servidor foi criado.
Se o seu IP mudar (troca de rede, roteador reiniciado), atualize a regra:
EC2 → Instâncias → `barberagenda` → aba **Segurança** → grupo `launch-wizard-1` → **Editar regras de entrada** →
na regra SSH, escolha **Meu IP** → **Salvar**. O site continua funcionando normalmente nesse meio tempo.

## Nome e HTTPS

O nome `meubarberagenda.duckdns.org` aponta para o IP fixo `18.118.76.238`. O IP elástico não muda, nem se a instância
for parada e iniciada, então não é preciso mexer no DuckDNS. O `.env` do servidor tem `DOMINIO=meubarberagenda.duckdns.org`,
e o Caddy usa esse valor para pedir e renovar o certificado HTTPS (Let's Encrypt).

## Próximos passos opcionais

- **Domínio próprio:** registrar um domínio no registro.br (ex.: `meubarberagenda.com.br`), criar um registro **A**
  apontando para `18.118.76.238` e trocar `DOMINIO` no `.env` do servidor. Depois rode `sudo docker compose up -d caddy`.
- **Limite de gasto da IA:** em [openrouter.ai/settings/keys](https://openrouter.ai/settings/keys), defina um *credit limit*
  na chave para que o chat nunca gaste mais do que o previsto.
