package com.lorman.ref.spring.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Minimal HS256 JWT generator for tests. Uses the same default secret as application.properties.
 */
public final class TestJwtUtil {

    private static final String DEFAULT_SECRET = "dev-test-secret-change-me-please-32b";

    private TestJwtUtil() {
    }

    public static String createToken(List<String> roles) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("test-suite")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("roles", roles)
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();
            SignedJWT jwt = new SignedJWT(header, claims);

            JWSSigner signer = new MACSigner(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JWT", e);
        }
    }
}
