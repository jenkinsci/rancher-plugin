package jenkins.plugins.rancher;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.rancher.entity.Service;
import jenkins.plugins.rancher.entity.Services;
import jenkins.plugins.rancher.entity.Stack;
import jenkins.plugins.rancher.entity.Stacks;
import jenkins.plugins.rancher.util.CredentialsUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class RancherBuilderTest {

    private Run build;
    private Launcher launcher;
    private FilePath filePath;
    private TaskListener listener;
    private CredentialsUtil credentialsUtil;
    private RancherClient rancherClient;
    private RancherBuilder rancherBuilder;

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

        rancherBuilder = RancherBuilder.newInstance(
                "1a7", "http://localhost:8080/v2-beta", "credentialId", "stack/service", "nginx", true, false, "", "", 50,
                rancherClient, credentialsUtil);
    }

    @Test
    public void should_create_stack_and_service_when_both_of_them_not_present() throws IOException, InterruptedException {
        // given
        Stacks emptyStacks = new Stacks();
        emptyStacks.setData(Collections.emptyList());
        when(rancherClient.stacks(anyString())).thenReturn(Optional.of(emptyStacks));

        Stack newStack = new Stack();
        newStack.setName("stack");

        when(rancherClient.createStack(any(Stack.class), any(String.class))).thenReturn(Optional.of(newStack));

        Services emptyServices = new Services();
        emptyServices.setData(Collections.emptyList());

        when(rancherClient.services(anyString(), anyString())).thenReturn(Optional.of(emptyServices));

        Service newService = new Service();
        newService.setState("ACTIVE");

        when(rancherClient.createService(any(Service.class), anyString(), anyString())).thenReturn(Optional.of(newService));

        when(rancherClient.service(anyString(), anyString())).thenReturn(Optional.of(newService));

        // when
        rancherBuilder.perform(build, filePath, launcher, listener);

        // then
        verify(rancherClient, timeout(1)).createStack(any(Stack.class), anyString());
        verify(rancherClient, timeout(1)).createService(any(Service.class), anyString(), anyString());
    }

}