/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vendoree.payara.rolespermitted.rest;

import fish.payara.cdi.auth.roles.RolesPermitted;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.security.enterprise.SecurityContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 *
 * @author susan
 */
@Path("test")
public class TestRolesPermitted {

    @GET
    @Path("admin")
    @RolesPermitted({"payaraAdmin"})
    public String adminOnlyAccess() {
        return "Admin Protected";
    }

    @GET
    @Path("general")
    @RolesPermitted({"payaraAdmin", "payaraUser"})
    public String generalAccess() {
        return "Both the user have access";
    }
}
