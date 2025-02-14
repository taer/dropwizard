package io.dropwizard.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.NullLevelFilterFactory;
import io.dropwizard.logging.layout.DropwizardLayoutFactory;
import io.dropwizard.validation.BaseValidator;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class AppenderFactoryCustomTimeZoneTest {

    static {
        BootstrapLogging.bootstrap();
    }

    private final ConfigurationSourceProvider configurationSourceProvider = new ResourceConfigurationSourceProvider();

    @SuppressWarnings("rawtypes")
    private final YamlConfigurationFactory<ConsoleAppenderFactory> factory = new YamlConfigurationFactory<>(
        ConsoleAppenderFactory.class, BaseValidator.newValidator(), Jackson.newObjectMapper(), "dw");

    @Test
    void testLoadAppenderWithTimeZoneInFullFormat() throws Exception {
        final ConsoleAppenderFactory<?> appender = factory.build(configurationSourceProvider, "yaml/appender_with_time_zone_in_full_format.yml");
        assertThat(appender.getTimeZone().getID()).isEqualTo("America/Los_Angeles");
    }

    @Test
    void testLoadAppenderWithTimeZoneInCustomFormat() throws Exception {
        final ConsoleAppenderFactory<?> appender = factory.build(configurationSourceProvider, "yaml/appender_with_custom_time_zone_format.yml");
        assertThat(appender.getTimeZone().getID()).isEqualTo("GMT-02:00");
    }

    @Test
    void testLoadAppenderWithNoTimeZone() throws Exception {
        final ConsoleAppenderFactory<?> appender = factory.build(configurationSourceProvider, "yaml/appender_with_no_time_zone.yml");
        assertThat(appender.getTimeZone().getID()).isEqualTo("UTC");
    }

    @Test
    void testLoadAppenderWithUtcTimeZone() throws Exception {
        final ConsoleAppenderFactory<?> appender = factory.build(configurationSourceProvider, "yaml/appender_with_utc_time_zone.yml");
        assertThat(appender.getTimeZone().getID()).isEqualTo("UTC");
    }

    @Test
    void testLoadAppenderWithWrongTimeZone() throws Exception {
        final ConsoleAppenderFactory<?> appender = factory.build(new ResourceConfigurationSourceProvider(), "yaml/appender_with_wrong_time_zone.yml");
        assertThat(appender.getTimeZone().getID()).isEqualTo("GMT");
    }

    @Test
    void testLoadAppenderWithSystemTimeZone() throws Exception {
        final ConsoleAppenderFactory<?> appender = factory.build(configurationSourceProvider, "yaml/appender_with_system_time_zone.yml");
        assertThat(appender.getTimeZone()).isEqualTo(TimeZone.getDefault());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBuildAppenderWithTimeZonePlaceholderInLogFormat() throws Exception {
        AsyncAppender appender = (AsyncAppender) factory.build(configurationSourceProvider, "yaml/appender_with_time_zone_placeholder.yml")
            .build(new LoggerContext(), "test-custom-time-zone", new DropwizardLayoutFactory(),
                new NullLevelFilterFactory<>(), new AsyncLoggingEventAppenderFactory());
        ConsoleAppender<?> consoleAppender = (ConsoleAppender<?>) appender.getAppender("console-appender");
        LayoutWrappingEncoder<?> encoder = (LayoutWrappingEncoder<?>) consoleAppender.getEncoder();
        PatternLayoutBase<?> layout = (PatternLayoutBase<?>) encoder.getLayout();
        assertThat(layout.getPattern()).isEqualTo("custom format with UTC");
    }

}
