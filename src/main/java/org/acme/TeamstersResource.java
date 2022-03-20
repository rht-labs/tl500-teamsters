package org.acme;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.util.Config;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.MultipartForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

@Path("/teamsters")
public class TeamstersResource {

    @ConfigProperty(name = "CLUSTER_DOMAIN", defaultValue = "apps.openshift-cluster.com")
    String clusterDomain;

    @ConfigProperty(name = "GIT_SERVER", defaultValue = "gitlab-ce.apps.openshift-cluster.com")
    String gitServer;

    @ConfigProperty(name = "GITLAB_USER", defaultValue = "user")
    String gitUser;

    @ConfigProperty(name = "GITLAB_PASSWORD", defaultValue = "password")
    String gitPassword;

    @ConfigProperty(name = "OCP_ADMIN_USER", defaultValue = "admin")
    String ocpAdminUser;

    @ConfigProperty(name = "OCP_ADMIN_PASSWORD", defaultValue = "password")
    String ocpAdminPassword;

    @ConfigProperty(name = "FORCE_HTTPS", defaultValue = "false")
    boolean forceHttps;

    private final Template page;
    private PageData pd = new PageData();

    final List<Integer> exercises = IntStream.range(1, 6).boxed().collect(Collectors.toList());

    public TeamstersResource(Template page) {
        this.page = requireNonNull(page, "page is required");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@QueryParam(value = "show") String show) {
        pd.clusterDomain = clusterDomain;
        pd.gitServer = gitServer;
        pd.gitUser = gitUser;
        pd.gitPassword = gitPassword;
        pd.ocpAdminUser = ocpAdminUser;
        pd.ocpAdminPassword = ocpAdminPassword;
        return page.data("exercises", exercises)
                .data("page", pd)
                .data("show", show);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @Path("/nuke")
    public Response nuke(@MultipartForm BaseForm form, @Context UriInfo info) {
        pd.nukedTeams.add(form.teamName);
        try {
            nukeIt(form);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        URI uri = info.getBaseUriBuilder().scheme((forceHttps) ? "https" : "http").path("teamsters").queryParam("show", "nuke").build();
        return Response.status(301)
                .location(uri)
                .build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @Path("/create")
    public Response create(@MultipartForm BaseForm form, @Context UriInfo info) {
        pd.createdTeams.add(form.teamName);
        try {
            createIt(form);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        URI uri = info.getBaseUriBuilder().scheme((forceHttps) ? "https" : "http").path("teamsters").queryParam("show", "create").build();
        return Response.status(301)
                .location(uri)
                .build();
    }

    @POST
    @Path("/clear-created")
    public Response clearCreated(@Context UriInfo info) {
        pd.createdTeams.clear();
        URI uri = info.getBaseUriBuilder().scheme((forceHttps) ? "https" : "http").path("teamsters").queryParam("show", "create").build();
        return Response.status(301)
                .location(uri)
                .build();
    }

    @POST
    @Path("/clear-nuked")
    public Response clearNuked(@Context UriInfo info) {
        pd.nukedTeams.clear();
        URI uri = info.getBaseUriBuilder().scheme((forceHttps) ? "https" : "http").path("teamsters").queryParam("show", "nuke").build();
        return Response.status(301)
                .location(uri)
                .build();
    }

    private List<V1EnvVar> getEnvVars(BaseForm form) {
        List<V1EnvVar> envVars = new ArrayList<>();
        envVars.add(new V1EnvVar().name("CLUSTER_DOMAIN").value(form.clusterDomain));
        envVars.add(new V1EnvVar().name("GIT_SERVER").value(form.gitServer));
        envVars.add(new V1EnvVar().name("TEAM_NAME").value(form.teamName));
        envVars.add(new V1EnvVar().name("GITLAB_USER").value(form.gitUser));
        envVars.add(new V1EnvVar().name("GITLAB_PASSWORD").value(form.gitPassword));
        envVars.add(new V1EnvVar().name("OCP_USER").value(form.gitUser));
        envVars.add(new V1EnvVar().name("OCP_PASSWORD").value(form.gitPassword));
        envVars.add(new V1EnvVar().name("OCP_ADMIN_USER").value(form.ocpAdminUser));
        envVars.add(new V1EnvVar().name("OCP_ADMIN_PASSWORD").value(form.ocpAdminPassword));
        return envVars;
    }

    private void nukeIt(@MultipartForm BaseForm form) throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        List<V1EnvVar> envVars = getEnvVars(form);
        envVars.add(new V1EnvVar().name("NUKE_ONLY").value("true"));

        V1Pod pod = new V1PodBuilder()
                .withNewMetadata()
                .withName("nuke-" + form.teamName)
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy("Never")
                .addNewContainer()
                .withName("nuke")
                .withEnv(envVars)
                .withImage("quay.io/eformat/tech-exercise-test")
                .withNewResources()
                .withLimits(new HashMap<>())
                .endResources()
                .endContainer()
                .endSpec()
                .build();
        //System.out.println(Yaml.dump(pod));
        // delete pod if it exists
        V1Pod deleteResult;
        try {
            deleteResult = api.deleteNamespacedPod("nuke-" + form.teamName, "tl500", null, null, 30, null, null, null);
            //System.out.println(deleteResult);
        } catch (ApiException e) {
            if (404 != e.getCode()) {
                System.out.println("Expected ok or 404(NotFound) got " + e.getCode() + " returning");
                return;
            }
        }
        V1Pod createResult;
        try {
            createResult = api.createNamespacedPod("tl500", pod, null, null, null);
        } catch (ApiException e) {
            System.out.println("Caught exception " + e.getCode());
            return;
        }
        //System.out.println(createResult.getStatus());
        System.out.println("Pod " + "nuke-" + form.teamName + " created OK in tl500 namespace.");
    }

    private void createIt(@MultipartForm BaseForm form) throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();

        List<V1EnvVar> envVars = getEnvVars(form);
        envVars.add(new V1EnvVar().name("EXERCISE").value(Integer.toString(form.exercise)));

        V1Pod pod = new V1PodBuilder()
                .withNewMetadata()
                .withName("create-" + form.teamName)
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy("Never")
                .addNewContainer()
                .withName("create")
                .withEnv(envVars)
                .withImage("quay.io/eformat/tech-exercise-test")
                .withNewResources()
                .withLimits(new HashMap<>())
                .endResources()
                .endContainer()
                .endSpec()
                .build();

        // FIXME return if already running pod
        // token logout will not work if reusing same git user in parallel runs
        // need to use different git/ocp users

        //System.out.println(Yaml.dump(pod));
        // delete pod if it exists
        V1Pod deleteResult;
        try {
            deleteResult = api.deleteNamespacedPod("create-" + form.teamName, "tl500", null, null, 30, null, null, null);
            //System.out.println(deleteResult);
        } catch (ApiException e) {
            if (404 != e.getCode()) {
                System.out.println("Expected ok or 404(NotFound) got " + e.getCode() + " returning");
                return;
            }
        }
        V1Pod createResult;
        try {
            createResult = api.createNamespacedPod("tl500", pod, null, null, null);
        } catch (ApiException e) {
            System.out.println("Caught exception " + e.getCode());
            return;
        }
        //System.out.println(createResult.getStatus());
        System.out.println("Pod " + "create-" + form.teamName + " created OK in tl500 namespace.");
    }

}
