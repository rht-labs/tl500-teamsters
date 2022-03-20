package org.acme;

import java.util.ArrayList;
import java.util.List;

public class PageData {

    public PageData() {
        nukedTeams = new ArrayList<>();
        createdTeams = new ArrayList<>();
    }

    public int exercise;
    public List<String> nukedTeams;
    public List<String> createdTeams;
    public String show;

    public String clusterDomain;
    public String gitServer;
    public String gitUser;
    public String gitPassword;
    public String ocpAdminUser;
    public String ocpAdminPassword;
}
