package br.com.barbearia.util;

import java.text.Normalizer;

/** Gera o identificador da barbearia usado no link publico: "Barbearia do Zé" -> "barbearia-do-ze". */
public final class Slug {

    private Slug() {
    }

    public static String gerar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = semAcento.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > 60) {
            slug = slug.substring(0, 60).replaceAll("-+$", "");
        }
        return slug;
    }
}
