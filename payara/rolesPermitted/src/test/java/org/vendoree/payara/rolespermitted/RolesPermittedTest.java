/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.vendoree.payara.rolespermitted;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Susan Rai
 */
@RunWith(Arquillian.class)
public class RolesPermittedTest {

    private static final String WEBAPP_SOURCE = "src/main/webapp";
    private static final String USERNAME = "payara";
    private static final String PASSWORD_ADMIN = "fish";
    private static final String PASSWORD_USER = "user";

    private WebClient webClient;

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "rolesPermitted.war")
                .addPackage("org.vendoree.payara.rolespermitted")
                .addPackage("org.vendoree.payara.rolespermitted.rest")
                .addAsWebInfResource(new File(WEBAPP_SOURCE, "WEB-INF/web.xml"))
                .addAsWebInfResource(new File(WEBAPP_SOURCE, "WEB-INF/glassfish-web.xml"));
    }

    @Before
    public void setUp() throws InterruptedException {
        webClient = new WebClient();
        //prevent spurious 404 errors
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
    }

    @Test
    @RunAsClient
    public void testAuthenticationWithIncorrectUser() {
        WebResponse response = getResponse("/rest/test/admin?username=" + "wrongUser" + "&password=" + PASSWORD_ADMIN);
        System.out.println("sdad = " + response.getContentAsString());
        assertEquals(401, response.getStatusCode());
    }

    @Test
    @RunAsClient
    public void testToAccessAdminResource() {
        WebResponse webresponse = getResponse("/rest/test/admin?username=" + USERNAME + "&password=" + PASSWORD_ADMIN);
        assertEquals(200, webresponse.getStatusCode());

        String response = getResponse("/rest/test/admin?username=" + USERNAME + "&password=" + PASSWORD_USER).getContentAsString();
        if (response.contains("Caller was not permitted access to a protected resource")) {
            Assert.assertTrue("User was sucessfully not permitted to access admin resource", true);
        } else {
            Assert.fail("Only admin should be able to access admin protected resource");

        }
    }

    @Test
    @RunAsClient
    public void testToAccessGeneralResource() {
        WebResponse webresponse = getResponse("/rest/test/admin?username=" + USERNAME + "&password=" + PASSWORD_ADMIN);
        assertEquals(200, webresponse.getStatusCode());

        WebResponse webresponse2 = getResponse("/rest/test/admin?username=" + USERNAME + "&password=" + PASSWORD_USER);
        assertEquals(200, webresponse2.getStatusCode());
    }

    private WebResponse getResponse(String url) {
        WebResponse webResponse = getHtmlpage(url).getWebResponse();
        return webResponse;
    }

    private Page getHtmlpage(String path) {
        if (url.toString().endsWith("/") && path.startsWith("/")) {
            path = path.substring(1);
        }

        Page page = null;
        try {
            page = webClient.getPage(url + path);
        } catch (IOException | FailingHttpStatusCodeException ex) {
            Logger.getLogger(RolesPermittedTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        return page;
    }

    @After
    public void cleanUp() {
        webClient.getCookieManager().clearCookies();
        webClient.close();
    }
}
