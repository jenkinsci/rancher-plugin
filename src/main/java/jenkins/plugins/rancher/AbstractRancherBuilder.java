package jenkins.plugins.rancher;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Strings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import jenkins.plugins.rancher.entity.Service;
import jenkins.plugins.rancher.entity.Stack;
import jenkins.plugins.rancher.entity.Stacks;
import jenkins.plugins.rancher.util.CredentialsUtil;
import jenkins.plugins.rancher.util.EnvironmentParser;
import jenkins.plugins.rancher.util.ServiceField;
import jenkins.tasks.SimpleBuildStep;

public abstract class AbstractRancherBuilder extends Builder implements SimpleBuildStep {
    protected final String environmentId;
    protected final String endpoint;
    protected final String credentialId;
    protected final String service;
    protected int timeout = 50;
    protected RancherClient rancherClient;
    protected CredentialsUtil credentialsUtil;
    protected String environmentIdParsed;

    public AbstractRancherBuilder(String environmentId, String endpoint, String credentialId, String service, int timeout) {
        this.environmentId = environmentId;
        this.endpoint = endpoint;
        this.credentialId = credentialId;
        this.service = service;
        this.timeout = timeout;
    }

    public void setCredentialsUtil(CredentialsUtil credentialsUtil) {
        this.credentialsUtil = credentialsUtil;
    }

    public void setRancherClient(RancherClient rancherClient) {
        this.rancherClient = rancherClient;
    }

    protected void initializeClient(String endpoint) {
        if (credentialsUtil == null) {
            credentialsUtil = new CredentialsUtil();
        }

        if (rancherClient == null) {
            rancherClient = newRancherClient(endpoint);
        }
    }

    private RancherClient newRancherClient(String endpoint) {
        if (!Strings.isNullOrEmpty(credentialId)) {
            Optional<StandardUsernamePasswordCredentials> credential = credentialsUtil.getCredential(credentialId);
            if (credential.isPresent()) {
                return new RancherClient(endpoint, credential.get().getUsername(), credential.get().getPassword().getPlainText());
            }
        }
        return new RancherClient(endpoint);
    }

    protected void checkServiceState(Service service, TaskListener listener) throws AbortException {
        String state = service.getState();
        listener.getLogger().printf("service %s current state is %s%n", service.getName(), state);
        if (!(RancherBuilder.INACTIVE.equalsIgnoreCase(state) || RancherBuilder.ACTIVE.equalsIgnoreCase(state))) {
            throw new AbortException("Before upgrade service the service instance state should be 'inactive' or 'active'");
        }
    }

    protected Map<String, String> getBuildEnvs(Run<?, ?> build, TaskListener listener) {
        Map<String, String> envs = new HashMap<>();

        try {
            EnvVars environment = build.getEnvironment(listener);
            environment.keySet().forEach(key -> {
                String value = environment.get(key);
                envs.put(key, value);
            });
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
        }
        return envs;
    }

    protected Map<String, Object> customEnvironments(String environments) {
        return EnvironmentParser.parse(environments);
    }

    protected void waitUntilServiceStateIs(String serviceId, String targetState, TaskListener listener) throws AbortException {

        int timeoutMs = timeout != 0 ?  1000 * timeout : 1000 * 50;

        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        listener.getLogger().println("waiting service state to be " + targetState + " (timeout:" + timeout + "s)");
        try {
            boolean success = false;
            while ((current - start) < timeoutMs) {
                Optional<Service> checkService = rancherClient.service(environmentIdParsed, serviceId);
                String state = checkService.get().getState();
                if (state.equalsIgnoreCase(targetState)) {
                    listener.getLogger().println("current service state is " + targetState);
                    success = true;
                    break;
                }
                Thread.sleep(2000);
                current = System.currentTimeMillis();
            }
            if (!success) {
                throw new AbortException("timeout");
            }
        } catch (Exception e) {
            throw new AbortException("Exception happened to wait service state with message:" + e.getMessage());
        }
    }

    protected Stack getStack(@Nonnull TaskListener listener, ServiceField serviceField, RancherClient rancherClient, boolean createIfNotExists) throws IOException {
        Optional<Stacks> stacks = rancherClient.stacks(environmentIdParsed);
        if (!stacks.isPresent()) {
            throw new AbortException("error happen when fetch stack in environment<" + environmentIdParsed + ">");
        }

        Optional<Stack> stack = stacks.get().getData().stream().filter(stackItem -> isEqual(serviceField, stackItem)).findAny();
        if (stack.isPresent()) {
            listener.getLogger().println("Stack already exist. skip");
            return stack.get();
        } else if (!createIfNotExists){
            throw new AbortException(String.format("Stack [%s] does not exists", serviceField.getStackName()));
        } else {
            listener.getLogger().println("Stack not exist, create first");
            return createStack(serviceField, rancherClient);
        }
    }

    private Stack createStack(ServiceField serviceField, RancherClient rancherClient) throws IOException {
        Stack stack = new Stack();
        stack.setName(serviceField.getStackName());
        Optional<Stack> stackOptional = rancherClient.createStack(stack, environmentIdParsed);
        if (!stackOptional.isPresent()) {
            throw new AbortException("error happen when create stack");
        } else {
            return stackOptional.get();
        }
    }

    private boolean isEqual(ServiceField serviceField, Stack stack1) {
        return stack1.getName().equals(serviceField.getStackName());
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getEnvironmentId() {
      return environmentId;
  }

    public String getCredentialId() {
        return credentialId;
    }

    public int getTimeout() {
        return timeout == 0 ? RancherBuilder.DEFAULT_TIMEOUT : timeout;
    }

    public String getService() {
        return service;
    }
}
