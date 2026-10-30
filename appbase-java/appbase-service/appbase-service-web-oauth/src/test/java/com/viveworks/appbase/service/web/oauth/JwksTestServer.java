/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.service.web.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * A local, loopback-only JWKS endpoint for the validation engine tests. Serves a JWK set that a test may
 * replace at runtime (to exercise key rotation), backed by the JDK's built-in HTTPS server so no network,
 * container or extra dependency is needed offline.
 *
 * <p>Also mints the signing keys and RS256 tokens the tests validate, so a test reads as
 * {@code sign(key, claims)} against {@code new JwksTestServer(publicSet)}.
 *
 * <p>Published as a test-jar so the provider modules (N04-N07) reuse the identical offline fixture. It is
 * the intentional, public test-support seam over the validation engine: {@link #validator(OAuthConfiguration)}
 * hands back a {@link TokenValidator} wired to this fixture's loopback JWKS over the test SSLContext, so a
 * provider test never needs the engine's package-private resource-retriever constructor and can live in its
 * own package. That HTTPS/trust-injection seam stays encapsulated here, in the core module that owns it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class JwksTestServer implements AutoCloseable {

    private static final String PATH = "/jwks";

    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

    private final HttpsServer server;
    private final SSLContext sslContext;
    private volatile JWKSet published;

    public JwksTestServer(JWKSet initial) throws IOException, GeneralSecurityException {
        this.published = initial;
        this.sslContext = sslContext();
        this.server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        this.server.createContext(PATH, exchange -> {
            byte[] body = published.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        this.server.start();
    }

    /** Replaces the published key set — the provider rotating its keys. */
    void publish(JWKSet jwks) {
        this.published = jwks;
    }

    /** The URI the engine is configured with as its {@code jwksUri}. */
    public URI uri() {
        return URI.create("https://localhost:"
                + server.getAddress().getPort() + PATH);
    }

    /**
     * A {@link TokenValidator} wired to this fixture's loopback JWKS over the test SSLContext. Provider
     * modules validate a provider-shaped token through this instead of constructing the engine directly,
     * so the engine's package-private resource-retriever constructor stays encapsulated in this module.
     *
     * @param configuration the provider's {@link OAuthConfiguration} with its {@code jwksUri} pointed at
     *                       {@link #uri()}
     * @return a validator that verifies against this fixture's published key set
     */
    public TokenValidator validator(OAuthConfiguration configuration) {
        return new NimbusTokenValidator(configuration, new Sha256TokenKeyHasher(), resourceRetriever());
    }

    ResourceRetriever resourceRetriever() {
        return url -> {
            URLConnection connection = url.openConnection();
            ((javax.net.ssl.HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
            try (InputStream input = connection.getInputStream()) {
                return new Resource(new String(input.readAllBytes(), StandardCharsets.UTF_8), connection.getContentType());
            }
        };
    }

    private static SSLContext sslContext() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = JwksTestServer.class.getResourceAsStream("/jwks-test.p12")) {
            if (input == null) {
                throw new IOException("missing test TLS keystore");
            }
            keyStore.load(input, KEYSTORE_PASSWORD);
        }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, KEYSTORE_PASSWORD);
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(keyStore);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), trustManagers.getTrustManagers(), null);
        return context;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** Generates a fresh 2048-bit RSA signing key tagged with {@code kid}. */
    public static RSAKey rsaKey(String kid) throws JOSEException {
        return new RSAKeyGenerator(2048).keyID(kid).generate();
    }

    /** A JWK set exposing only the public halves of the given keys, as a provider would publish them. */
    public static JWKSet publicSet(RSAKey... keys) {
        RSAKey[] pub = new RSAKey[keys.length];
        for (int i = 0; i < keys.length; i++) {
            pub[i] = keys[i].toPublicJWK();
        }
        return new JWKSet(java.util.List.of((com.nimbusds.jose.jwk.JWK[]) pub));
    }

    /** Signs {@code claims} as an RS256 JWT with {@code key}, stamping the key's {@code kid} in the header. */
    public static String signRs256(RSAKey key, JWTClaimsSet claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
