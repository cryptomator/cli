import ch.qos.logback.classic.spi.Configurator;
import org.cryptomator.cli.LogbackConfigurator;

open module org.cryptomator.cli {
    requires org.cryptomator.cryptofs;
    requires org.cryptomator.frontend.fuse;
    requires info.picocli;
    requires org.slf4j;
    requires org.cryptomator.integrations.api;
    requires org.fusesource.jansi;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;

    provides Configurator with LogbackConfigurator;
}