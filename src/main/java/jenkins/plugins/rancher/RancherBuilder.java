package jenkins.plugins.rancher;


import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.rancher.action.InServiceStrategy;
import jenkins.plugins.rancher.action.ServiceUpgrade;
import jenkins.plugins.rancher.entity.*;
import jenkins.plugins.rancher.entity.Stack;
import jenkins.plugins.rancher.util.CredentialsUtil;
import jenkins.plugins.rancher.util.EnvironmentParser;
import jenkins.plugins.rancher.util.Parser;
import jenkins.plugins.rancher.util.ServiceField;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RancherBuilder extends AbstractRancherBuilder {

    public static final String UPGRADED = "upgraded";
    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final int DEFAULT_TIMEOUT = 50;

    private final String image;
    private final boolean confirm;
    private final boolean startFirst;
    private final String ports;
    private final String environments;

    @DataBoundConstructor
    public RancherBuilder(
            String environmentId, String endpoint, String credentialId, String service,
            String image, boolean confirm, boolean startFirst, String ports, String environments, int timeout) {
        super(environmentId, endpoint, credentialId,service, timeout);
        this.image = image;
        this.confirm = confirm;
        this.startFirst = startFirst;
        this.ports = ports;
        this.environments = environments;
    }

    protected static RancherBuilder newInstance(String environmentId, String endpoint, String credentialId, String service,
                                                String image, boolean confirm, boolean startFirst, String ports, String environments, int timeout,
                                                RancherClient rancherClient, CredentialsUtil credentialsUtil) {
        RancherBuilder rancherBuilder = new RancherBuilder(environmentId, endpoint, credentialId, service, image, confirm, startFirst, ports, environments, timeout);
        rancherBuilder.setCredentialsUtil(credentialsUtil);
        rancherBuilder.setRancherClient(rancherClient);
        return rancherBuilder;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {

        Map<String, String> buildEnvironments = getBuildEnvs(build, listener);
        Map<String, Object> environments = this.customEnvironments(Parser.paraser(this.environments, buildEnvironments));

        String dockerUUID = String.format("docker:%s", Parser.paraser(image, buildEnvironments));

        environmentIdParsed = Parser.paraser(environmentId, buildEnvironments);
        initializeClient(Parser.paraser(endpoint, buildEnvironments));

        String service = Parser.paraser(this.getService(), buildEnvironments);
        ServiceField serviceField = new ServiceField(service);

        listener.getLogger().printf("Deploy/Upgrade image[%s] to service [%s] to rancher environment [%s/projects/%s]%n", dockerUUID, service, endpoint, environmentIdParsed);

        Stack stack = getStack(listener, serviceField, rancherClient, true);
        Optional<Services> services = rancherClient.services(environmentIdParsed, stack.getId());
        if (!services.isPresent()) {
            throw new AbortException("Error happen when fetch stack<" + stack.getName() + "> services");
        }

        Optional<Service> serviceInstance = services.get().getData().stream().filter(s -> s.getName().equals(serviceField.getServiceName())).findAny();
        if (serviceInstance.isPresent()) {
            upgradeService(serviceInstance.get(), dockerUUID, listener, environments);
        } else {
            createService(stack, serviceField.getServiceName(), dockerUUID, listener, environments);
        }
    }

    private void upgradeService(Service service, String dockerUUID, TaskListener listener, Map<String, Object> environments) throws IOException {
        listener.getLogger().println("Upgrading service instance");
        checkServiceState(service, listener);
        ServiceUpgrade serviceUpgrade = new ServiceUpgrade();
        InServiceStrategy inServiceStrategy = new InServiceStrategy();

        LaunchConfig launchConfig = service.getLaunchConfig();
        launchConfig.setImageUuid(dockerUUID);
        launchConfig.getEnvironment().putAll(environments);

        if (!Strings.isNullOrEmpty(ports)) {
            launchConfig.setPorts(Arrays.asList(ports.split(",")));
        }

        if (startFirst && launchConfig.getPorts().isEmpty() ) {
            inServiceStrategy.setStartFirst(startFirst);

        }
        else if (startFirst && !(launchConfig.getPorts().isEmpty())){
            throw new AbortException("Ports can not be in use with start with stop service.");
        }
        else { 
            inServiceStrategy.setStartFirst(startFirst);
        }
        // inServiceStrategy.setStartFirst(launchConfig.getPorts().isEmpty());

        inServiceStrategy.setLaunchConfig(launchConfig);
        serviceUpgrade.setInServiceStrategy(inServiceStrategy);
        Optional<Service> serviceInstance = rancherClient.upgradeService(environmentIdParsed, service.getId(), serviceUpgrade);
        if (!serviceInstance.isPresent()) {
            throw new AbortException("upgrade service error");
        }

        waitUntilServiceStateIs(serviceInstance.get().getId(), UPGRADED, listener);

        if (!confirm) {
            return;
        }

        rancherClient.finishUpgradeService(environmentIdParsed, serviceInstance.get().getId());
        waitUntilServiceStateIs(serviceInstance.get().getId(), ACTIVE, listener);
    }

    private void createService(Stack stack, String serviceName, String dockerUUID, TaskListener listener, Map<String, Object> environments) throws IOException {
        listener.getLogger().println("Creating service instance");
        Service service = new Service();
        service.setName(serviceName);
        LaunchConfig launchConfig = new LaunchConfig();
        launchConfig.setImageUuid(dockerUUID);
        launchConfig.setEnvironment(environments);
        if (!Strings.isNullOrEmpty(ports)) {
            launchConfig.setPorts(Arrays.asList(ports.split(",")));
        }
        service.setLaunchConfig(launchConfig);
        Optional<Service> serviceInstance = rancherClient.createService(service, environmentIdParsed, stack.getId());

        if (!serviceInstance.isPresent()) {
            throw new AbortException("upgrade service error");
        }

        waitUntilServiceStateIs(serviceInstance.get().getId(), ACTIVE, listener);
    }

    public boolean isConfirm() {
        return confirm;
    }

    public boolean isStartFirst() {
        return startFirst;
    }

    public String getEnvironments() {
        return environments;
    }

    public String getImage() {
        return image;
    }

    public String getPorts() {
        return ports;
    }

    @Symbol("rancher")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private static final CredentialsUtil credentialsUtil = new CredentialsUtil();

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName() {
            return "Deploy/Upgrade Rancher Service";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillCredentialIdItems() {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new ListBoxModel();
            }
            List<StandardUsernamePasswordCredentials> credentials = credentialsUtil.getCredentials();
            return new StandardUsernameListBoxModel()
                    .withEmptySelection()
                    .withAll(credentials);
        }

        public FormValidation doTestConnection(
                @QueryParameter("endpoint") final String endpoint,
                @QueryParameter("environmentId") final String environmentId,
                @QueryParameter("credentialId") final String credentialId
        ) throws IOException, ServletException {

            try {
                RancherClient client;
                Optional<StandardUsernamePasswordCredentials> credential = credentialsUtil.getCredential(credentialId);
                if (credential.isPresent()) {
                    client = new RancherClient(endpoint, credential.get().getUsername(), credential.get().getPassword().getPlainText());
                } else {
                    client = new RancherClient(endpoint);
                }
                Optional<Environment> environment = client.environment(environmentId);
                if (!environment.isPresent()) {
                    return FormValidation.error("Environment [" + environmentId + "] not found please check configuration");
                }
                return FormValidation.ok("Connection Success");
            } catch (Exception e) {
                return FormValidation.error("Connection fails with message : " + e.getMessage());
            }
        }

        public FormValidation doCheckTimeout(@QueryParameter int value) {
            return value > 0 ? FormValidation.ok() : FormValidation.error("Time should be at least 1");
        }

        public FormValidation doCheckPorts(@QueryParameter String value) {
            if (Strings.isNullOrEmpty(value)) {
                return FormValidation.ok();
            }

            String[] ports = value.split(",");
            boolean inValid = Arrays.asList(ports)
                    .stream()
                    .anyMatch(
                            port -> Arrays.asList(port.split(":"))
                                    .stream()
                                    .anyMatch(part -> !StringUtils.isNumeric(part)));
            return inValid ? FormValidation.error("Ports config should be like: 8080:8080,8181:8181") : FormValidation.ok();
        }

        public FormValidation doCheckCredentialId(@QueryParameter String value) {
            return !Strings.isNullOrEmpty(value)
                    && credentialsUtil.getCredential(value).isPresent()
                    ? FormValidation.ok() : FormValidation.warning("API key is required when Rancher ACL is enable");
        }

        public FormValidation doCheckEndpoint(@QueryParameter String value) {
            try {
                new URL(value);
                return FormValidation.ok();
            } catch (MalformedURLException e) {
                return FormValidation.error("Not a rancher v2 api endpoint");
            }
        }

        public FormValidation doCheckAccessKey(@QueryParameter String value) {
            return !Strings.isNullOrEmpty(value) ? FormValidation.ok() : FormValidation.error("AccessKey can't be empty");
        }

        public FormValidation doCheckSecretKey(@QueryParameter String value) {
            return !Strings.isNullOrEmpty(value) ? FormValidation.ok() : FormValidation.error("SecretKey can't be empty");
        }

        public FormValidation doCheckEnvironmentId(@QueryParameter String value) {
            return !Strings.isNullOrEmpty(value) ? FormValidation.ok() : FormValidation.error("EnvironmentId can't be empty");
        }

        public FormValidation doCheckService(@QueryParameter String value) {
            boolean validate = !Strings.isNullOrEmpty(value) && value.contains("/") && value.split("/").length == 2;
            return validate ? FormValidation.ok() : FormValidation.error("Service name should be like stack/service");
        }

        public FormValidation doCheckImage(@QueryParameter String value) {
            return !Strings.isNullOrEmpty(value) ? FormValidation.ok() : FormValidation.error("Docker image can't be empty");
        }

    }

}
