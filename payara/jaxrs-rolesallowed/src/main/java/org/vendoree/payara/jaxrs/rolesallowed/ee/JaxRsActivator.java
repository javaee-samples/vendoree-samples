/** Copyright Payara Services Limited **/
package org.vendoree.payara.jaxrs.rolesallowed.ee;

import javax.annotation.security.DeclareRoles;
import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationScoped
@ApplicationPath("/rest")
@DeclareRoles({ "a", "b" })
@BasicAuthenticationMechanismDefinition(realmName = "foo-ee")
public class JaxRsActivator extends Application {

}
