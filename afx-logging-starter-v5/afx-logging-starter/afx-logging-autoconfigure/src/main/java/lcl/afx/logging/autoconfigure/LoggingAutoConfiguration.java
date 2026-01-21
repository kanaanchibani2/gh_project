package lcl.afx.logging.autoconfigure;

import lcl.afx.logging.aspect.*;
import lcl.afx.logging.masking.DataMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration pour l'enchaînement logique des logs.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "afx.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataMasker dataMasker() {
        return new DataMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "afx.logging.flow", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogFlowAspect logFlowAspect() {
        return new LogFlowAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "afx.logging.validation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogValidationAspect logValidationAspect() {
        return new LogValidationAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "afx.logging.database", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogDatabaseAspect logDatabaseAspect() {
        return new LogDatabaseAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "afx.logging.cics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogCicsAspect logCicsAspect() {
        return new LogCicsAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "afx.logging.api", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogApiAspect logApiAspect() {
        return new LogApiAspect();
    }
}
