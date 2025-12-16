package com.github.studyzy.codei18n.services;

import com.github.studyzy.codei18n.utils.ProcessExecutor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CliServiceTest {

    @Mock
    private Project project;
    @Mock
    private ProcessExecutor processExecutor;
    @Mock
    private GeneralCommandLine commandLine;
    @Mock
    private ProcessOutput processOutput;

    private CliService cliService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(project.getBasePath()).thenReturn("/test/path");
        cliService = new CliService(project);
        cliService.setProcessExecutor(processExecutor);
    }

    @Test
    public void testExecuteCommandSuccess() throws ExecutionException {
        when(processExecutor.createCommandLine(any(), any(), any())).thenReturn(commandLine);
        when(processExecutor.execute(any(), anyInt())).thenReturn(processOutput);
        when(processOutput.isTimeout()).thenReturn(false);
        when(processOutput.getExitCode()).thenReturn(0);
        when(processOutput.getStdout()).thenReturn("success");

        String result = cliService.executeCommand("codei18n", List.of("version"), null, 5000);
        assertEquals("success", result);
    }

    @Test
    public void testExecuteCommandTimeout() throws ExecutionException {
        when(processExecutor.createCommandLine(any(), any(), any())).thenReturn(commandLine);
        when(processExecutor.execute(any(), anyInt())).thenReturn(processOutput);
        when(processOutput.isTimeout()).thenReturn(true);

        String result = cliService.executeCommand("codei18n", List.of("version"), null, 5000);
        assertNull(result);
    }
}
