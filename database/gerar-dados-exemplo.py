#!/usr/bin/env python3
"""
Gera dados FICTICIOS e realistas para o BarberAgenda, como se o site ja tivesse varios meses de uso:
barbearias em cidades diferentes, donos com login, barbeiros (alguns desativados), servicos,
clientes que voltam e milhares de agendamentos (concluidos, faltas, cancelamentos e os proximos dias).

Os agendamentos respeitam as mesmas regras do sistema: dias e horario de funcionamento, intervalo
da agenda, duracao do servico e nenhum horario sobreposto para o mesmo barbeiro.

Uso (gera o SQL na saida padrao e as senhas dos donos em um arquivo LOCAL, fora do Git):
    python3 database/gerar-dados-exemplo.py --logins database/logins-exemplo.local.txt > dados.sql
    mysql -u root -p barbearia < dados.sql

Rode em um banco recem-criado (barbearia.sql) ou sem barbearias: os IDs comecam em 1.
Telefones usam o prefixo 9 0000 (nao atribuido a celulares) e e-mails usam example.com (reservado).
"""
import argparse
import base64
import hashlib
import os
import random
import secrets
import string
import sys
from datetime import date, datetime, time, timedelta
from zoneinfo import ZoneInfo

FUSO = ZoneInfo("America/Sao_Paulo")
ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
ITERACOES = 120_000

NOMES = ("João Pedro Lucas Mateus Gabriel Pedro Thiago Felipe André Rodrigo Vinícius Gustavo Leonardo Rafael "
         "Bruno Diego Eduardo Henrique Caio Daniel Marcelo Fernando Ricardo Paulo Igor Murilo Otávio Renan "
         "Samuel Vitor Arthur Bernardo Heitor Davi Miguel Enzo Nicolas Kauã Luan Wesley Anderson Fábio Júlio "
         "Márcio Alexandre Leandro Rogério Sérgio Cláudio Douglas Everton Jefferson Robson Willian Yuri Ícaro "
         "Breno Cauã Erick Pietro Lorenzo Benício Theo Joaquim Augusto Antônio Carlos José Luiz Fabrício").split()
SOBRENOMES = ("Silva Santos Oliveira Souza Rodrigues Ferreira Alves Pereira Lima Gomes Costa Ribeiro Martins "
              "Carvalho Almeida Lopes Soares Fernandes Vieira Barbosa Rocha Dias Nascimento Andrade Moreira Nunes "
              "Marques Machado Mendes Freitas Cardoso Ramos Gonçalves Santana Teixeira Araújo Pinto Correia "
              "Moura Cavalcanti Batista Campos Duarte Monteiro Rezende Prado Toledo Bueno Franco Siqueira").split()

# nome, slug, cidade, DDD, abre, fecha, intervalo, dias (1=seg .. 7=dom), movimento (0-1), barbeiros
BARBEARIAS = [
    ("Navalha de Ouro Barbearia", "navalha-de-ouro", "Rua Paraná, 812 - Centro, Jacarezinho/PR", "43",
     time(9), time(20), 30, [1, 2, 3, 4, 5, 6], 0.80, 4),
    ("Dom Bigode Barbearia", "dom-bigode", "Av. Higienópolis, 1450 - Centro, Londrina/PR", "43",
     time(9), time(21), 30, [1, 2, 3, 4, 5, 6], 0.85, 5),
    ("Corte Fino Barber Shop", "corte-fino", "Rua Antônio Carlos Mori, 230 - Vila Mano, Ourinhos/SP", "14",
     time(10), time(20), 30, [2, 3, 4, 5, 6], 0.65, 3),
    ("Barbearia Vintage 43", "vintage-43", "Rua Rui Barbosa, 95 - Centro, Santo Antônio da Platina/PR", "43",
     time(8), time(19), 30, [1, 2, 3, 4, 5, 6], 0.55, 2),
    ("Tesoura Afiada", "tesoura-afiada", "Av. Minas Gerais, 640 - Centro, Cornélio Procópio/PR", "43",
     time(9), time(19), 20, [1, 2, 3, 4, 5, 6], 0.60, 3),
    ("Barbearia do Zé", "barbearia-do-ze", "Rua Frei Rafael Proner, 58 - Centro, Bandeirantes/PR", "43",
     time(8), time(18), 30, [1, 2, 3, 4, 5, 6], 0.50, 2),
    ("Black Beard Barbearia", "black-beard", "Av. Brasil, 3020 - Zona 1, Maringá/PR", "44",
     time(10), time(22), 30, [2, 3, 4, 5, 6, 7], 0.75, 4),
    ("Studio Barba Negra", "barba-negra", "Rua Comendador Araújo, 410 - Batel, Curitiba/PR", "41",
     time(9), time(21), 15, [1, 2, 3, 4, 5, 6], 0.80, 5),
    ("Barbearia Central", "barbearia-central", "Praça Getúlio Vargas, 12 - Centro, Siqueira Campos/PR", "43",
     time(8), time(18), 30, [1, 2, 3, 4, 5, 6], 0.45, 2),
    ("Old School Barber", "old-school", "Rua Jorge Velho, 300 - Centro, Andirá/PR", "43",
     time(9), time(20), 30, [2, 3, 4, 5, 6], 0.60, 3),
]

