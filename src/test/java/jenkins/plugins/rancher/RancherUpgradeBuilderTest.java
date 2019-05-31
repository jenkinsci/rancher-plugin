package jenkins.plugins.rancher;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.rancher.entity.*;
import jenkins.plugins.rancher.util.CredentialsUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Optional;

import static jenkins.plugins.rancher.RancherBuilder.ACTIVE;
import static jenkins.plugins.rancher.RancherBuilder.UPGRADED;
import static jenkins.plugins.rancher.RancherUpgradeBuilder.ROLLBACK_ACTION;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class RancherUpgradeBuilderTest {

    private Run build;
    private Launcher launcher;
    private FilePath filePath;
    private TaskListener listener;
    private CredentialsUtil credentialsUtil;
    private RancherClient rancherClient;
    private RancherUpgradeBuilder rancherUpgradeBuilder;

    @Before
    public void setUp() throws Exception {
        filePath = new FilePath(new File("/tmp/jenkins/workspace/test"));
        build = mock(Run.class);
        launcher = mock(Launcher.class);
        listener = mock(TaskListener.class);
        PrintStream logger = mock(PrintStream.class);
        when(listener.getLogger()).thenReturn(logger);

        credentialsUtil = mock(CredentialsUtil.class);
        when(credentialsUtil.getCredential(anyString())).thenReturn(Optional.empty());

        rancherClient = mock(RancherClient.class);

        rancherUpgradeBuilder = RancherUpgradeBuilder.newInstance(
                "1a7", "http://localhost:8080/v2-beta", "credentialId", "stack/service",
                ROLLBACK_ACTION, 50, rancherClient, credentialsUtil);
    }

    @Test
    public void should_rollback_service() throws IOException, InterruptedException {
        // given
        Stacks existingStacks = new Stacks();
        Stack stack = new Stack();
        stack.setName("stack");
        existingStacks.setData(Collections.singletonList(stack));
        when(rancherClient.stacks(anyString())).thenReturn(Optional.of(existingStacks));

        Services upgradedServices = new Services();
        Service upgradedService = makeTestService(UPGRADED);
        upgradedServices.setData(Collections.singletonList(upgradedService));

        Services activeServices = new Services();
        Service activeService = makeTestService(ACTIVE);
        activeServices.setData(Collections.singletonList(activeService));

        when(rancherClient.services(anyString(), anyString())).thenReturn(Optional.of(upgradedServices));
        when(rancherClient.service(anyString(), anyString())).thenReturn(Optional.of(upgradedService), Optional.of(activeService));

        when(rancherClient.rollbackUpgradeService(anyString(), anyString())).thenReturn(Optional.of(activeService));

        // when
        rancherUpgradeBuilder.perform(build, filePath, launcher, listener);

        //then
        verify(rancherClient,timeout(1)).rollbackUpgradeService(anyString(), anyString());

    }
    private Service makeTestService(String state) {
        Service service = new Service();
        service.setName("service");
        service.setState(state);
        service.setLaunchConfig(mock(LaunchConfig.class));
        return service;
    }

}
