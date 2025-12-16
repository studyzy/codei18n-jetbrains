package com.github.studyzy.codei18n.models;

import java.util.HashMap;
import java.util.Map;

public class CliConfiguration {
    private String cliPath = "codei18n";
    private String targetLanguage = "zh-CN";
    private int timeout = 5000;
    private String workingDirectory;
    private Map<String, String> environmentVariables = new HashMap<>();

    public String getCliPath() { return cliPath; }
    public void setCliPath(String cliPath) { this.cliPath = cliPath; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
}
