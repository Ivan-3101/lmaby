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
            // 1. Determine which Agent to call (Default to aml-agent1 if not set)
            String agentId = (String) execution.getVariable("targetAgentID");
            if (agentId == null || agentId.isEmpty()) {
                agentId = "aml-agent1";
                log.info("targetAgentID not found, defaulting to: " + agentId);
            } else {
                log.info("Determined Target Agent ID from DMN: " + agentId);
            }

            String transactionJson = execution.getVariable("Transaction").toString();
            JSONObject transactionObj = new JSONObject(transactionJson);
            int itenantid = Integer.parseInt(execution.getTenantId());

            // 2. Build base request data
            JSONObject data = new JSONObject();
            data.put("itenantid", itenantid);

            // 3. Conditional Data Construction based on Agent ID
            if ("aml-agent3".equalsIgnoreCase(agentId)) {

                // Agent 3 (Night-time Transaction) requires transaction_timestamp only
                // Reads from txn.txnDateTime in TransactionJson
                Object txnDateTimeObj = transactionObj.optQuery("/txn/txnDateTime");
                if (txnDateTimeObj == null || txnDateTimeObj == JSONObject.NULL) {
                    handleError(execution, "Missing required field for Agent 3: txn/txnDateTime");
                    return;
                }
                data.put("transaction_timestamp", txnDateTimeObj.toString());
                log.info("Prepared data for Agent 3: transaction_timestamp=" + txnDateTimeObj.toString());

            } else {

                // Agents 1, 2, 4 all require iaccountid
                Object accountIdObj = transactionObj.optQuery("/observations/account/iaccountid");
                if (accountIdObj == null || accountIdObj == JSONObject.NULL) {
                    handleError(execution, "Missing required field: iaccountid");
                    return;
                }
                long iaccountid = ((Number) accountIdObj).longValue();
                if (iaccountid == 0) {
                    handleError(execution, "Invalid iaccountid (value is 0)");
                    return;
                }
                data.put("iaccountid", iaccountid);

                if ("aml-agent1".equalsIgnoreCase(agentId)) {

                    // Agent 1 (Merchant Volume Growth) additionally requires ipayeemccid
                    Object mccIdObj = transactionObj.optQuery("/observations/account/imcc");
                    if (mccIdObj == null || mccIdObj == JSONObject.NULL) {
                        handleError(execution, "Missing required field for Agent 1: imcc");
                        return;
                    }
                    long ipayeemccid = ((Number) mccIdObj).longValue();
                    if (ipayeemccid == 0) {
                        handleError(execution, "Invalid ipayeemccid (value is 0)");
                        return;
                    }
                    data.put("ipayeemccid", ipayeemccid);
                    log.info("Prepared data for Agent 1: iaccountid=" + iaccountid + ", ipayeemccid=" + ipayeemccid);

                } else if ("aml-agent4".equalsIgnoreCase(agentId)) {

                    // Agent 4 (High Value Threshold) additionally requires transaction_amount
                    // Reads from payee.amount in TransactionJson (same field used by GenerateIDAndWorkflowName)
                    Object txnAmountObj = transactionObj.optQuery("/payee/amount");
                    if (txnAmountObj == null || txnAmountObj == JSONObject.NULL) {
                        handleError(execution, "Missing required field for Agent 4: payee/amount");
                        return;
                    }
                    double txnAmount = ((Number) txnAmountObj).doubleValue();
                    data.put("transaction_amount", txnAmount);
                    log.info("Prepared data for Agent 4: iaccountid=" + iaccountid + ", transaction_amount=" + txnAmount);

                } else {

                    // Agent 2 (and any future agents) require only iaccountid + itenantid
                    log.info("Prepared data for " + agentId + ": iaccountid=" + iaccountid);

                }
            }

            // 4. Construct Final Request Body
            JSONObject requestBody = new JSONObject();
            requestBody.put("data", data);
            requestBody.put("agentid", agentId);

            log.info("AML Agent request body: " + requestBody.toString());

            // 5. Call API
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
        }
    }

    private void handleError(DelegateExecution execution, String reason) {
        log.error(reason);
        execution.setVariable("agentStatusCode", -1);
        execution.setVariable("agentDecision", "ERROR");
        execution.setVariable("agentReason", reason);
    }
}