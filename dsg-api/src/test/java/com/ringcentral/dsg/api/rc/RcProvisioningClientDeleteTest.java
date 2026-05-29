package com.ringcentral.dsg.api.rc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class RcProvisioningClientDeleteTest {

    private HttpServer server;
    private String capturedContentType;
    private String capturedMethod;
    private String capturedPath;
    private String capturedQuery;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            capturedMethod = exchange.getRequestMethod();
            capturedPath = exchange.getRequestURI().getPath();
            capturedQuery = exchange.getRequestURI().getQuery();
            capturedContentType = exchange.getRequestHeaders().getFirst("Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void deleteExtensionSendsDeleteWithoutContentTypeHeader() {
        int port = server.getAddress().getPort();
        RcOAuthProperties properties = new RcOAuthProperties();
        properties.setServerUrl("http://localhost:" + port);
        RcProvisioningClient client = new RcProvisioningClient(new RestTemplate(), properties, new ObjectMapper());

        client.deleteExtension("test-token", "5175553020", true);

        assertEquals("DELETE", capturedMethod);
        assertEquals("/restapi/v1.0/account/~/extension/5175553020", capturedPath);
        assertEquals("savePhoneLines=true", capturedQuery);
        assertNull(capturedContentType);
    }
}
