package org.acme.notification.infrastructure.push;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/** La librería de Web Push cifra y firma con BouncyCastle: arma un envío completo sin salir a la red. */
class WebPushEncryptionTest {

    @Test
    void armaElEnvioCifradoYFirmadoConClavesVapid() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPair vapid = p256();
        KeyPair browser = p256();
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);

        PushService service = new PushService(publicKey(vapid), privateKey(vapid), "https://asistente.example");
        Notification notification = new Notification("https://fcm.googleapis.com/fcm/send/abc", publicKey(browser), b64(auth),
                "{\"title\":\"Gas casa\"}".getBytes(StandardCharsets.UTF_8));
        HttpPost post = service.preparePost(notification, nl.martijndwars.webpush.Encoding.AES128GCM);

        assertTrue(post.getFirstHeader("Authorization").getValue().startsWith("vapid t="));
        assertEquals("aes128gcm", post.getFirstHeader("Content-Encoding").getValue());
        assertTrue(post.getEntity().getContentLength() > 0);
    }

    private static KeyPair p256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    /** Punto sin comprimir (0x04 + X + Y), como lo da el navegador y npx web-push. */
    private static String publicKey(KeyPair pair) {
        ECPublicKey key = (ECPublicKey) pair.getPublic();
        byte[] out = new byte[65];
        out[0] = 4;
        System.arraycopy(unsigned(key.getW().getAffineX()), 0, out, 1, 32);
        System.arraycopy(unsigned(key.getW().getAffineY()), 0, out, 33, 32);
        return b64(out);
    }

    private static String privateKey(KeyPair pair) {
        return b64(unsigned(((ECPrivateKey) pair.getPrivate()).getS()));
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] out = new byte[32];
        int from = Math.max(0, raw.length - 32);
        System.arraycopy(raw, from, out, 32 - (raw.length - from), raw.length - from);
        return out;
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
