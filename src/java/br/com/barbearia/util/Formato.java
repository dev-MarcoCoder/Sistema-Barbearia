package br.com.barbearia.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Formatacao de datas, horas, dinheiro e telefone no padrao brasileiro. */
public final class Formato {

    public static final Locale BRASIL = Locale.of("pt", "BR");

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", BRASIL);

    private Formato() {
    }

    public static String data(LocalDate data) {
        return data == null ? "" : data.format(DATA);
    }

    public static String dataCurta(LocalDate data) {
        return data == null ? "" : data.format(DATA_CURTA);
    }

    /** Ex.: "Segunda-feira, 23 de novembro" */
    public static String dataExtenso(LocalDate data) {
        if (data == null) {
            return "";
        }
        String dia = data.getDayOfWeek().getDisplayName(TextStyle.FULL, BRASIL);
        String mes = data.getMonth().getDisplayName(TextStyle.FULL, BRASIL);
        return Character.toUpperCase(dia.charAt(0)) + dia.substring(1) + ", " + data.getDayOfMonth() + " de " + mes;
    }

    /** Ex.: "seg" */
    public static String diaSemanaCurto(LocalDate data) {
        String texto = data.getDayOfWeek().getDisplayName(TextStyle.SHORT, BRASIL);
        return texto.replace(".", "");
    }

    public static String diaSemana(int diaIso) {
        return java.time.DayOfWeek.of(diaIso).getDisplayName(TextStyle.FULL, BRASIL);
    }

    public static String mesAno(LocalDate data) {
        String texto = data.format(MES_ANO);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "" : hora.format(HORA);
    }

    public static String moeda(BigDecimal valor) {
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(' ', ' ');
    }

    /** Valor para preencher um input (ex.: 45,00). */
    public static String decimal(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    public static String duracao(int minutos) {
        if (minutos < 60) {
            return minutos + " min";
        }
        int horas = minutos / 60;
        int resto = minutos % 60;
        return resto == 0 ? horas + "h" : horas + "h" + String.format("%02d", resto);
    }

    /** Formata apenas digitos: 11987654321 -> (11) 98765-4321 */
    /** "1 falta", "3 faltas": escolhe singular ou plural pela quantidade. */
    public static String plural(long quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }

    public static String telefone(String digitos) {
        if (digitos == null) {
            return "";
        }
        String d = digitos.replaceAll("\\D", "");
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        return digitos;
    }
}
