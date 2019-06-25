/** Copyright Payara Services Limited **/
package org.vendoree.tomcat.eesecurity;

import java.io.IOException;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@BasicAuthenticationMechanismDefinition(realmName = "myRealm")

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
