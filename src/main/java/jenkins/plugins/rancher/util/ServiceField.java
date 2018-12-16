package jenkins.plugins.rancher.util;

import com.google.common.base.Strings;
import hudson.AbortException;

public class ServiceField {
    private final String stackName;
    private final String serviceName;

    public ServiceField(String service) throws AbortException {

        if (Strings.isNullOrEmpty(service)) {
            throw new AbortException("ServerName is Empty");
        }

        int firstSlashPosition = service.indexOf("/");
        if (firstSlashPosition == -1) {
            throw new AbortException("ServerName should be has StackName/ServiceName");
        }
        this.stackName = service.substring(0, firstSlashPosition);
        this.serviceName = service.substring(firstSlashPosition + 1);
    }

    public String getStackName() {
        return stackName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
