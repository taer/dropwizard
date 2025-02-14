package io.dropwizard.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.TestPatternLayoutFactory.TestPatternLayout;
import io.dropwizard.logging.async.AsyncLoggingEventAppenderFactory;
import io.dropwizard.logging.filter.NullLevelFilterFactory;
import io.dropwizard.logging.layout.DropwizardLayoutFactory;
import io.dropwizard.validation.BaseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class AppenderFactoryCustomLayoutTest {

    static {
        BootstrapLogging.bootstrap();
    }

    private final ObjectMapper objectMapper = Jackson.newObjectMapper();
    @SuppressWarnings("rawtypes")
    private final YamlConfigurationFactory<ConsoleAppenderFactory> factory = new YamlConfigurationFactory<>(
        ConsoleAppenderFactory.class, BaseValidator.newValidator(), objectMapper, "dw-layout");

    @BeforeEach
    void setUp() throws Exception {
        objectMapper.registerSubtypes(TestLayoutFactory.class);
        objectMapper.registerSubtypes(TestPatternLayoutFactory.class);
    }

    @Test
    void testLoadAppenderWithCustomLayout() throws Exception {
        final ConsoleAppenderFactory<ILoggingEvent> appender = factory
            .build(new ResourceConfigurationSourceProvider(), "yaml/appender_with_custom_layout.yml");
        assertThat(appender.getLayout()).isNotNull().isInstanceOf(TestLayoutFactory.class);
        TestLayoutFactory layoutFactory = (TestLayoutFactory) appender.getLayout();
        assertThat(layoutFactory).isNotNull().extracting(TestLayoutFactory::isIncludeSeparator).isEqualTo(true);
    }

    @Test
    void testBuildAppenderWithCustomLayout() throws Exception {
        ConsoleAppender<?> consoleAppender = buildAppender("yaml/appender_with_custom_layout.yml");
        LayoutWrappingEncoder<?> encoder = (LayoutWrappingEncoder<?>) consoleAppender.getEncoder();
        assertThat(encoder.getLayout()).isInstanceOf(TestLayoutFactory.TestLayout.class);
    }

    @Test
    void testBuildAppenderWithCustomPatternLayoutAndFormat() throws Exception {
        ConsoleAppender<?> consoleAppender = buildAppender("yaml/appender_with_custom_layout_and_format.yml");
        LayoutWrappingEncoder<?> encoder = (LayoutWrappingEncoder<?>) consoleAppender.getEncoder();
        TestPatternLayout layout = (TestPatternLayout) encoder.getLayout();
        assertThat(layout.getPattern()).isEqualTo("custom pattern");
    }

    private ConsoleAppender<?> buildAppender(String resourceName) throws Exception {
        AsyncAppender appender = (AsyncAppender) factory.build(new ResourceConfigurationSourceProvider(), resourceName)
            .build(new LoggerContext(), "test-custom-layout", new DropwizardLayoutFactory(),
                new NullLevelFilterFactory<>(), new AsyncLoggingEventAppenderFactory());
        return (ConsoleAppender<?>) appender.getAppender("console-appender");
    }
}
