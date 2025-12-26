package com.DronaPay.frm.AMLCASES;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class APIServices {

    private String dia_agent_url;
    private String dia_agent_username;
    private String dia_agent_password;

    public APIServices(String tenantid) throws IOException {
        Properties props = TenantPropertiesUtil.getTenantProps(tenantid);
        this.dia_agent_url = props.getProperty("dia.agent.url");
        this.dia_agent_username = props.getProperty("dia.agent.username");
        this.dia_agent_password = props.getProperty("dia.agent.password");
    }

    public CloseableHttpResponse callDIAAgent(String body) throws IOException, URISyntaxException {
        log.info("Call DIA agent API Called");

        CloseableHttpClient client = HttpClients.createDefault();
        URI uri = new URI(this.dia_agent_url);
        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader("Content-Type", "application/json");

        String auth = this.dia_agent_username + ":" + this.dia_agent_password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;
        httpPost.setHeader("Authorization", authHeader);

        httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        CloseableHttpResponse response = client.execute(httpPost);

        return response;
    }
}