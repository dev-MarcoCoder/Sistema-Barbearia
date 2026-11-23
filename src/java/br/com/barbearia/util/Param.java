package br.com.barbearia.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Converte parametros do request (sempre String) para os tipos certos,
 * sem estourar excecao quando o valor vem vazio ou invalido.
 */
public final class Param {

    private Param() {
    }

    public static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static int inteiro(String valor, int padrao) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (RuntimeException e) {
            return padrao;
        }
    }

    public static LocalDate data(String valor) {
        try {
            return LocalDate.parse(valor.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static LocalTime hora(String valor) {
        try {
            return LocalTime.parse(valor.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static YearMonth mes(String valor) {
        try {
            return YearMonth.parse(valor.trim());
        } catch (NullPointerException | DateTimeParseException e) {
            return null;
        }
    }

    /** Aceita "45,90", "45.90" ou "R$ 45,90". */
    public static BigDecimal decimal(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.replace("R$", "").replace(" ", "").trim();
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
