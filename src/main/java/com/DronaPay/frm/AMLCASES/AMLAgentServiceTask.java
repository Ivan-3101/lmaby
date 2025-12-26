package com.DronaPay.frm.AMLCASES;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AMLAgentServiceTask implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("AML Agent Service Task called for ticket id: " + execution.getVariable("TicketID"));

        // Build request body
        JSONObject requestBody = new JSONObject();
        JSONObject data = new JSONObject();

        // Hardcoded values for testing - will be dynamic later
        data.put("iaccountid", 1964362);
        data.put("ipayeemccid", 1001);
        data.put("itenantid", 8);

        requestBody.put("data", data);
        requestBody.put("agentid", "aml-agent1");

        log.info("AML Agent request body: " + requestBody.toString());

        // Call the DIA agent
        APIServices apiServices = new APIServices(execution.getTenantId());
        CloseableHttpResponse response = apiServices.callDIAAgent(requestBody.toString());

        String resp = EntityUtils.toString(response.getEntity());
        int statusCode = response.getStatusLine().getStatusCode();

        log.info("AML Agent API status: " + statusCode);
        log.info("AML Agent API response: " + resp);

        // Store response in process variables
        execution.setVariable("agentStatusCode", statusCode);
        execution.setVariable("agentResponse", resp);

        if (statusCode == 200) {
            JSONObject responseObj = new JSONObject(resp);
            execution.setVariable("agentDecision", responseObj.optString("decision"));
            execution.setVariable("agentReason", responseObj.optString("reason"));
            log.info("Agent Decision: " + responseObj.optString("decision"));
            log.info("Agent Reason: " + responseObj.optString("reason"));
        } else {
            log.error("AML Agent API call failed with status: " + statusCode);
        }
    }
}