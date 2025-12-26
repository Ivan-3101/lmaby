package com.DronaPay.frm.AMLCASES;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;
import org.cibseven.bpm.engine.history.HistoricTaskInstance;
import org.cibseven.bpm.engine.history.HistoricTaskInstanceQuery;
import org.cibseven.spin.plugin.variable.SpinValues;
import org.cibseven.spin.plugin.variable.value.JsonValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class AutoAllocationServiceTask implements JavaDelegate {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AutoAllocationServiceTask.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.debug("entered class " + AutoAllocationServiceTask.class + " method execute");
        Properties props = TenantPropertiesUtil.getTenantProps(execution.getTenantId());

        String childTaskID = null;
        String parentTaskID = null;
        String nextTaskID = null;
        String nextGroup = null;
        String parentGroup = null;
        String childGroup = null;
        String parentUser = null;
        String childUser = null;
        String group = null;
        String workflowKey = execution.getVariable("WorkflowKey").toString();
        ObjectMapper objectMapper = new ObjectMapper();

        if (execution.getVariableLocal("childTaskID") != null) {
            childTaskID = execution.getVariableLocal("childTaskID").toString();
        }
        if (execution.getVariableLocal("parentTaskID") != null) {
            parentTaskID = execution.getVariableLocal("parentTaskID").toString();
        }
        if (execution.getVariableLocal("parentGroup") != null) {
            parentGroup = execution.getVariableLocal("parentGroup").toString();
        }
        if (execution.getVariableLocal("childGroup") != null) {
            childGroup = execution.getVariableLocal("childGroup").toString();
        }
        if (execution.getVariableLocal("nextTaskID") != null) {
            nextTaskID = execution.getVariableLocal("nextTaskID").toString();
        }
        if (execution.getVariableLocal("nextGroup") != null) {
            nextGroup = execution.getVariableLocal("nextGroup").toString();
        }
        if (execution.getVariableLocal("group") != null) {
            group = execution.getVariableLocal("group").toString();
        }
        LOGGER.debug("parentGroup " + parentGroup + "parentTaskID " + parentTaskID);

        int retryMailInterval = Integer.parseInt(props.getProperty("email.retry.interval.sec"));

        // retry handler
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                // Retry up to 3 times
                if (executionCount > 3) {
                    return false;
                }

                // Retry on IOException
                if (exception instanceof IOException) {
                    try {
                        Thread.sleep(1000 * retryMailInterval);
                    } catch (InterruptedException e) {
                        return true;
                    }
                    log.debug("Retry to send mail ,retry count " + executionCount);
                    return true;
                }

                return false;
            }
        };

        GetUserMappingRequest getUserMappingRequest = new GetUserMappingRequest();
        getUserMappingRequest.setWorkflowKey(workflowKey);
        JSONArray usersJsonIntial = new JSONArray();
        JSONObject emptyUser=new JSONObject();
        emptyUser.putOpt("username", JSONObject.NULL);
        emptyUser.putOpt("noOfTasksOfThisProcess", 0);
        emptyUser.putOpt("group", nextGroup);
        usersJsonIntial.put(emptyUser);
        JsonValue usersIntial = SpinValues.jsonValue(usersJsonIntial.toString()).create();
        execution.setVariable("users", usersIntial);

        String tenantid = execution.getTenantId();
        if (group != null) {
            // 1st scenario
            getUserMappingRequest.setChildUserGroupID(
                    Arrays.stream(group.split(","))
                            .map(String::trim)
                            .map(s -> s + "_"+tenantid)
                            .toList()
            );
            execution.setVariable("taskGroup", group);
        } else {
            String processId = execution.getProcessInstanceId();
            HistoricTaskInstanceQuery historyQuery = execution.getProcessEngineServices().getHistoryService()
                    .createHistoricTaskInstanceQuery().processInstanceId(processId);
            List<HistoricTaskInstance> taskList = historyQuery.orderByHistoricTaskInstanceEndTime().desc().list();
            // check if 3rd scenario can be applicable
            // search in all history if this task was previously executed
            String username = null;
            for (HistoricTaskInstance inst : taskList) {
                if (inst.getTaskDefinitionKey().equals(nextTaskID)) {
                    username = inst.getAssignee();
                    break;
                }
            }
            if (username != null) {
                JSONArray usersJson = new JSONArray();
                usersJson.put(new JSONObject().put("username", username).put("group", nextGroup)
                        .put("noOfTasksOfThisProcess", 0));
                LOGGER.debug("3rd scenario, users: " + usersJson.toString());
                JsonValue users = SpinValues.jsonValue(usersJson.toString()).create();
                execution.setVariable("users", users);
                execution.setVariable("taskGroup", nextGroup);
                LOGGER.debug("exiting class " + AutoAllocationServiceTask.class + " method execute");
                return;
            }


            // 2nd scenario
            HistoricTaskInstance previousTask = taskList.get(0);
            if (previousTask.getTaskDefinitionKey().equals(childTaskID)) {
                childUser = previousTask.getAssignee();
                execution.setVariable("taskGroup", nextGroup);
                getUserMappingRequest.setChildUserGroupID(
                        Arrays.stream(childGroup.split(","))
                                .map(String::trim)
                                .map(s -> s + "_"+tenantid)
                                .toList());
                getUserMappingRequest.setParentUserGroupID(
                        Arrays.stream(parentGroup.split(","))
                                .map(String::trim)
                                .map(s -> s + "_"+tenantid)
                                .toList());
                getUserMappingRequest.setChildUserName(childUser);
            } else if (previousTask.getTaskDefinitionKey().equals(parentTaskID)) {
                parentUser = previousTask.getAssignee();
                execution.setVariable("taskGroup", nextGroup);
                getUserMappingRequest.setChildUserGroupID(
                        Arrays.stream(childGroup.split(","))
                                .map(String::trim)
                                .map(s -> s + "_"+tenantid)
                                .toList()
                        );
                getUserMappingRequest.setParentUserGroupID(
                        Arrays.stream(parentGroup.split(","))
                                .map(String::trim)
                                .map(s -> s + "_"+tenantid)
                                .toList());
                getUserMappingRequest.setParentUserName(parentUser);
            }
        }

        try (CloseableHttpClient clientGetUsers = HttpClients.custom().setRetryHandler(retryHandler).build()) {
            String body = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(getUserMappingRequest);
            LOGGER.debug("Request body UIServer " + body);
            HttpPost httpPost = new HttpPost(
                    props.getProperty("uiserver.url") + "/api/v1/testing/allocation-mapper/get-user-mapping/tenant-id/"
                            + execution.getTenantId());
            httpPost.addHeader("Content-Type", "application/json");
            if (props.getProperty("uiserver.auth.type").equalsIgnoreCase("apikey")) {
                httpPost.addHeader("X-API-Key", props.getProperty("uiserver.api.key"));
            } else {
                httpPost.addHeader("Authorization", TokenUtil.getToken(execution.getTenantId()));
            }
            httpPost.setEntity(new StringEntity(body));
            try (CloseableHttpResponse resGetUsers = clientGetUsers.execute(httpPost)) {
                String resBody = (resGetUsers.getEntity() != null) ? EntityUtils.toString(resGetUsers.getEntity()) : "";
                if (resGetUsers.getStatusLine().getStatusCode() == 200) {
                    LOGGER.debug("UIServer API call returned successfully");
                    LOGGER.debug("1st or 2nd scenario, users" + resBody);
                    JsonValue users = SpinValues.jsonValue(resBody).create();
                    execution.setVariable("users", users);
                } else {
                    LOGGER.error("Error from UIServer API: status " + resGetUsers.getStatusLine().getStatusCode()
                            + " body " + resBody + " for ticket id : " + execution.getVariable("TicketID"));
                }
            } catch (Exception e) {
                LOGGER.error("Error invoking UIServer autoallocation API " + e);
            }
        } catch (Exception e) {
            LOGGER.error("Error invoking UIServer autoallocation API " + e);
        }
        LOGGER.debug("exiting class " + AutoAllocationServiceTask.class + " method execute");
    }
}