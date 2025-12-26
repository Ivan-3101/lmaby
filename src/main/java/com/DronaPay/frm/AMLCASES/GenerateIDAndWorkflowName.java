package com.DronaPay.frm.AMLCASES;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spinjar.com.fasterxml.jackson.databind.JsonNode;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

public class GenerateIDAndWorkflowName implements JavaDelegate {

    protected static final Logger LOGGER = LoggerFactory.getLogger(
            GenerateIDAndWorkflowName.class
    );

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.debug(
                "entered class " + GenerateIDAndWorkflowName.class + " method execute"
        );

        execution.setVariable("level1level2Asignee", "not-defined");
        execution.setVariable("level2Asignee", "not-defined");
        execution.setVariable("level3Asignee", "not-defined");
        execution.setVariable("level4Asignee", "not-defined");
        execution.setVariable("level5Asignee", "not-defined");
        execution.setVariable("itAsignee", "not-defined");

        try {
            execution.setVariable("TicketID", getGeneratedID(execution));
            LOGGER.info("ticket id generated successfully");
        } catch (Exception e) {
            LOGGER.error("Error : " + e + "\nParam : " + execution);
            throw e;
        }
        execution.setVariable("WorkflowName", "AML Cases");
        execution.setVariable("WorkflowKey", "AMLCases");
        String result = execution.getVariable("Result").toString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(result);

        String transactionType = (String) execution.getVariable("TransactionType");

        if (transactionType == null) {
            transactionType = "";
        }

        LOGGER.info("Transaction Type : " + transactionType);

        String transaction = execution.getVariable("Transaction").toString();
        List<String> failedRules = new ArrayList<>();
        List<String> alertIDs = new ArrayList<>();
        String alert = "";

        Boolean isManual = false;
        if (
                execution.getVariable("isCreatedManually") != null &&
                        (Boolean) execution.getVariable("isCreatedManually")
        ) {
            isManual = true;
        }

        if (isManual) {
            JSONArray jsonArray = new JSONArray(
                    execution.getVariable("manualAlerts").toString()
            );
            alert = jsonArray.getJSONObject(0).getString("label");
            Long highRisk = jsonArray.getJSONObject(0).getLong("score");
            if (jsonArray.getJSONObject(0).getLong("score") > 0) {
                failedRules.add(jsonArray.getJSONObject(0).getString("label"));
                execution.setVariable(
                        jsonArray.getJSONObject(0).getString("label"),
                        jsonArray.getJSONObject(0).getString("label")
                );
                String alertId =
                        rootNode.get("reqid").asText() +
                                jsonArray.getJSONObject(0).optInt("value");
                alertIDs.add(alertId);
            }
            for (int i = 1; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).getLong("score") > highRisk) {
                    highRisk = jsonArray.getJSONObject(i).getLong("score");
                    alert = jsonArray.getJSONObject(i).getString("label");
                } else if (
                        jsonArray.getJSONObject(i).getLong("score") == highRisk &&
                                jsonArray.getJSONObject(i).getLong("score") > 0
                ) {
                    alert = alert + " " + jsonArray.getJSONObject(i).getString("label");
                }

