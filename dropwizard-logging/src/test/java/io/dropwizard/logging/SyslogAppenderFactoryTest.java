package io.dropwizard.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.NullLevelFilterFactory;
import io.dropwizard.logging.layout.DropwizardLayoutFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SyslogAppenderFactoryTest {

    static {
        BootstrapLogging.bootstrap();
    }

    @Test
    void isDiscoverable() throws Exception {
        assertThat(new DiscoverableSubtypeResolver().getDiscoveredSubtypes())
                .contains(SyslogAppenderFactory.class);
    }

    @Test
    void defaultIncludesAppName() throws Exception {
        assertThat(new SyslogAppenderFactory().getLogFormat())
                .contains("%app");
    }

    @Test
    void defaultIncludesPid() throws Exception {
        assertThat(new SyslogAppenderFactory().getLogFormat())
                .contains("%pid");
    }

    @Test
    void patternIncludesAppNameAndPid() throws Exception {
        final AsyncAppender wrapper = (AsyncAppender) new SyslogAppenderFactory()
                .build(new LoggerContext(), "MyApplication", new DropwizardLayoutFactory(), new NullLevelFilterFactory<>(), new AsyncLoggingEventAppenderFactory());
        assertThat(((SyslogAppender) wrapper.getAppender("syslog-appender")).getSuffixPattern())
                .matches("^MyApplication\\[\\d+\\].+");
    }

    @Test
    void stackTracePatternCanBeSet() throws Exception {
        final SyslogAppenderFactory syslogAppenderFactory = new SyslogAppenderFactory();
        syslogAppenderFactory.setStackTracePrefix("--->");
        final AsyncAppender wrapper = (AsyncAppender) syslogAppenderFactory
                .build(new LoggerContext(), "MyApplication", new DropwizardLayoutFactory(), new NullLevelFilterFactory<>(), new AsyncLoggingEventAppenderFactory());
        assertThat(((SyslogAppender) wrapper.getAppender("syslog-appender"))
                .getStackTracePattern()).isEqualTo("--->");
    }

    @Test
    void appenderContextIsSet() throws Exception {
        final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        final SyslogAppenderFactory appenderFactory = new SyslogAppenderFactory();
        final Appender<ILoggingEvent> appender = appenderFactory.build(root.getLoggerContext(), "test", new DropwizardLayoutFactory(),
            new NullLevelFilterFactory<>(), new AsyncLoggingEventAppenderFactory());

        assertThat(appender.getContext()).isEqualTo(root.getLoggerContext());
    }

    @Test
    void appenderNameIsSet() throws Exception {
        final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        final SyslogAppenderFactory appenderFactory = new SyslogAppenderFactory();
        final Appender<ILoggingEvent> appender = appenderFactory.build(root.getLoggerContext(), "test", new DropwizardLayoutFactory(),
            new NullLevelFilterFactory<>(), new AsyncLoggingEventAppenderFactory());

        assertThat(appender.getName()).isEqualTo("async-syslog-appender");
    }

    @Test
    void syslogFacilityTest() {
        for (SyslogAppenderFactory.Facility facility : SyslogAppenderFactory.Facility.values()) {
            assertThat(SyslogAppender.facilityStringToint(facility.toString().toLowerCase(Locale.ENGLISH)))
                .isNotNegative();
        }
    }
}
