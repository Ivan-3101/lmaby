package com.DronaPay.frm.AMLCASES;

import java.util.Base64;
import java.util.Properties;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateQc implements JavaDelegate {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CreateQc.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        LOGGER.info("Create QC service Called");
        execution.setVariable("status", "False Positive");
        Properties props = TenantPropertiesUtil.getTenantProps(execution.getTenantId());
        String res = props.getProperty("camunda.aml.username") + ":"
                + props.getProperty("camunda.aml.password");

        String response = "Basic " + Base64.getEncoder()
                .encodeToString(res.getBytes());

        String transaction = execution.getVariable("Transaction").toString();
        String result = (String) execution.getVariable("Result").toString();

        JSONObject ticketBody = new JSONObject();
        JSONObject variableBody = new JSONObject();
        JSONObject transactionValueNTypeBody = new JSONObject();
        transactionValueNTypeBody.put("value", transaction);
        transactionValueNTypeBody.put("type", "json");
        JSONObject resultValueNTypeBody = new JSONObject();
        resultValueNTypeBody.put("value", result);
        resultValueNTypeBody.put("type", "json");

        JSONObject parentprocessInstanceId = new JSONObject();
        parentprocessInstanceId.put("value",
                "{\n  \"Id\":\"" + execution.getProcessInstanceId() + "\",\n  \"defId\":\""
                        + execution.getProcessDefinitionId() + "\"\n}");
        parentprocessInstanceId.put("type", "string");
        JSONObject amlTicketId = new JSONObject();
        amlTicketId.put("value", execution.getVariable("TicketID"));
        amlTicketId.put("type", "string");

        Object vpa = execution.getVariable("batchvpa");
        Object account = execution.getVariable("batchaccount");
        Object customer = execution.getVariable("batchcustomer");

        if(vpa != null) {
            JSONObject vpaBody = new JSONObject();
            vpaBody.put("value", vpa.toString());
            vpaBody.put("type", "string");
            variableBody.put("batchvpa", vpaBody);
        }

        if(account != null) {
            JSONObject accountBody = new JSONObject();
            accountBody.put("value", account.toString());
            accountBody.put("type", "string");
            variableBody.put("batchaccount", accountBody);
        }

        if(customer != null) {
            JSONObject customerBody = new JSONObject();
            customerBody.put("value", customer.toString());
            customerBody.put("type", "string");
            variableBody.put("batchcustomer", customerBody);
        }

        variableBody.put("Transaction", transactionValueNTypeBody);
        variableBody.put("Result", resultValueNTypeBody);
        variableBody.put("parentProcess", parentprocessInstanceId);
        variableBody.put("AMLTicketID", amlTicketId);

        String transactionType = (String) execution.getVariable("TransactionType");

        if (transactionType == null) {
            transactionType = "";
        }

        JSONObject transactionTypebody = new JSONObject();
        transactionTypebody.put("value", transactionType);
        transactionTypebody.put("type", "String");

        String basedon = (String) execution.getVariable("basedon");

        JSONObject basedonbody = new JSONObject();
        basedonbody.put("value", basedon);
        basedonbody.put("type", "String");

        String payeeAccountNum = (String) execution.getVariable("account");

        JSONObject addressBody = new JSONObject();
        addressBody.put("value", payeeAccountNum);
        addressBody.put("type", "String");

        if (transactionType.equalsIgnoreCase("batch"))
            variableBody.put("TransactionType", transactionTypebody);
        if (transactionType.equalsIgnoreCase("batch"))
            variableBody.put("account", addressBody);
        if (basedon != null)
            variableBody.put("basedon", basedonbody);
        if (execution.getVariable("payeeName") != null) {
            JSONObject payeeName = new JSONObject();
            payeeName.put("value", execution.getVariable("payeeName"));
            payeeName.put("type", "String");
            variableBody.put("payeeName", payeeName);
        }

        if (execution.getVariable("triggeredtype") != null) {
            JSONObject triggeredType = new JSONObject();
            triggeredType.put("value", execution.getVariable("triggeredtype"));
            triggeredType.put("type", "String");
            variableBody.put("triggeredtype", triggeredType);
        }

        if (execution.getVariable("AvgRiskScore") != null) {

            JSONObject avgRiskScore = new JSONObject();
            avgRiskScore.put("value", execution.getVariable("AvgRiskScore"));
            avgRiskScore.put("type", "long");
            variableBody.put("AvgRiskScore", avgRiskScore);

        }
        if (execution.getVariable("ListOfScores") != null) {

            JSONObject listOFScore = new JSONObject();
            listOFScore.put("value", execution.getVariable("ListOfScores"));
            listOFScore.put("type", "string");
            variableBody.put("ListOfScores", listOFScore);

        }

        if (execution.getVariable("failedRules") != null) {

            JSONObject failedRules = new JSONObject();
            failedRules.put("value", execution.getVariable("failedRules"));
            failedRules.put("type", "string");
            variableBody.put("failedRules", failedRules);

        }
        ticketBody.put("variables", variableBody);
        ticketBody.put("businessKey", System.currentTimeMillis());

        LOGGER.info("Create ticket API Call Starting wit body ....");

        LOGGER.info(ticketBody.toString());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(
                    props.getProperty("camunda.url")
                            + "/engine-rest/process-definition/key/Qc/tenant-id/"+execution.getTenantId()+"/start");
            httpPost.addHeader("Content-Type", "application/json");

            httpPost.addHeader("Authorization", response);
            httpPost.setEntity(new StringEntity(ticketBody.toString()));
            try (CloseableHttpResponse cresponse = client.execute(httpPost)) {

                String resBody = (cresponse.getEntity() != null)
                        ? EntityUtils.toString(cresponse.getEntity())
                        : "";

                LOGGER.info("API Call Completed");
                LOGGER.info("Create Qc API Response " + resBody);
                LOGGER.info("Create Qc API status " + cresponse.getStatusLine().getStatusCode());
            } catch (Exception e) {
                LOGGER.error("Exception while creating ticket " + e);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while creating ticket " + e);
        }
    }

}
