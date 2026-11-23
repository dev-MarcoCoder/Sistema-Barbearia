package br.com.barbearia.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Funcoes de seguranca:
 * - hash de senha com PBKDF2 + salt aleatorio (a senha nunca e salva em texto puro);
 * - geracao de codigos aleatorios para agendamentos e tokens CSRF.
 */
public final class Seguranca {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_HASH = 256;
    /** Sem 0/O e 1/I/L para o cliente nao confundir ao digitar o codigo. */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private Seguranca() {
    }

    /** Gera o texto salvo no banco no formato iteracoes:salt:hash (Base64). */
    public static String gerarHashSenha(String senha) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(senha, salt, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return ITERACOES + ":" + b64.encodeToString(salt) + ":" + b64.encodeToString(hash);
    }

    public static boolean conferirSenha(String senha, String hashSalvo) {
        if (senha == null || hashSalvo == null) {
            return false;
        }
        String[] partes = hashSalvo.split(":");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[0]);
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            byte[] calculado = pbkdf2(senha, salt, iteracoes);
            // comparacao em tempo constante
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String senha, byte[] salt, int iteracoes) {
        try {
            KeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, TAMANHO_HASH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel gerar o hash da senha", e);
        }
    }

    public static String gerarCodigo(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
        }
        return sb.toString();
    }

    public static String gerarToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Utilitario de linha de comando para gerar o hash de uma senha (usado no script SQL de demonstracao). */
    public static void main(String[] args) {
        for (String senha : args) {
            System.out.println(gerarHashSenha(senha));
        }
    }
}
