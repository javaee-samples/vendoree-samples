/** Copyright Payara Services Limited **/
package org.vendoree.tomcat.jwt;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.auth.LoginConfig;

@LoginConfig(authMethod = "MP-JWT")
@ApplicationScoped
public class ApplicationInit {}
