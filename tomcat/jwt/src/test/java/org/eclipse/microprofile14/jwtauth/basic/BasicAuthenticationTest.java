/** Copyright Payara Services Limited **/
package org.eclipse.microprofile14.jwtauth.basic;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.vendoree.JwtTokenGenerator.generateJWTString;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile14.jwtauth.basic.ApplicationInit;
import org.eclipse.microprofile14.jwtauth.basic.Servlet;
import org.glassfish.soteria.cdi.CdiUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Arjan Tijms
 */
@RunWith(Arquillian.class)
public class BasicAuthenticationTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive =
            create(WebArchive.class)
                .addClasses(
                    ApplicationInit.class,
                    Servlet.class,
                    CdiUtils.class
                ).addAsResource(
                    // Main Properties file configuring that "org.eclipse.microprofile12" is the valid issuer
                    // and META-INF/public-key.pem is the public key
                    "META-INF/microprofile-config.properties"
                ).addAsResource(
                    // Public key to verify the incoming signed JWT's signature
                    "META-INF/public-key.pem"
                ).addAsManifestResource(new File("src/main/webapp/META-INF/context.xml"))
                ;

        System.out.println("************************************************************");
        System.out.println(archive.toString(true));
        System.out.println("************************************************************");
        
        System.out.println("War file content: \n" + archive.toString(true));

        return archive;

    }

    @Test
    @RunAsClient
    public void testProtectedPageNotLoggedin() throws IOException {

        Response response =
                newClient()
                     .target(
                         URI.create(new URL(base, "servlet").toExternalForm()))
                     .request(TEXT_PLAIN)
                     .get();

        // Not logged-in thus should not be accessible.
        assertFalse(
            "Not authenticated, so should not have been able to access protected resource",
            response.readEntity(String.class).contains("This is a protected servlet")
        );
    }

    @Test
    @RunAsClient
    public void testProtectedPageLoggedin() throws Exception {

        String response =
                newClient()
                     .target(
                         URI.create(new URL(base, "servlet").toExternalForm()))
                     .request(TEXT_PLAIN)
                     .header(AUTHORIZATION, "Bearer " + generateJWTString("jwt-token.json"))
                     .get(String.class);

        // Now has to be logged-in so page is accessible
        assertTrue(
            "Should have been authenticated, but could not access protected resource",
            response.contains("This is a protected servlet")
        );

        // Not only does the page needs to be accessible, the caller should have
        // the correct name and roles as well

        // Being able to access a page protected by a role but then seeing the un-authenticated
        // (anonymous) user would normally be impossible, but could happen if the authorization
        // system checks roles on the authenticated subject, but does not correctly expose
        // or propagate these to the HttpServletRequest
        assertFalse(
            "Protected resource could be accessed, but the user appears to be the unauthenticated user. " +
            "This should not be possible",
            response.contains("web username: null")
        );

        // An authenticated user should have the exact name "test" and nothing else.
        assertTrue(
            "Protected resource could be accessed, but the username is not correct.",
            response.contains("web username: test")
        );

        // Being able to access a page protected by role "architect" but failing
        // the test for this role would normally be impossible, but could happen if the
        // authorization system checks roles on the authenticated subject, but does not
        // correctly expose or propagate these to the HttpServletRequest
        assertTrue(
            "Resource protected by role \"architect\" could be accessed, but user fails test for this role." +
            "This should not be possible",
            response.contains("web user has role \"architect\": true")
        );
    }

}