# nome, descricao, duracao, preco base, peso (popularidade)
CATALOGO = [
    ("Corte masculino", "Tesoura ou máquina, com lavagem e finalização", 30, 40, 30),
    ("Barba completa", "Toalha quente, navalha e hidratação", 30, 30, 14),
    ("Corte + barba", "O combo mais pedido da casa", 60, 65, 22),
    ("Degradê / fade", "Degradê na navalha com acabamento", 45, 45, 14),
    ("Pezinho / acabamento", "Contorno entre um corte e outro", 15, 15, 6),
    ("Sobrancelha", "Design na navalha", 15, 15, 4),
    ("Corte infantil", "Para crianças até 12 anos", 30, 30, 5),
    ("Pigmentação de barba", "Disfarça falhas na barba", 30, 35, 2),
    ("Platinado / luzes", "Descoloração com tonalização", 120, 150, 1),
    ("Hidratação capilar", "Tratamento com máscara e vapor", 30, 35, 2),
]

STATUS_PASSADO = [("CONCLUIDO", 86), ("FALTOU", 5), ("CANCELADO", 9)]
STATUS_FUTURO = [("AGENDADO", 93), ("CANCELADO", 7)]
OCUPACAO_DIA = {1: 0.40, 2: 0.50, 3: 0.55, 4: 0.60, 5: 0.75, 6: 0.90, 7: 0.45}


def hash_senha(senha):
    salt = os.urandom(16)
    h = hashlib.pbkdf2_hmac("sha256", senha.encode(), salt, ITERACOES, 32)
    return f"{ITERACOES}:{base64.b64encode(salt).decode()}:{base64.b64encode(h).decode()}"


