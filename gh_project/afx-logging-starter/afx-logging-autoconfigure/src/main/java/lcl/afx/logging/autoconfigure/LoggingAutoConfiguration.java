package lcl.afx.logging.autoconfigure;

import lcl.afx.logging.aspect.PaymentLoggingAspect;
import lcl.afx.logging.filter.CorrelationIdFilter;
import lcl.afx.logging.masking.DataMasker;
import lcl.afx.logging.propagation.FeignCorrelationInterceptor;
import lcl.afx.logging.propagation.RestClientCorrelationInterceptor;
import lcl.afx.logging.propagation.RestTemplateCorrelationInterceptor;
import lcl.afx.logging.propagation.WebClientCorrelationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

/**
 * Auto-configuration Spring Boot pour le logging centralisé.
 * 
 * <p>Cette configuration est automatiquement chargée par Spring Boot
 * grâce au fichier META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "afx.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    private final LoggingProperties properties;

    public LoggingAutoConfiguration(LoggingProperties properties) {
        this.properties = properties;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA MASKER
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afx.logging.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataMasker dataMasker() {
        return new DataMasker();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CORRELATION ID FILTER (Servlet)
    // ══════════════════════════════════════════════════════════════════════════

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    @ConditionalOnProperty(prefix = "afx.logging.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public class ServletAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public CorrelationIdFilter correlationIdFilter() {
            CorrelationIdFilter filter = new CorrelationIdFilter();
            filter.setCorrelationIdHeader(properties.getCorrelation().getHeaderName());
            filter.setGenerateIfMissing(properties.getCorrelation().isGenerateIfMissing());
            return filter;
        }

        @Bean
        public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
                CorrelationIdFilter filter) {
            FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.setName("correlationIdFilter");
            return registration;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AOP ASPECT
    // ══════════════════════════════════════════════════════════════════════════

    @Configuration
    @EnableAspectJAutoProxy
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "afx.logging.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
    public class AspectAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PaymentLoggingAspect paymentLoggingAspect(DataMasker dataMasker) {
            PaymentLoggingAspect aspect = new PaymentLoggingAspect(dataMasker);
            aspect.setDefaultPerformanceThresholdMs(properties.getAspect().getPerformanceThresholdMs());
            return aspect;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPAGATION : RestTemplate
    // ══════════════════════════════════════════════════════════════════════════

    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
    @ConditionalOnProperty(prefix = "afx.logging.propagation", name = "rest-template", havingValue = "true", matchIfMissing = true)
    public class RestTemplateAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RestTemplateCorrelationInterceptor restTemplateCorrelationInterceptor() {
            return new RestTemplateCorrelationInterceptor();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPAGATION : RestClient (Spring Boot 3.2+)
    // ══════════════════════════════════════════════════════════════════════════

    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.client.RestClient")
    @ConditionalOnProperty(prefix = "afx.logging.propagation", name = "rest-client", havingValue = "true", matchIfMissing = true)
    public class RestClientAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RestClientCorrelationInterceptor restClientCorrelationInterceptor() {
            return new RestClientCorrelationInterceptor();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPAGATION : WebClient (Reactive)
    // ══════════════════════════════════════════════════════════════════════════

    @Configuration
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    @ConditionalOnProperty(prefix = "afx.logging.propagation", name = "web-client", havingValue = "true", matchIfMissing = true)
    public class WebClientAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebClientCorrelationFilter webClientCorrelationFilter() {
            return WebClientCorrelationFilter.create();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROPAGATION : Feign
    // ══════════════════════════════════════════════════════════════════════════

    @Configuration
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnProperty(prefix = "afx.logging.propagation", name = "feign", havingValue = "true", matchIfMissing = true)
    public class FeignAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public FeignCorrelationInterceptor feignCorrelationInterceptor() {
            return new FeignCorrelationInterceptor();
        }
    }
}
