/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vendoree.payara.rolespermitted;

import fish.payara.cdi.auth.roles.RolesPermitted;
import java.security.Principal;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 *
 * @author susan
 */
@RequestScoped
public class TestRolesPermitted {

    @Inject
    Principal principal;

    @RolesPermitted({"payaraAdmin"})
    public String getUserName() {
        return principal.getName();
    }
}
