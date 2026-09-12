import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

/** JDK 17 source-file launcher. Writes only PUBLIC, signed update metadata. */
class SignReleaseMetadata {
    public static void main(String[] args) throws Exception {
        if (args.length != 6) throw new IllegalArgumentException("Expected APK, code, version, URL, output notes path, verified APK signer digest");
        var apk = Path.of(args[0]);
        int code = Integer.parseInt(args[1]);
        String version = args[2], url = args[3];
        if (!version.matches("1\\.2\\.[1-9][0-9]{0,6}") || code != 100000 + Integer.parseInt(version.substring(4)))
            throw new IllegalArgumentException("Noncanonical release version");
        String expected = "https://github.com/thisrony506-prog/LifeMate/releases/download/v" + version + "/LifeMate-" + code + ".apk";
        if (!url.equals(expected) || !apk.getFileName().toString().equals("LifeMate-" + code + ".apk"))
            throw new IllegalArgumentException("Noncanonical APK location");
        long size = Files.size(apk);
        if (size < 1 || size > 209715200L) throw new IllegalArgumentException("Unexpected APK size");
        char[] storePassword = Objects.requireNonNull(System.getenv("LIFEMATE_STORE_PASSWORD")).toCharArray();
        char[] keyPassword = Objects.requireNonNull(System.getenv("LIFEMATE_KEY_PASSWORD")).toCharArray();
        try {
            var store = KeyStore.getInstance("JKS");
            try (var input = Files.newInputStream(Path.of(System.getenv("LIFEMATE_KEYSTORE_PATH")))) { store.load(input, storePassword); }
            String alias = Objects.requireNonNull(System.getenv("LIFEMATE_KEY_ALIAS"));
            Certificate cert = store.getCertificate(alias);
            String signer = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            if (!signer.equals(args[5])) throw new SecurityException("Keystore signer differs from verified APK signer");
            var digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(apk)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            String sha = HexFormat.of().formatHex(digest.digest());
            String canonical = "LifeMate-Update-V1\n" + code + "\n" + version + "\n" + url + "\n" + size + "\n" + sha + "\n" + signer + "\n";
            byte[] payload = canonical.getBytes(StandardCharsets.UTF_8);
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign((PrivateKey) store.getKey(alias, keyPassword)); signature.update(payload);
            byte[] proof = signature.sign();
            signature.initVerify(cert.getPublicKey()); signature.update(payload);
            if (!signature.verify(proof)) throw new SecurityException("Metadata signature self-check failed");
            String json = "{\"code\":" + code + ",\"version\":\"" + version + "\",\"url\":\"" + url + "\",\"size\":" + size +
                ",\"sha256\":\"" + sha + "\",\"signer\":\"" + signer + "\",\"signature\":\"" + Base64.getEncoder().encodeToString(proof) + "\"}";
            Files.writeString(Path.of(args[4]), "Signed LifeMate " + version + " for Android 8.0+. Install over the official app to retain data. Android asks before updating.\n\n<!-- lifemate-update-v1\n" + json + "\n-->\n");
        } finally { Arrays.fill(storePassword, '\0'); Arrays.fill(keyPassword, '\0'); }
    }
}
