package com.appdynamics.extensions.sslcertificate;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runs cmd/openssl.sh against a local TLS server and checks what the server actually observed:
 * the SNI sent by the client and the certificate it selected from it. The server presents a
 * 1-day certificate to a client that sends SNI "backend.localhost" and a default 90-day certificate otherwise.
 */
public class OpensslScriptSniTest {

    // *.localhost resolves to loopback without any DNS/hosts setup and is a DNS-formatted name,
    // so OpenSSL 1.1.1+ would derive an SNI from it when connecting.
    private static final String BACKEND_FQDN = "backend.localhost";
    private static final char[] PASSWORD = "changeit".toCharArray();
    private static final DateTimeFormatter OPENSSL_DATE =
            DateTimeFormatter.ofPattern("MMM d HH:mm:ss yyyy 'GMT'", Locale.US);

    private Path dir;
    private KeyStore keyStore;
    private SniServer server;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("sh not available", new File("/bin/sh").canExecute());
        Assume.assumeFalse("no openssl binary found", opensslBinaries().isEmpty());
        try {
            InetAddress.getByName(BACKEND_FQDN);
        } catch (UnknownHostException e) {
            Assume.assumeNoException(BACKEND_FQDN + " does not resolve", e);
        }
        dir = Files.createTempDirectory("sni-test");
        Path ks = dir.resolve("ks.p12");
        genKey(ks, "default", 90);
        genKey(ks, "sni", 1);
        keyStore = KeyStore.getInstance("PKCS12");
        try (java.io.InputStream in = Files.newInputStream(ks)) {
            keyStore.load(in, PASSWORD);
        }
        server = new SniServer(keyStore);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.close();
        }
        if (dir != null) {
            try (java.util.stream.Stream<Path> files = Files.walk(dir)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private void genKey(Path ks, String alias, int days) throws Exception {
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", alias, "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=" + alias, "-validity", String.valueOf(days), "-keystore", ks.toString(),
                "-storetype", "PKCS12", "-storepass", new String(PASSWORD)).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Assert.assertEquals(out, 0, p.waitFor());
    }

    /** Distinct openssl binaries to exercise: the script's default one and the first one on the PATH. */
    private static Set<String> opensslBinaries() {
        Set<String> bins = new LinkedHashSet<>();
        bins.add("/usr/bin/openssl");
        for (String d : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
            bins.add(new File(d, "openssl").getPath());
        }
        bins.removeIf(b -> !new File(b).canExecute());
        return bins;
    }

    private String notAfterOf(String alias) throws Exception {
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
        return LocalDateTime.ofInstant(cert.getNotAfter().toInstant(), ZoneOffset.UTC).toString();
    }

    /** Runs the real script for the given site config and returns the notAfter it reported (ISO local date-time). */
    private String runScript(String openssl, String domain, String targetHost) throws Exception {
        Map<String, Object> site = new HashMap<>();
        site.put("domain", domain);
        site.put("port", server.port());
        if (targetHost != null) {
            site.put("targetHost", targetHost);
        }
        String[] cmd = SslCertificateMonitor.buildCommand(new File("cmd/openssl.sh").getAbsolutePath(), site);
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.environment().put("OPENSSL_BIN", openssl);
        Process p = pb.start();
        p.getOutputStream().close();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        Assert.assertTrue("script timed out", p.waitFor(30, TimeUnit.SECONDS));
        Assert.assertTrue("unexpected output from " + openssl + ": " + out, out.startsWith("notAfter="));
        String date = out.substring("notAfter=".length()).replaceAll("\\s+", " ");
        return LocalDateTime.parse(date, OPENSSL_DATE).toString();
    }

    private void assertNoSni(String domain, String targetHost) throws Exception {
        for (String openssl : opensslBinaries()) {
            server.reset();
            String notAfter = runScript(openssl, domain, targetHost);
            Assert.assertEquals(openssl + ": client must not send SNI", Collections.emptyList(), server.observedSni());
            Assert.assertEquals(openssl + ": default certificate must be selected", notAfterOf("default"), notAfter);
        }
    }

    @Test
    public void ipDomainWithFqdnTargetHostSendsNoSni() throws Exception {
        assertNoSni("192.0.2.10", BACKEND_FQDN);
    }

    @Test
    public void bracketedIpv6DomainWithFqdnTargetHostSendsNoSni() throws Exception {
        assertNoSni("[2001:db8::1]", BACKEND_FQDN);
    }

    @Test
    public void unbracketedIpv6DomainWithFqdnTargetHostSendsNoSni() throws Exception {
        assertNoSni("2001:db8::1", BACKEND_FQDN);
    }

    @Test
    public void hostnameDomainWithFqdnTargetHostSendsDomainAsSni() throws Exception {
        for (String openssl : opensslBinaries()) {
            server.reset();
            String notAfter = runScript(openssl, "backend.localhost", "localhost");
            Assert.assertEquals(openssl, Collections.singletonList("backend.localhost"), server.observedSni());
            Assert.assertEquals(openssl, notAfterOf("sni"), notAfter);
        }
    }

    /** TLS server that records the SNI of every handshake and picks the certificate from it. */
    private static class SniServer implements AutoCloseable {
        private final SSLServerSocket socket;
        private final Set<String> sni = Collections.synchronizedSet(new LinkedHashSet<>());
        private final Thread acceptor;

        SniServer(KeyStore ks) throws Exception {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, PASSWORD);
            X509ExtendedKeyManager delegate = (X509ExtendedKeyManager) kmf.getKeyManagers()[0];
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[]{new SniKeyManager(delegate, sni)}, null, null);
            socket = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(0, 50, null);
            acceptor = new Thread(this::acceptLoop, "sni-test-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return socket.getLocalPort();
        }

        void reset() {
            sni.clear();
        }

        List<String> observedSni() {
            synchronized (sni) {
                return new ArrayList<>(sni);
            }
        }

        private void acceptLoop() {
            while (!socket.isClosed()) {
                try (SSLSocket s = (SSLSocket) socket.accept()) {
                    s.setSoTimeout(5000);
                    s.startHandshake();
                    s.getInputStream().read();
                } catch (Exception ignored) {
                    // client hang-ups are expected
                }
            }
        }

        @Override
        public void close() throws Exception {
            socket.close();
            acceptor.join(5000);
        }
    }

    private static class SniKeyManager extends X509ExtendedKeyManager {
        private final X509ExtendedKeyManager delegate;
        private final Set<String> sni;

        SniKeyManager(X509ExtendedKeyManager delegate, Set<String> sni) {
            this.delegate = delegate;
            this.sni = sni;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            String alias = "default";
            if (socket instanceof SSLSocket
                    && ((SSLSocket) socket).getHandshakeSession() instanceof ExtendedSSLSession) {
                ExtendedSSLSession session = (ExtendedSSLSession) ((SSLSocket) socket).getHandshakeSession();
                for (SNIServerName name : session.getRequestedServerNames()) {
                    if (name instanceof SNIHostName) {
                        String host = ((SNIHostName) name).getAsciiName();
                        sni.add(host);
                        if (host.equals("backend.localhost")) {
                            alias = "sni";
                        }
                    }
                }
            }
            return alias;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return delegate.getServerAliases(keyType, issuers);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return delegate.getCertificateChain(alias);
        }

        @Override
        public java.security.PrivateKey getPrivateKey(String alias) {
            return delegate.getPrivateKey(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return delegate.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return delegate.chooseClientAlias(keyType, issuers, socket);
        }
    }
}
