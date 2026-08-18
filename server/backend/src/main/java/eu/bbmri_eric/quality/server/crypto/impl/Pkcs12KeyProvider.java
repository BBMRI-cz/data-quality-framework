package eu.bbmri_eric.quality.server.crypto.impl;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Loads a private/public key pair from a PKCS#12 keystore on the filesystem. */
@Component
class Pkcs12KeyProvider implements KeyProvider {

  private static final Logger log = LoggerFactory.getLogger(Pkcs12KeyProvider.class);

  private final Path keyPath;
  private final String password;
  private final String alias;

  private PrivateKey privateKey;
  private PublicKey publicKey;
  private String keyId;

  Pkcs12KeyProvider(
      @Value("${app.crypto.key-path:}") String keyPath,
      @Value("${app.crypto.key-password:}") String password,
      @Value("${app.crypto.key-alias:central-server-key}") String alias) {
    this.keyPath = keyPath == null || keyPath.isBlank() ? null : Path.of(keyPath.trim());
    this.password = password;
    this.alias = alias;
  }

  @PostConstruct
  void loadKeys() {
    if (keyPath == null) {
      log.warn("Crypto functionality is not set up: 'app.crypto.key-path' is not configured.");
      return;
    }
    try (InputStream input = Files.newInputStream(keyPath)) {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(input, password.toCharArray());

      Key key = keyStore.getKey(alias, password.toCharArray());
      if (!(key instanceof PrivateKey pk)) {
        throw new IllegalStateException(
            "Alias '%s' does not reference a private key".formatted(alias));
      }
      privateKey = pk;

      Certificate certificate = keyStore.getCertificate(alias);
      if (certificate == null) {
        throw new IllegalStateException("No certificate found for alias '%s'".formatted(alias));
      }
      publicKey = certificate.getPublicKey();
      keyId = alias;
    } catch (IOException
        | KeyStoreException
        | NoSuchAlgorithmException
        | CertificateException
        | UnrecoverableKeyException e) {
      throw new IllegalStateException("Failed to load key store from '%s'".formatted(keyPath), e);
    }
    log.info("Successfully loaded the keypair for crypto functionality");
  }

  @Override
  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  @Override
  public PublicKey getPublicKey() {
    if (publicKey == null) {
      throw new EntityNotFoundException("Cryptographic functionality is not setup");
    }
    return publicKey;
  }

  @Override
  public String getKeyId() {
    return keyId;
  }
}
