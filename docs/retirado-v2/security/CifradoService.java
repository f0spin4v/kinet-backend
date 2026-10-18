package co.edu.uniquindio.kinet.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra con AES-GCM los tokens de los proveedores wearables antes de guardarlos.
 * La clave sale de kinet.cifrado.clave (se deriva a 256 bits con SHA-256).
 */
@Component
public class CifradoService {

    private static final int LONGITUD_IV = 12;
    private static final int BITS_ETIQUETA = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKeySpec clave;

    public CifradoService(@Value("${kinet.cifrado.clave}") String secreto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(secreto.getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(bytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo preparar la clave de cifrado", e);
        }
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[LONGITUD_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
                    .put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el token", e);
        }
    }

    public String descifrar(String textoCifrado) {
        try {
            ByteBuffer datos = ByteBuffer.wrap(Base64.getDecoder().decode(textoCifrado));
            byte[] iv = new byte[LONGITUD_IV];
            datos.get(iv);
            byte[] cifrado = new byte[datos.remaining()];
            datos.get(cifrado);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            return new String(cifrador.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el token", e);
        }
    }
}
