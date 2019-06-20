
package fish.payara.tomcat;

import java.io.IOException;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.auth.LoginConfig;


//@BasicAuthenticationMechanismDefinition(
//    realmName="myRealm"
//)

@LoginConfig(
        authMethod = "MP-JWT",
        // Even though specified being only for HTTP Basic auth, JBoss/WildFly/Swarm mandates this
        // to refer to its proprietary "security domain" concept.
        realmName = "MP-JWT"
    )

@WebServlet("/servlet")
@ServletSecurity(@HttpConstraint(rolesAllowed = "myRole"))
public class Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().write(" Request for caller " + request.getUserPrincipal().getName());
        response.getWriter().write(" Caller has role myRole " + request.isUserInRole("myRole"));
    }
}
