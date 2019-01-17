package com.liveramp.daemon_lib;

import com.liveramp.daemon_lib.built_in.NoOpDaemonLock;
import com.liveramp.daemon_lib.executors.ExecutionContext;
import com.liveramp.daemon_lib.executors.JobletExecutor;
import com.liveramp.daemon_lib.executors.processes.execution_conditions.postconfig.ConfigBasedExecutionCondition;
import com.liveramp.daemon_lib.executors.processes.execution_conditions.preconfig.ExecutionCondition;
import com.liveramp.daemon_lib.utils.DaemonException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.withSettings;

public class TestDaemon extends DaemonLibTestCase {
  JobletExecutor<JobletConfig> executor;
  Daemon<JobletConfig> daemon;
  private JobletConfig config;
  private JobletConfigProducer configProducer;
  private ExecutionCondition executionCondition;
  private ConfigBasedExecutionCondition configBasedExecutionCondition;

  @Before
  @SuppressWarnings("unchecked")
  public void setup() {
    this.executor = mock(JobletExecutor.class);
    this.config = mock(JobletConfig.class);
    this.configProducer = mock(JobletConfigProducer.class);
    this.executionCondition = mock(ExecutionCondition.class);
    this.configBasedExecutionCondition = mock(ConfigBasedExecutionCondition.class);
    this.daemon = new Daemon("identifier", executor, configProducer, new JobletCallback.None<>(),
        new NoOpDaemonLock(), mock(DaemonNotifier.class), new Daemon.Options(), executionCondition, configBasedExecutionCondition);
  }

  @Test
  public void executeConfig() throws DaemonException {
    Mockito.when(executionCondition.canExecute()).thenReturn(true);
    Mockito.when(configBasedExecutionCondition.apply(config)).thenReturn(true);
    Mockito.when(configProducer.getNextConfig()).thenReturn(config);

    daemon.markAsRunning();
    daemon.processNext();

    Mockito.verify(executor, times(1)).execute(any(ExecutionContext.class));
  }

  @Test
  public void executionUnavailable() throws DaemonException {
    Mockito.when(executionCondition.canExecute()).thenReturn(false);
    Mockito.when(configProducer.getNextConfig()).thenReturn(config);

    daemon.markAsRunning();
    daemon.processNext();

    Mockito.verify(executor, never()).execute(any(ExecutionContext.class));
  }

  @Test
  public void noNextConfig() throws DaemonException {
    Mockito.when(executionCondition.canExecute()).thenReturn(false);
    Mockito.when(configProducer.getNextConfig()).thenReturn(null);

    daemon.markAsRunning();
    daemon.processNext();

    Mockito.verify(executor, never()).execute(any(ExecutionContext.class));
  }

  @Test
  public void noExecutionIfNotRunning() throws DaemonException {
    Mockito.when(executionCondition.canExecute()).thenReturn(true);
    Mockito.when(configBasedExecutionCondition.apply(config)).thenReturn(true);
    Mockito.when(configProducer.getNextConfig()).thenReturn(config);

    daemon.processNext();

    Mockito.verifyZeroInteractions(executor);
  }
}
