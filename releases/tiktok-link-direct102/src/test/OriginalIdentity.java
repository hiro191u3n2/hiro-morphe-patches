/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
import com.android.apksig.ApkVerifier;
import java.io.File;
import java.security.MessageDigest;

/** Independently verifies the original input's signature; never signs/distributes it. */
public final class OriginalIdentity {
    public static void main(String[] args) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(new File(args[0]))
            .setMinCheckedPlatformVersion(23).setMaxCheckedPlatformVersion(37).build().verify();
        if (!result.isVerified() || !result.getErrors().isEmpty()) throw new AssertionError("Original APK signature failed: " + result.getErrors());
        if (result.getSignerCertificates().size() != 1) throw new AssertionError("Unexpected signer count");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(result.getSignerCertificates().get(0).getEncoded());
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format("%02x", b & 255));
        if (!hex.toString().equals("9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba"))
            throw new AssertionError("Unexpected original APK signer " + hex);
        System.out.println("PASS OriginalIdentity verified=true signer_certificate_sha256=" + hex
            + " v1=" + result.isVerifiedUsingV1Scheme() + " v2=" + result.isVerifiedUsingV2Scheme() + " v3=" + result.isVerifiedUsingV3Scheme());
    }
}