                if (jsonArray.getJSONObject(i).getLong("score") > 0) {
                    String alertId =
                            rootNode.get("reqid").asText() +
                                    jsonArray.getJSONObject(i).optInt("value");
                    alertIDs.add(alertId);
                    execution.setVariable(
                            jsonArray.getJSONObject(i).getString("label"),
                            jsonArray.getJSONObject(i).getString("label")
                    );
                    failedRules.add(jsonArray.getJSONObject(i).getString("label"));
                }
            }
        } else {
            JSONArray jsonArray = new JSONArray(
                    rootNode.get("score").get("decisiondetails").toString()
            );
            alert = jsonArray.getJSONObject(0).getString("rulename");
            Long highRisk = jsonArray.getJSONObject(0).getLong("score");
            if (jsonArray.getJSONObject(0).getLong("score") > 0) {
                failedRules.add(jsonArray.getJSONObject(0).getString("rulename"));
                execution.setVariable(
                        jsonArray.getJSONObject(0).getString("rulename"),
                        jsonArray.getJSONObject(0).getString("rulename")
                );
                String alertId =
                        rootNode.get("reqid").asText() +
                                jsonArray.getJSONObject(0).optInt("ruleid");
                alertIDs.add(alertId);
            }
            for (int i = 1; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).getLong("score") > highRisk) {
                    highRisk = jsonArray.getJSONObject(i).getLong("score");
                    alert = jsonArray.getJSONObject(i).getString("rulename");
                } else if (
                        jsonArray.getJSONObject(i).getLong("score") == highRisk &&
                                jsonArray.getJSONObject(i).getLong("score") > 0
                ) {
                    alert =
                            alert + " " + jsonArray.getJSONObject(i).getString("rulename");
                }

                if (jsonArray.getJSONObject(i).getLong("score") > 0) {
                    String alertId =
                            rootNode.get("reqid").asText() +
                                    jsonArray.getJSONObject(i).optInt("ruleid");
                    alertIDs.add(alertId);
                    execution.setVariable(
                            jsonArray.getJSONObject(i).getString("rulename"),
                            jsonArray.getJSONObject(i).getString("rulename")
                    );
                    failedRules.add(jsonArray.getJSONObject(i).getString("rulename"));
                }
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String body = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(failedRules);
        execution.setVariable("failedRules", body);
        execution.setVariable("Alert", alert);

        ObjectMapper mappertrans = new ObjectMapper();
        JsonNode rootNodeTrans = mappertrans.readTree(transaction);

        Double transactionAmount = null;
        String payer = null;
        String payee = null;
        try {
            if (!transactionType.equalsIgnoreCase("batch")) {
                transactionAmount = rootNodeTrans.get("payee").get("amount").asDouble();

                if (rootNodeTrans.get("observations").get("payeeVPA") != null) {
                    payee =
                            rootNodeTrans
                                    .get("observations")
                                    .get("payeeVPA")
                                    .get("externalId")
                                    .asText();
                }

                if (rootNodeTrans.get("observations").get("payerVPA") != null) {
                    payer =
                            rootNodeTrans
                                    .get("observations")
                                    .get("payerVPA")
                                    .get("externalId")
                                    .asText();
                }

                if (
                        rootNodeTrans.get("observations").get("payeeVPA").get("account") !=
                                null
                ) {
                    if (
                            rootNodeTrans
                                    .get("observations")
                                    .get("payeeVPA")
                                    .get("account")
                                    .get("externalId") !=
                                    null
                    ) {
                        String payeeAccountNum = rootNodeTrans
                                .get("observations")
                                .get("payeeVPA")
                                .get("account")
                                .get("externalId")
                                .asText();
                        execution.setVariable("payeeAccount", payeeAccountNum);

                        try {
                            execution.setVariable(
                                    "payeeCustId",
                                    rootNodeTrans
                                            .get("observations")
                                            .get("payeeVPA")
                                            .get("account")
                                            .get("customer")
                                            .get("externalId")
                                            .asText()
                            );
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                }

                if (
                        rootNodeTrans.get("observations").get("payerVPA").get("account") !=
                                null
                ) {
                    if (
                            rootNodeTrans
                                    .get("observations")
                                    .get("payerVPA")
                                    .get("account")
                                    .get("externalId") !=
                                    null
                    ) {
                        String payerAccountNum = rootNodeTrans
                                .get("observations")
                                .get("payerVPA")
                                .get("account")
                                .get("externalId")
                                .asText();
                        execution.setVariable("payerAccount", payerAccountNum);
                        try {
                            execution.setVariable(
                                    "payeeCustId",
                                    rootNodeTrans
                                            .get("observations")
                                            .get("payerVPA")
                                            .get("account")
                                            .get("customer")
                                            .get("externalId")
                                            .asText()
                            );
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                }

                if (rootNodeTrans.get("txn") != null) {
                    if (rootNodeTrans.get("txn").get("class") != null) {
                        execution.setVariable(
                                "class",
                                rootNodeTrans.get("txn").get("class").asText()
                        );
                    }
                }
            }

            if (rootNodeTrans.get("txn") != null) {
                if (rootNodeTrans.get("txn").get("class") != null) {
                    execution.setVariable(
                            "class",
                            rootNodeTrans.get("txn").get("class").asText()
                    );
                }
            }

            if (execution.getVariable("basedon") != null) {

                try {
                    String vpa = rootNodeTrans.get("observations").get("vpa")
                            .get("vcexternaladdressid").asText();
                    execution.setVariable("batchvpa", vpa);
                }
                catch (NullPointerException e)
                {
                    LOGGER.info("failed to find vpa observations.vpa.vcexternaladdressid");
                }
                catch (Exception e) {
                    LOGGER.info("failed to find vpa vcexternaladdressid", e);
                }

                try
                {
                    String account = rootNodeTrans.get("observations").get("account")
                            .get("vcexternalaccountid").asText();
                    execution.setVariable("batchaccount", account);
                } catch (Exception e) {
                    LOGGER.info("failed to find account vcexternalaccountid", e);
                }

                try
                {
                    String customer = rootNodeTrans.get("observations").get("customer")
                            .get("vcexternalcustid").asText();
                    execution.setVariable("batchcustomer", customer);
                } catch (Exception e) {
                    LOGGER.info("failed to find customer vcexternalcustid", e);
                }


                String payeeName = null;

                if (
                        execution
                                .getVariable("basedon")
                                .toString()
                                .equalsIgnoreCase("account")
                ) {
                    JsonNode nameNode = null;
                    try {
                        nameNode =
                                rootNodeTrans
                                        .get("observations")
                                        .get("customer")
                                        .get("vcattribs")
                                        .get("yb_raw")
                                        .get("businessName");
                    } catch (Exception e) {}
                    if (nameNode != null && !nameNode.isNull()) {
                        payeeName = nameNode.asText();
                    }
                } else if (
                        execution.getVariable("basedon").toString().equalsIgnoreCase("vpa")
                ) {
                    JsonNode nameNode = rootNodeTrans
                            .get("observations")
                            .get("vpa")
                            .get("vcaddress");
                    if (nameNode != null && !nameNode.isNull()) {
                        payeeName = nameNode.asText();
                    }
                }
                if (payeeName != null) {
                    execution.setVariable("payeeName", payeeName);
                }
            }

            LOGGER.info("parsed result json");
            if (isManual) {
                execution.setVariable("triggeredtype", "Alert");
            } else {
                if (rootNode.at("/status") != null) {
                    String status = rootNode.at("/status").asText();
                    switch (status) {
                        case "Success":
                        case "Low Risk":
                        case "High Risk":
                            execution.setVariable("triggeredtype", "Alert");
                            break;
                        case "Failed":
                            execution.setVariable("triggeredtype", "Decline");
                            break;
                    }
                }
            }
        } catch (NullPointerException e) {
            // TODO: handle exception
            LOGGER.error("failed to information from transaction is null ", e);
        }

        if (transactionAmount != null) execution.setVariable(
                "TransactionAmount",
                transactionAmount
        );
        if (payer != null) execution.setVariable("payer", payer);
        if (payee != null) execution.setVariable("payee", payee);
        if (alertIDs.size() > 0) {
            execution.setVariable("AlertIDs", String.join(",", alertIDs));
        }
        if (!isManual) {
            execution.setVariable(
                    "RiskScore",
                    rootNode.get("score").get("score").asLong()
            );
        }
        execution.setVariable("status", "Pending");
        declareFilterParams(execution, rootNodeTrans, rootNode);
        LOGGER.debug(
                "exiting class " + GenerateIDAndWorkflowName.class + " method execute"
        );
    }

    public long getGeneratedID(DelegateExecution execution) throws Exception {
        LOGGER.debug(
                "entered class " +
                        GenerateIDAndWorkflowName.class +
                        " method getGeneratedID"
        );

        long myId = 0;
        Connection conn = execution
                .getProcessEngine()
                .getProcessEngineConfiguration()
                .getDataSource()
                .getConnection();
        try {
            PreparedStatement insertStatement = conn.prepareStatement(
                    "INSERT INTO ui.ticketidgenerator(processinstanceid)VALUES (?);"
            );
            insertStatement.setString(1, execution.getProcessInstanceId());
            insertStatement.executeUpdate();
            PreparedStatement selectStatement = conn.prepareStatement(
                    "Select ticketid from ui.ticketidgenerator where processinstanceid = ? ;"
            );
            selectStatement.setString(1, execution.getProcessInstanceId());

            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) myId = rs.getLong(1); else throw new NullPointerException(
                    "Failed to retrieve id"
            );
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("Error : " + e);
            throw e;
        } finally {
            conn.close();
        }
        LOGGER.debug(
                "exiting class " +
                        GenerateIDAndWorkflowName.class +
                        " method getGeneratedID"
        );
        return myId;
    }

    private void castAndDeclare(
            DelegateExecution execution,
            String varName,
            String value,
            String type
    ) {
        switch (type) {
            case "double":
                execution.setVariable(varName, Double.parseDouble(value));
                break;
            case "int":
                execution.setVariable(varName, Integer.parseInt(value));
                break;
            case "string":
                execution.setVariable(varName, (String) value);
                break;
            case "custom_date":
                DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
                Instant instant = Instant.from(formatter.parse(value));
                execution.setVariable(varName, instant.toEpochMilli());
                break;
            default:
                break;
        }
    }

    private void declareFilterParams(
            DelegateExecution execution,
            JsonNode transJsonNode,
            JsonNode resultNode
    ) throws Exception {
        LOGGER.debug(
                "entered class " +
                        GenerateIDAndWorkflowName.class +
                        " method declareFilterParams"
        );
        Connection conn = execution
                .getProcessEngine()
                .getProcessEngineConfiguration()
                .getDataSource()
                .getConnection();

        try {
            PreparedStatement selectStatement = conn.prepareStatement(
                    "Select filterparams from ui.workflowmasters where itenantid = ? and workflowkey = ? ;"
            );
            selectStatement.setInt(1, Integer.parseInt(execution.getTenantId()));
            selectStatement.setString(
                    2,
                    execution.getProcessDefinitionId().split(":")[0]
            );
            LOGGER.debug("Filter params query : " + selectStatement.toString());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()) {
                String filterParams = rs.getString("filterparams");
                LOGGER.debug("filterParams found " + filterParams);
                JSONArray filterArray = new JSONArray(filterParams);
                for (
                        int param_index = 0;
                        param_index < filterArray.length();
                        param_index++
                ) {
                    String extract_type = filterArray
                            .getJSONObject(param_index)
                            .getJSONObject("value_config")
                            .getString("extract_from");
                    try {
                        switch (extract_type) {
                            case "default_value":
                                LOGGER.debug("default Value found ");
                                castAndDeclare(
                                        execution,
                                        filterArray.getJSONObject(param_index).optString("name"),
                                        filterArray
                                                .getJSONObject(param_index)
                                                .getJSONObject("value_config")
                                                .get("value")
                                                .toString(),
                                        filterArray.getJSONObject(param_index).optString("data_type")
                                );
                                break;
                            case "result_json":
                                LOGGER.debug("result node configured Value found ");
                                castAndDeclare(
                                        execution,
                                        filterArray.getJSONObject(param_index).optString("name"),
                                        resultNode
                                                .at(
                                                        filterArray
                                                                .getJSONObject(param_index)
                                                                .getJSONObject("value_config")
                                                                .optString("value")
                                                )
                                                .asText(),
                                        filterArray.getJSONObject(param_index).optString("data_type")
                                );
                                break;
                            case "trans_json":
                                LOGGER.debug("tran node configured Value found ");
                                castAndDeclare(
                                        execution,
                                        filterArray.getJSONObject(param_index).optString("name"),
                                        transJsonNode
                                                .at(
                                                        filterArray
                                                                .getJSONObject(param_index)
                                                                .getJSONObject("value_config")
                                                                .optString("value")
                                                )
                                                .asText(),
                                        filterArray.getJSONObject(param_index).optString("data_type")
                                );
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            } else {
                LOGGER.debug("Filter Params not found");
                conn.close();
            }
        } catch (Exception e) {
            LOGGER.error("Error : " + e);
        } finally {
            conn.close();
        }
    }
}
