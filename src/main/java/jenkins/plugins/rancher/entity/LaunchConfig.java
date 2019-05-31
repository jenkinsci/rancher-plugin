package jenkins.plugins.rancher.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LaunchConfig {
    private String imageUuid;
    private String kind;
    private List<String> ports = new ArrayList<>();
    private String type;
    private boolean stdinOpen = true;

    private List<String> dataVolumes;
    private List<String> dataVolumesFrom;

    private InstanceHealthCheck healthCheck;
    private String[] healthCmd;
    private int healthInterval;
    private int healthRetries;
    private int healthTimeout;
    private LogConfig logConfig;

    private String workingDir;
    private List<String> entryPoint;
    private List<String> command;
    private Map<String, Object> environment = new HashMap<>();
    private Map<String, String> labels = new HashMap<>();

    private boolean tty = true;

    private String networkMode;

    private String requestedHostId;
    private String requestedIpAddress;


    private String domainName;
    private List<String> dnsSearch;
    private List<String> dns;

    private List<Secret> secrets = new ArrayList<>();

    private Long memory;
    private Long memoryReservation;

    public InstanceHealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(InstanceHealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    public String[] getHealthCmd() {
        return healthCmd;
    }

    public void setHealthCmd(String[] healthCmd) {
        this.healthCmd = healthCmd;
    }

    public int getHealthInterval() {
        return healthInterval;
    }

    public void setHealthInterval(int healthInterval) {
        this.healthInterval = healthInterval;
    }

    public int getHealthRetries() {
        return healthRetries;
    }

    public void setHealthRetries(int healthRetries) {
        this.healthRetries = healthRetries;
    }

    public int getHealthTimeout() {
        return healthTimeout;
    }

    public void setHealthTimeout(int healthTimeout) {
        this.healthTimeout = healthTimeout;
    }

    public List<String> getCommand() {
        return command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    public List<String> getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(List<String> entryPoint) {
        this.entryPoint = entryPoint;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public List<String> getDataVolumes() {
        return dataVolumes;
    }

    public void setDataVolumes(List<String> dataVolumes) {
        this.dataVolumes = dataVolumes;
    }

    public List<String> getDataVolumesFrom() {
        return dataVolumesFrom;
    }

    public void setDataVolumesFrom(List<String> dataVolumesFrom) {
        this.dataVolumesFrom = dataVolumesFrom;
    }

    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }

    public boolean isStdinOpen() {
        return stdinOpen;
    }

    public void setStdinOpen(boolean stdinOpen) {
        this.stdinOpen = stdinOpen;
    }

    public boolean isTty() {
        return tty;
    }

    public void setTty(boolean tty) {
        this.tty = tty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public LogConfig getLogConfig() {
        return logConfig;
    }

    public void setLogConfig(LogConfig logConfig) {
        this.logConfig = logConfig;
    }

    public List<Secret> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<Secret> secrets) {
        this.secrets = secrets;
    }

    public String getRequestedHostId() {
        return requestedHostId;
    }

    public void setRequestedHostId(String requestedHostId) {
        this.requestedHostId = requestedHostId;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public String getRequestedIpAddress() {
        return requestedIpAddress;
    }

    public void setRequestedIpAddress(String requestedIpAddress) {
        this.requestedIpAddress = requestedIpAddress;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<String> getDnsSearch() {
        return dnsSearch;
    }

    public void setDnsSearch(List<String> dnsSearch) {
        this.dnsSearch = dnsSearch;
    }

    public List<String> getDns() {
        return dns;
    }

    public void setDns(List<String> dns) {
        this.dns = dns;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public Long getMemoryReservation() {
        return memoryReservation;
    }

    public void setMemoryReservation(Long memoryReservation) {
        this.memoryReservation = memoryReservation;
    }
}
