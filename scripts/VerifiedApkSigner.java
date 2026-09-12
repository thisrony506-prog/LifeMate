import com.android.apksig.ApkVerifier;
import java.io.File;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Use Android's verified certificate objects, not unstable human-readable CLI labels. */
class VerifiedApkSigner {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected one APK path");
        var result = new ApkVerifier.Builder(new File(args[0])).setMinCheckedPlatformVersion(26).build().verify();
        if (!result.isVerified()) throw new SecurityException("Android apksig rejected the APK signature");
        var signers = result.getSignerCertificates();
        if (signers.size() != 1) throw new SecurityException("Exactly one verified APK signer is required");
        var sha = MessageDigest.getInstance("SHA-256").digest(signers.get(0).getEncoded());
        System.out.println(HexFormat.of().formatHex(sha));
    }
}
