/** Copyright Payara Services Limited **/
package org.vendoree.payara.jaxrs.rolesallowed.servlet;

import javax.annotation.security.DeclareRoles;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/rest")
@DeclareRoles({ "a", "b" })
public class JaxRsActivator extends Application {

}
