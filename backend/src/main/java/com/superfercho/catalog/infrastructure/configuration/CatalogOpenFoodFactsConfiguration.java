package com.superfercho.catalog.infrastructure.configuration;

import com.superfercho.catalog.application.port.ProductBarcodeLookupPort;
import com.superfercho.catalog.application.usecase.LookupProductByBarcodeUseCase;
import com.superfercho.catalog.infrastructure.integration.openfoodfacts.OpenFoodFactsProductLookupAdapter;
import com.superfercho.catalog.infrastructure.integration.openfoodfacts.OpenFoodFactsProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenFoodFactsProperties.class)
public class CatalogOpenFoodFactsConfiguration {

    @Bean
    @Profile("!test")
    ProductBarcodeLookupPort productBarcodeLookupPort(
            RestClient.Builder restClientBuilder, OpenFoodFactsProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return new OpenFoodFactsProductLookupAdapter(
                restClientBuilder.requestFactory(requestFactory).build(), properties);
    }

    @Bean
    @Profile("!test")
    LookupProductByBarcodeUseCase lookupProductByBarcodeUseCase(
            ProductBarcodeLookupPort productBarcodeLookupPort) {
        return new LookupProductByBarcodeUseCase(productBarcodeLookupPort);
    }
}
