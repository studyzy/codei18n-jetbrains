package com.github.studyzy.codei18n.services;

import com.github.studyzy.codei18n.settings.PluginSettings;
import com.github.studyzy.codei18n.utils.ProcessExecutor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class CliService {
    private static final Logger LOG = Logger.getInstance(CliService.class);
    private final Project project;
    private ProcessExecutor processExecutor;

    public CliService(Project project) {
        this.project = project;
    }

    public static CliService getInstance(Project project) {
        return project.getService(CliService.class);
    }
    
    // For testing
    public void setProcessExecutor(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    private ProcessExecutor getProcessExecutor() {
        return processExecutor != null ? processExecutor : ProcessExecutor.getInstance();
    }

    public String executeCommand(String command, List<String> args, String workingDir, int timeout) {
        return executeCommand(command, args, workingDir, timeout, null);
    }

    public String executeCommand(String command, List<String> args, String workingDir, int timeout, String input) {
        try {
            ProcessExecutor executor = getProcessExecutor();
            GeneralCommandLine commandLine = executor.createCommandLine(
                    command, 
                    args, 
                    workingDir != null ? workingDir : project.getBasePath()
            );

            LOG.info("Executing CLI command: " + commandLine.getCommandLineString());

            ProcessOutput output = executor.execute(commandLine, timeout, input);

            if (output.isTimeout()) {
                LOG.warn("CLI execution timed out");
                return null;
            }

            if (output.getExitCode() != 0) {
                LOG.warn("CLI execution failed with exit code " + output.getExitCode() + ": " + output.getStderr());
                return null;
            }

            return output.getStdout();

        } catch (ExecutionException e) {
            LOG.error("Failed to execute CLI command", e);
            return null;
        }
    }

    public String getVersion(String cliPath) {
        return executeCommand(cliPath, List.of("version"), null, 5000);
    }

    public boolean initProject(String cliPath, String sourceLang, String localLang) {
        var args = new ArrayList<String>();
        args.add("init");
        if (sourceLang != null) {
            args.addAll(List.of("--source-lang", sourceLang));
        }
        if (localLang != null) {
            args.addAll(List.of("--local-lang", localLang));
        }
        String output = executeCommand(cliPath, args, project.getBasePath(), 10000);
        return output != null;
    }

    public String scanFile(String filePath, boolean withTranslations, boolean stdin, String content) {
        var args = new ArrayList<String>();
        args.addAll(List.of("scan", "--file", filePath, "--format", "json"));
        
        if (withTranslations) {
            args.add("--with-translations");
        }
        
        if (stdin) {
            args.add("--stdin");
        }
        
        String cliPath = PluginSettings.getInstance().cliConfiguration.getCliPath();
        
        return executeCommand(cliPath, args, project.getBasePath(), 5000, content);
    }
}
