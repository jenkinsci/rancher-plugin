package jenkins.plugins.rancher;

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.rancher.entity.Environment;
import jenkins.plugins.rancher.entity.Service;
import jenkins.plugins.rancher.entity.Services;
import jenkins.plugins.rancher.entity.Stack;
import jenkins.plugins.rancher.util.CredentialsUtil;
import jenkins.plugins.rancher.util.Parser;
import jenkins.plugins.rancher.util.ServiceField;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jenkins.plugins.rancher.RancherBuilder.ACTIVE;
import static jenkins.plugins.rancher.RancherBuilder.UPGRADED;

public class RancherUpgradeBuilder extends AbstractRancherBuilder {
    public static final String ROLLBACK_ACTION = "rollback";
    private final String finishAction;

    @DataBoundConstructor
    public RancherUpgradeBuilder(
            String environmentId, String endpoint, String credentialId, String service, String finishAction, int timeout) {
        super(environmentId, endpoint, credentialId, service, timeout);
        this.finishAction = finishAction;
    }

    protected static RancherUpgradeBuilder newInstance(String environmentId, String endpoint, String credentialId, String service,
                                                       String finishAction, int timeout, RancherClient rancherClient, CredentialsUtil credentialsUtil) {
        RancherUpgradeBuilder rancherBuilder = new RancherUpgradeBuilder(environmentId, endpoint, credentialId, service,finishAction, timeout);
        rancherBuilder.setCredentialsUtil(credentialsUtil);
        rancherBuilder.setRancherClient(rancherClient);
        return rancherBuilder;
    }


    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        Map<String, String> buildEnvironments = getBuildEnvs(build, listener);

        String envid=Parser.paraser(getEnvironmentId(), buildEnvironments);
        initializeClient(Parser.paraser(endpoint, buildEnvironments));

        String service = Parser.paraser(this.getService(), buildEnvironments);
        ServiceField serviceField = new ServiceField(service);

        listener.getLogger().printf("Finish[%s] upgraded service [%s] to rancher environment [%s/projects/%s]%n", finishAction, service, endpoint, envid);

        Stack stack = getStack(listener, serviceField, rancherClient, false);
        Optional<Services> services = rancherClient.services(envid, stack.getId());
        if (!services.isPresent()) {
            throw new AbortException("Error happen when fetch stack<" + stack.getName() + "> services");
        }
        Optional<Service> serviceInstance = services.get().getData().stream().filter(s -> s.getName().equals(serviceField.getServiceName())).findAny();

        if (serviceInstance.isPresent()) {
            String state = serviceInstance.get().getState();
            listener.getLogger().printf("service %s current state is %s%n", service, state);
            if (!UPGRADED.equalsIgnoreCase(state)) {
                throw new AbortException("Before confirming service the service instance state should be 'UPGRADED'");
            }
            if (ROLLBACK_ACTION.equalsIgnoreCase(finishAction)) {
                rancherClient.rollbackUpgradeService(environmentId, serviceInstance.get().getId());
            } else {
                rancherClient.finishUpgradeService(environmentId, serviceInstance.get().getId());
            }
            waitUntilServiceStateIs(serviceInstance.get().getId(), ACTIVE, listener);
        } else {
            throw new AbortException(String.format("Service [%s] does not exist.", service));
        }
    }

    public String getFinishAction() {
        return finishAction;
    }

    @Symbol("confirm")
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
            return "Finish Rancher Service Upgrade";
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

    }

}
