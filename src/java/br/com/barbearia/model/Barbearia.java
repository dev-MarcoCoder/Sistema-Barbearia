package br.com.barbearia.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

/**
 * Barbearia que usa o sistema (o "inquilino"). Todos os outros dados
 * (barbeiros, servicos, agendamentos) pertencem a uma barbearia.
 * O uso e gratuito: nao ha planos nem limites por barbearia.
 */
public class Barbearia {

    private int id;
    private String nome;
    private String slug;
    private String telefone;
    private String endereco;
    private LocalTime horaAbertura = LocalTime.of(9, 0);
    private LocalTime horaFechamento = LocalTime.of(19, 0);
    private int intervaloMin = 30;
    /** Dias de funcionamento no padrao ISO: 1 = segunda ... 7 = domingo. */
    private Set<Integer> diasFuncionamento = new TreeSet<>(Set.of(1, 2, 3, 4, 5, 6));

    public boolean abreNo(DayOfWeek dia) {
        return diasFuncionamento.contains(dia.getValue());
    }

    /** Converte o texto salvo no banco ("1,2,3") para o conjunto de dias. */
    public void setDiasFuncionamentoTexto(String texto) {
        Set<Integer> dias = new TreeSet<>();
        if (texto != null) {
            for (String parte : texto.split(",")) {
                parte = parte.trim();
                if (!parte.isEmpty()) {
                    int dia = Integer.parseInt(parte);
                    if (dia >= 1 && dia <= 7) {
                        dias.add(dia);
                    }
                }
            }
        }
        this.diasFuncionamento = dias;
    }

    public String getDiasFuncionamentoTexto() {
        StringBuilder sb = new StringBuilder();
        for (Integer dia : diasFuncionamento) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(dia);
        }
        return sb.toString();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public LocalTime getHoraAbertura() { return horaAbertura; }
    public void setHoraAbertura(LocalTime horaAbertura) { this.horaAbertura = horaAbertura; }
    public LocalTime getHoraFechamento() { return horaFechamento; }
    public void setHoraFechamento(LocalTime horaFechamento) { this.horaFechamento = horaFechamento; }
    public int getIntervaloMin() { return intervaloMin; }
    public void setIntervaloMin(int intervaloMin) { this.intervaloMin = intervaloMin; }
    public Set<Integer> getDiasFuncionamento() { return diasFuncionamento; }
    public void setDiasFuncionamento(Set<Integer> dias) { this.diasFuncionamento = new TreeSet<>(dias); }
}
