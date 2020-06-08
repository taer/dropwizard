package io.dropwizard.documentation;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class ManagedExecutorApp extends Application<Configuration> {
    private final String nameFormat = "my-executor-%d";
    private final int maxThreads = 4;

    @Override
    public void run(Configuration configuration, Environment environment) {
        ExecutorService executorService = environment.lifecycle()
                .executorService(nameFormat)
                .maxThreads(maxThreads)
                .build();

        ScheduledExecutorService scheduledExecutorService = environment.lifecycle()
                .scheduledExecutorService(nameFormat)
                .build();
    }
}