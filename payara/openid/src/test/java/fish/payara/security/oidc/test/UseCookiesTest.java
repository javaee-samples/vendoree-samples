/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.oidc.test;

import com.gargoylesoftware.htmlunit.WebClient;
import static fish.payara.security.annotations.OpenIdAuthenticationDefinition.OPENID_MP_USE_SESSION;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.omnifaces.utils.security.Certificates.createTempJKSKeyStore;
import org.vendoree.ServerOperations;
import static org.omnifaces.utils.Lang.isEmpty;
import static org.omnifaces.utils.security.Certificates.createTempJKSKeyStore;
import static org.omnifaces.utils.security.Certificates.createTempJKSTrustStore;
import static org.omnifaces.utils.security.Certificates.generateRandomRSAKeys;
import static org.omnifaces.utils.security.Certificates.getCertificateChainFromServer;
import static org.vendoree.ServerOperations.addCertificateToContainerTrustStore;
import static org.vendoree.ServerOperations.addContainerSystemProperty;
import static org.vendoree.ServerOperations.createSelfSignedCertificate;
import static org.vendoree.ServerOperations.enableSSLDebug;
import static org.vendoree.ServerOperations.getHostFromCertificate;

/**
 *
 * @author Gaurav Gupta
 */
@RunWith(Arquillian.class)
public class UseCookiesTest {

    private WebClient webClient;

    @OperateOnDeployment("openid-client")
    @ArquillianResource
    private URL base;

    private URL baseHttps;

    private static String clientKeyStorePath;

    @Deployment(name = "openid-server")
    public static WebArchive createServerDeployment() {
        return OpenIdTestUtil.createServerDeployment();
    }

    @Deployment(name = "openid-client")
    public static WebArchive createClientDeployment() {
        System.out.println("\n*********** DEPLOYMENT START ***************************");

        Security.addProvider(new BouncyCastleProvider());

        // Enable to get detailed logging about the SSL handshake on the client
        // For an explanation of the TLS handshake see: https://tls.ulfheim.net
        if (System.getProperty("ssl.debug") != null) {
            enableSSLDebug();
        }

        System.out.println("################################################################");

        // ### Generate keys for the client, create a certificate, and add those to a new local key store
        // Generate a Private/Public key pair for the client
        KeyPair clientKeyPair = generateRandomRSAKeys();

        // Create a certificate containing the client public key and signed with the private key
        X509Certificate clientCertificate = createSelfSignedCertificate(clientKeyPair);

        // Create a new local key store containing the client private key and the certificate
        clientKeyStorePath = createTempJKSKeyStore(clientKeyPair.getPrivate(), clientCertificate);

        // Enable to get detailed logging about the SSL handshake on the server
        if (System.getProperty("ssl.debug") != null) {
            System.out.println("Setting server SSL debug on");
            addContainerSystemProperty("javax.net.debug", "ssl:handshake");
        }

        // Only test TLS v1.2 for now
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");

        // Add the client certificate that we just generated to the trust store of the server.
        // That way the server will trust our certificate.
        // Set the actual domain used with -Dpayara_domain=[domain name]
        addCertificateToContainerTrustStore(clientCertificate);

        StringAsset mpConfig = new StringAsset(OPENID_MP_USE_SESSION + "=" + Boolean.FALSE.toString());
        return OpenIdTestUtil
                .createClientDeployment()
                .addAsWebInfResource(mpConfig, "classes/META-INF/microprofile-config.properties");
    }

    @Before
    public void setup() throws FileNotFoundException, IOException {
        webClient = new WebClient();
        baseHttps = ServerOperations.toContainerHttps(base);
        if (baseHttps == null) {
            throw new IllegalStateException("No https URL could be created from " + base);
        }

        // ### Ask the server for its certificate and add that to a new local trust store
        // Server -> client : the trust store certificates are used to validate the certificate sent
        // by the server
        X509Certificate[] serverCertificateChain = getCertificateChainFromServer(baseHttps.getHost(), baseHttps.getPort());

        if (!isEmpty(serverCertificateChain)) {

            System.out.println("Obtained certificate from server. Storing it in client trust store");

            String trustStorePath = createTempJKSTrustStore(serverCertificateChain);

            System.out.println("Reading trust store from: " + trustStorePath);

            webClient.getOptions().setSSLTrustStore(new File(trustStorePath).toURI().toURL(), "changeit", "jks");

            // If the use.cnHost property is we try to extract the host from the server
            // certificate and use exactly that host for our requests.
            // This is needed if a server is listening to multiple host names, for instance
            // localhost and example.com. If the certificate is for example.com, we can't
            // localhost for the request, as that will not be accepted.
            if (System.getProperty("use.cnHost") != null) {
                System.out.println("use.cnHost set. Trying to grab CN from certificate and use as host for requests.");
                baseHttps = getHostFromCertificate(serverCertificateChain, baseHttps);
            }
        } else {
            System.out.println("Could not obtain certificates from server. Continuing without custom truststore");
        }

        System.out.println("Using client key store from: " + clientKeyStorePath);

        // Client -> Server : the key store's private keys and certificates are used to sign
        // and sent a reply to the server
        webClient.getOptions().setSSLClientCertificate(new File(clientKeyStorePath).toURI().toURL(), "changeit", "jks");

        System.out.println("*********** SETUP DONE ***************************\n");
    }

    @Test
    @RunAsClient
    public void testOpenIdConnect() throws IOException {
        OpenIdTestUtil.testOpenIdConnect(webClient, baseHttps);
    }

}
