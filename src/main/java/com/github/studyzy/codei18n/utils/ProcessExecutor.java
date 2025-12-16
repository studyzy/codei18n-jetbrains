package com.github.studyzy.codei18n.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.application.ApplicationManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service(Service.Level.APP)
public class ProcessExecutor {
    public static ProcessExecutor getInstance() {
        return ApplicationManager.getApplication().getService(ProcessExecutor.class);
    }

    public ProcessOutput execute(GeneralCommandLine commandLine, int timeout) throws ExecutionException {
        return execute(commandLine, timeout, null);
    }

    public ProcessOutput execute(GeneralCommandLine commandLine, int timeout, String input) throws ExecutionException {
        CapturingProcessHandler handler = new CapturingProcessHandler(commandLine);
        if (input != null) {
            try {
                if (handler.getProcessInput() != null) {
                    handler.getProcessInput().write(input.getBytes(StandardCharsets.UTF_8));
                    handler.getProcessInput().close();
                }
            } catch (IOException e) {
                throw new ExecutionException("Failed to write to process input", e);
            }
        }
        return handler.runProcess(timeout);
    }
    
    public GeneralCommandLine createCommandLine(String exePath, List<String> parameters, String workingDir) {
        return new GeneralCommandLine()
                .withExePath(exePath)
                .withParameters(parameters)
                .withWorkDirectory(workingDir)
                .withCharset(StandardCharsets.UTF_8);
    }
}
