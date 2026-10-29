package hospedabem.controller;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash de senha PBKDF2-HMAC-SHA256 com salt aleatório (RNF-7). Não é reversível. */
public final class Senha {
    private static final int ITERACOES = 210_000;
    private static final int BYTES_SALT = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senha() {
    }

    public static String gerarHash(char[] senha) {
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACOES + "$" + b64.encodeToString(salt) + "$"
                + b64.encodeToString(derivar(senha, salt, ITERACOES));
    }

    public static boolean confere(char[] senha, String hashArmazenado) {
        String[] partes = hashArmazenado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) return false;
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] esperado = b64.decode(partes[3]);
        byte[] obtido = derivar(senha, b64.decode(partes[2]), Integer.parseInt(partes[1]));
        return MessageDigest.isEqual(esperado, obtido);
    }

    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível na JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Gera o hash de uma senha para scripts de carga: java Senha.java <senha>. */
    public static void main(String[] args) {
        System.out.println(gerarHash(args[0].toCharArray()));
    }
}
