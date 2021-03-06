package com.level11data.databricks.job.run;

import com.level11data.databricks.client.HttpException;
import com.level11data.databricks.cluster.Cluster;

public interface JobRun {

    RunState getRunState() throws HttpException;

    String getSparkContextId() throws HttpException;

    Long getSetupDuration() throws HttpException;

    Long getExecutionDuration() throws HttpException;

    Long getCleanupDuration() throws HttpException;

    Cluster getCluster() throws JobRunException;
}
