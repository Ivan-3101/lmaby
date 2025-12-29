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

        try {
            // Extract values from Transaction variable
            String transactionJson = execution.getVariable("Transaction").toString();
            JSONObject transactionObj = new JSONObject(transactionJson);

            // Extract with validation
            Object accountIdObj = transactionObj.optQuery("/observations/account/iaccountid");
            Object mccIdObj = transactionObj.optQuery("/observations/customer/imcc");

            if (accountIdObj == null || mccIdObj == null) {
                log.error("Missing required fields - iaccountid: " + accountIdObj + ", imcc: " + mccIdObj);
                execution.setVariable("agentStatusCode", -1);
                execution.setVariable("agentDecision", "ERROR");
                execution.setVariable("agentReason", "Missing iaccountid or ipayeemccid in Transaction data");
                return; // Exit gracefully, let workflow continue
            }

            long iaccountid = ((Number) accountIdObj).longValue();
            long ipayeemccid = ((Number) mccIdObj).longValue();
            int itenantid = Integer.parseInt(execution.getTenantId());

            if (iaccountid == 0 || ipayeemccid == 0) {
                log.error("Invalid values - iaccountid: " + iaccountid + ", ipayeemccid: " + ipayeemccid);
                execution.setVariable("agentStatusCode", -1);
                execution.setVariable("agentDecision", "ERROR");
                execution.setVariable("agentReason", "Invalid iaccountid or ipayeemccid (value is 0)");
                return; // Exit gracefully, let workflow continue
            }

            log.info("Extracted values - iaccountid: " + iaccountid + ", ipayeemccid: " + ipayeemccid + ", itenantid: " + itenantid);

            // Build request body
            JSONObject requestBody = new JSONObject();
            JSONObject data = new JSONObject();

            data.put("iaccountid", iaccountid);
            data.put("ipayeemccid", ipayeemccid);
            data.put("itenantid", itenantid);

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
                execution.setVariable("agentDecision", "ERROR");
                execution.setVariable("agentReason", "Agent API returned status: " + statusCode);
            }

        } catch (Exception e) {
            log.error("Error in AML Agent Service Task: " + e.getMessage(), e);
            execution.setVariable("agentStatusCode", -1);
            execution.setVariable("agentDecision", "ERROR");
            execution.setVariable("agentReason", "Exception: " + e.getMessage());
            // Don't throw - let workflow continue
        }
    }
}