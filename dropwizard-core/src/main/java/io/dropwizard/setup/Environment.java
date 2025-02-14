package io.dropwizard.setup;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.health.HealthEnvironment;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jersey.setup.JerseyServletContainer;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.validation.InjectValidatorFeature;

import javax.annotation.Nullable;
import javax.servlet.Servlet;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.Objects.requireNonNull;

/**
 * A Dropwizard application's environment.
 */
public class Environment {
    private final String name;
    private final MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry;

    private final ObjectMapper objectMapper;
    private Validator validator;

    private final JerseyContainerHolder jerseyServletContainer;
    private final JerseyEnvironment jerseyEnvironment;

    private final MutableServletContextHandler servletContext;
    private final ServletEnvironment servletEnvironment;

    private final LifecycleEnvironment lifecycleEnvironment;

    private final MutableServletContextHandler adminContext;
    private final AdminEnvironment adminEnvironment;

    private final HealthEnvironment healthEnvironment;

    private final ExecutorService healthCheckExecutorService;

    /**
     * Creates a new environment.
     *
     * @param name                the name of the application
     * @param objectMapper the {@link ObjectMapper} for the application
     */
    public Environment(String name,
                       ObjectMapper objectMapper,
                       ValidatorFactory validatorFactory,
                       MetricRegistry metricRegistry,
                       @Nullable ClassLoader classLoader,
                       HealthCheckRegistry healthCheckRegistry,
                       Configuration configuration) {
        this.name = name;
        this.objectMapper = objectMapper;
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = healthCheckRegistry;
        this.validator = validatorFactory.getValidator();

        this.servletContext = new MutableServletContextHandler();
        servletContext.setClassLoader(classLoader);
        this.servletEnvironment = new ServletEnvironment(servletContext);

        this.adminContext = new MutableServletContextHandler();
        adminContext.setClassLoader(classLoader);

        final AdminFactory adminFactory = configuration.getAdminFactory();
        this.adminEnvironment = new AdminEnvironment(adminContext, healthCheckRegistry, metricRegistry, adminFactory);

        this.healthEnvironment = new HealthEnvironment(healthCheckRegistry);

        this.lifecycleEnvironment = new LifecycleEnvironment(metricRegistry);

        final DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(metricRegistry);
        jerseyConfig.setContextPath(servletContext.getContextPath());

        this.jerseyServletContainer = new JerseyContainerHolder(new JerseyServletContainer(jerseyConfig));

        final JerseyEnvironment jerseyEnvironment = new JerseyEnvironment(jerseyServletContainer, jerseyConfig);
        jerseyEnvironment.register(new InjectValidatorFeature(validatorFactory));
        this.jerseyEnvironment = jerseyEnvironment;

        final HealthCheckConfiguration healthCheckConfig = adminFactory.getHealthChecks();
        this.healthCheckExecutorService = this.lifecycle().executorService("TimeBoundHealthCheck-pool-%d")
                .workQueue(new ArrayBlockingQueue<>(healthCheckConfig.getWorkQueueSize()))
                .minThreads(healthCheckConfig.getMinThreads())
                .maxThreads(healthCheckConfig.getMaxThreads())
                .threadFactory(r -> {
                    final Thread thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setDaemon(true);
                    return thread;
                })
                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build();

        // Set the default metric registry to the one in this environment, if
        // the default isn't already set. If a default is already registered,
        // ignore the exception.
        try {
            SharedMetricRegistries.setDefault("default", metricRegistry);
        } catch (IllegalStateException ignored) {
        }

        try {
            SharedHealthCheckRegistries.setDefault("default", healthCheckRegistry);
        } catch (IllegalStateException ignored) {
        }
    }

    /**
     * Creates an environment with the system classloader, default object mapper, default validator factory,
     * default health check registry, and default configuration for tests.
     *
     * @since 2.0
     */
    public Environment(String name) {
        this(name, Jackson.newObjectMapper(), Validators.newValidatorFactory(), new MetricRegistry(), ClassLoader.getSystemClassLoader(), new HealthCheckRegistry(), new Configuration());
    }

    /**
     * Returns the application's {@link JerseyEnvironment}.
     */
    public JerseyEnvironment jersey() {
        return jerseyEnvironment;
    }

    /**
     * Returns an {@link ExecutorService} to run time bound health checks
     */
    public ExecutorService getHealthCheckExecutorService() {
        return healthCheckExecutorService;
    }

    /**
     * Returns the application's {@link AdminEnvironment}.
     */
    public AdminEnvironment admin() {
        return adminEnvironment;
    }

    /**
     * Returns the application's {@link LifecycleEnvironment}.
     */
    public LifecycleEnvironment lifecycle() {
        return lifecycleEnvironment;
    }

    /**
     * Returns the application's {@link ServletEnvironment}.
     */
    public ServletEnvironment servlets() {
        return servletEnvironment;
    }

    /**
     * Returns the application's {@link HealthEnvironment}.
     */
    public HealthEnvironment health() {
        return healthEnvironment;
    }

    /**
     * Returns the application's {@link ObjectMapper}.
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Returns the application's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the application's {@link Validator}.
     */
    public Validator getValidator() {
        return validator;
    }

    /**
     * Sets the application's {@link Validator}.
     */
    public void setValidator(Validator validator) {
        this.validator = requireNonNull(validator);
    }

    /**
     * Returns the application's {@link MetricRegistry}.
     */
    public MetricRegistry metrics() {
        return metricRegistry;
    }

    /**
     * Returns the application's {@link HealthCheckRegistry}.
     */
    public HealthCheckRegistry healthChecks() {
        return healthCheckRegistry;
    }

    /*
    * Internal Accessors
    */

    public MutableServletContextHandler getApplicationContext() {
        return servletContext;
    }

    @Nullable
    public Servlet getJerseyServletContainer() {
        return jerseyServletContainer.getContainer();
    }

    public MutableServletContextHandler getAdminContext() {
        return adminContext;
    }
}
