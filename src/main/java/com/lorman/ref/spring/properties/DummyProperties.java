package com.lorman.ref.spring.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dummy.client")
@Getter
@Setter
public class DummyProperties {

    /**
     * Base URL host for DummyClient. Default points to localhost.
     */
    private String url = "http://localhost";

    /**
     * Target port for DummyClient. Must be explicitly configured via properties.
     * Default is 8080.
     */
    private int port = 8080;
}
