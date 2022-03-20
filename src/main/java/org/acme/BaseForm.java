package org.acme;

import javax.ws.rs.FormParam;

public class BaseForm {

    public @FormParam("teamName")
    String teamName;

    public @FormParam("clusterDomain")
    String clusterDomain;

    public @FormParam("gitServer")
    String gitServer;

    public @FormParam("gitUser")
    String gitUser;

    public @FormParam("gitPassword")
    String gitPassword;

    public @FormParam("ocpAdminUser")
    String ocpAdminUser;

    public @FormParam("ocpAdminPassword")
    String ocpAdminPassword;

    public @FormParam("exercise")
    int exercise;
}