def sql(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def escolher(rng, pares):
    return rng.choices([p[0] for p in pares], weights=[p[1] for p in pares])[0]


def minutos(t):
    return t.hour * 60 + t.minute


def hhmm(m):
    return f"{m // 60:02d}:{m % 60:02d}:00"


class Gerador:
    def __init__(self, rng, hoje, agora, dias_passado, dias_futuro):
        self.rng, self.hoje, self.agora = rng, hoje, agora
        self.dias_passado, self.dias_futuro = dias_passado, dias_futuro
        self.codigos = set()
        self.telefones = set()
        self.linhas = {"barbearia": [], "usuario": [], "barbeiro": [], "servico": [], "agendamento": []}
        self.ids = {"barbeiro": 0, "servico": 0}
        self.logins = []

    def codigo(self):
        while True:
            c = "".join(self.rng.choice(ALFABETO_CODIGO) for _ in range(6))
            if c not in self.codigos:
                self.codigos.add(c)
                return c

    def nome(self):
        return f"{self.rng.choice(NOMES)} {self.rng.choice(SOBRENOMES)}"

    def telefone(self, ddd):
        # 9 000x-xxxx: faixa nao atribuida a celulares; sem repetir numero
        while True:
            t = f"{ddd}9000{self.rng.randint(0, 99999):05d}"
            if t not in self.telefones:
                self.telefones.add(t)
                return t

    def barbearia(self, bid, dados, senha):
        rng = self.rng
        nome, slug, endereco, ddd, abre, fecha, intervalo, dias, movimento, n_barbeiros = dados
        criada = datetime.combine(self.hoje - timedelta(days=rng.randint(self.dias_passado + 10, 330)),
                                  time(rng.randint(8, 22), rng.randint(0, 59)))
        self.linhas["barbearia"].append((bid, nome, slug, self.telefone(ddd), endereco, abre.strftime("%H:%M:00"),
                                         fecha.strftime("%H:%M:00"), intervalo,
                                         ",".join(map(str, dias)), criada.strftime("%Y-%m-%d %H:%M:%S")))
        dono = self.nome()
        email = f"dono.{slug}@example.com"
        self.linhas["usuario"].append((bid, dono, email, hash_senha(senha), criada.strftime("%Y-%m-%d %H:%M:%S")))
        self.logins.append((nome, email, f"agendar.jsp?b={slug}"))

        # Barbeiros: o dono corta, os outros sao funcionarios; as maiores tem um ex-barbeiro desativado
        barbeiros = []
        nomes_usados = {dono}
        for i in range(n_barbeiros):
            nb = dono if i == 0 else self.nome()
            while i > 0 and nb in nomes_usados:
                nb = self.nome()
            nomes_usados.add(nb)
            self.ids["barbeiro"] += 1
            barbeiros.append({"id": self.ids["barbeiro"], "nome": nb, "ativo": True, "saiu": None,
                              "ritmo": rng.uniform(0.85, 1.1)})
        if n_barbeiros >= 4:
            ex = barbeiros[-1]
            ex["ativo"] = False
            ex["saiu"] = self.hoje - timedelta(days=rng.randint(20, 60))
        for b in barbeiros:
            self.linhas["barbeiro"].append((b["id"], bid, b["nome"], 1 if b["ativo"] else 0))

        # Servicos: os 4 basicos sempre, mais alguns extras; preco ajustado ao "nivel" da barbearia
        nivel = rng.uniform(0.85, 1.35)
        escolhidos = CATALOGO[:4] + rng.sample(CATALOGO[4:], rng.randint(1, 4))
        servicos = []
        for nome_s, desc, dur, preco, peso in escolhidos:
            self.ids["servico"] += 1
            valor = max(10, round(preco * nivel / 5) * 5)
            servicos.append({"id": self.ids["servico"], "duracao": dur, "preco": valor, "peso": peso, "ativo": True})
            self.linhas["servico"].append([self.ids["servico"], bid, nome_s, desc, dur, f"{valor:.2f}", 1])
        if len(servicos) > 5 and rng.random() < 0.5:
            servicos[-1]["ativo"] = False
            self.linhas["servico"][-1][6] = 0

        # Clientes da barbearia: os fieis voltam a cada 2-3 semanas, a maioria vem de vez em quando
        n_clientes = int(n_barbeiros * movimento * 260)
        clientes = []
        for _ in range(n_clientes):
            frequencia = rng.choices([4.0, 2.0, 1.0, 0.4], weights=[10, 25, 40, 25])[0]
            clientes.append((self.nome(), self.telefone(ddd), frequencia * rng.uniform(0.7, 1.3)))
        pesos_clientes = [c[2] for c in clientes]
        pesos_servicos = [s["peso"] for s in servicos]

        inicio = self.hoje - timedelta(days=self.dias_passado)
        abre_m, fecha_m = minutos(abre), minutos(fecha)
        agora_m = self.agora.hour * 60 + self.agora.minute
        for d in range(self.dias_passado + self.dias_futuro + 1):
            dia = inicio + timedelta(days=d)
            if dia.isoweekday() not in dias:
                continue
            futuro = dia > self.hoje
            # mais longe no futuro, menos horarios ja marcados
            fator_futuro = max(0.15, 1 - (dia - self.hoje).days / 16) if futuro else 1.0
            for b in barbeiros:
                if b["saiu"] and dia >= b["saiu"]:
                    continue
                ocup = min(0.97, OCUPACAO_DIA[dia.isoweekday()] * movimento * b["ritmo"] * fator_futuro * 1.15)
                almoco = rng.choice([12 * 60, 12 * 60 + 30, 13 * 60])
                m = abre_m
                while m < fecha_m:
                    if almoco <= m < almoco + 60:
                        m = almoco + 60
                        continue
                    if rng.random() >= ocup:
                        m += intervalo
                        continue
                    ativos = [s for s in servicos if s["ativo"] or not futuro]
                    s = rng.choices(ativos, weights=[x["peso"] for x in ativos])[0]
                    fim = m + s["duracao"]
                    if fim > fecha_m:
                        m += intervalo
                        continue
                    cliente = rng.choices(clientes, weights=pesos_clientes)[0]
                    if futuro or (dia == self.hoje and m >= agora_m):
                        status = escolher(rng, STATUS_FUTURO)
                    else:
                        status = escolher(rng, STATUS_PASSADO)
                    criado = datetime.combine(dia, time()) + timedelta(minutes=m) \
                        - timedelta(days=rng.choice([0, 0, 1, 1, 2, 3, 5, 7, 10]), minutes=rng.randint(30, 600))
                    if criado > self.agora:
                        criado = self.agora - timedelta(minutes=rng.randint(5, 600))
                    self.linhas["agendamento"].append((bid, b["id"], s["id"], cliente[0], cliente[1], dia.isoformat(),
                                                       hhmm(m), hhmm(fim), status, f"{s['preco']:.2f}",
                                                       self.codigo(), criado.strftime("%Y-%m-%d %H:%M:%S")))
                    # o proximo horario comeca no primeiro "degrau" da agenda depois do fim deste
                    passos = -(-(fim - abre_m) // intervalo)
                    m = abre_m + passos * intervalo

    def escrever(self, saida):
        colunas = {
            "barbearia": "id, nome, slug, telefone, endereco, hora_abertura, hora_fechamento, intervalo_min, "
                         "dias_funcionamento, criado_em",
            "usuario": "barbearia_id, nome, email, senha_hash, criado_em",
            "barbeiro": "id, barbearia_id, nome, ativo",
            "servico": "id, barbearia_id, nome, descricao, duracao_min, preco, ativo",
            "agendamento": "barbearia_id, barbeiro_id, servico_id, cliente_nome, cliente_telefone, data, hora_inicio, "
                           "hora_fim, status, valor, codigo, criado_em",
        }
        saida.write("-- Dados ficticios gerados por database/gerar-dados-exemplo.py em "
                    f"{self.agora:%Y-%m-%d %H:%M} (horario de Brasilia)\n")
        saida.write("SET NAMES utf8mb4;\nSTART TRANSACTION;\n")
        for tabela in ("barbearia", "usuario", "barbeiro", "servico", "agendamento"):
            linhas = self.linhas[tabela]
            for i in range(0, len(linhas), 400):
                bloco = ",\n".join("(" + ", ".join(sql(v) for v in l) + ")" for l in linhas[i:i + 400])
                saida.write(f"INSERT INTO {tabela} ({colunas[tabela]}) VALUES\n{bloco};\n")
        saida.write("COMMIT;\n")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--logins", required=True, help="arquivo LOCAL onde salvar e-mails e a senha dos donos")
    p.add_argument("--dias-passado", type=int, default=120)
    p.add_argument("--dias-futuro", type=int, default=14)
    p.add_argument("--semente", type=int, default=None, help="para gerar sempre os mesmos dados")
    a = p.parse_args()

    rng = random.Random(a.semente)
    agora = datetime.now(FUSO).replace(tzinfo=None, second=0, microsecond=0)
    senha = "".join(secrets.choice(string.ascii_letters + string.digits) for _ in range(14))
    g = Gerador(rng, agora.date(), agora, a.dias_passado, a.dias_futuro)
    for i, dados in enumerate(BARBEARIAS, start=1):
        g.barbearia(i, dados, senha)
    g.escrever(sys.stdout)

    with open(os.open(a.logins, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "w", encoding="utf-8") as f:
        f.write("Logins das barbearias ficticias (NAO versionar). Mesma senha para todas:\n")
        f.write(f"Senha: {senha}\n\n")
        for nome, email, link in g.logins:
            f.write(f"{nome}\n  e-mail: {email}\n  pagina: /{link}\n")
    total = {k: len(v) for k, v in g.linhas.items()}
    print(f"-- totais: {total}", file=sys.stderr)


if __name__ == "__main__":
    main()
