package com.DronaPay.frm.AMLCASES;

import java.io.IOException;
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
import org.cibseven.bpm.engine.IdentityService;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;
import org.cibseven.bpm.engine.identity.User;
import org.cibseven.bpm.engine.impl.context.Context;
import org.cibseven.bpm.engine.task.Comment;
import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;
import spinjar.com.fasterxml.jackson.core.JsonProcessingException;
import spinjar.com.fasterxml.jackson.databind.JsonNode;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class AssignmentMail implements TaskListener {

    @Override

    public void notify(DelegateTask delegateTask) {
        log.info("Assignment listner called");
        log.info(delegateTask.getAssignee());

        log.info(delegateTask.getEventName());
        Boolean sendEmail = true;
        Boolean updateUserAct = true;
        Properties props = new Properties();
        try {
            props = TenantPropertiesUtil.getTenantProps(delegateTask.getTenantId());
            if (props.getProperty("email.enable") != null && props.getProperty("email.enable").equals("false")) {
                log.debug("Sending emails is disabled");
                sendEmail = false;
            }
        } catch (IOException e) {
            log.error("Cannot load application.properties");
        }

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

        if (delegateTask.getName().equals("Review Case By L1/ L2") && delegateTask.getVariable("Action") == null) {
            log.debug("Autoallocated to L1/L2 first time, no email sent");
            sendEmail = false;
        }

        if (delegateTask.getAssignee() != null) {
            IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();

            if (delegateTask.getVariable("userActivity") != null) {
                // send email only if assignee has not unclaimed or claimed
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode userActivity;
                try {
                    userActivity = objectMapper.readTree(delegateTask.getVariable("userActivity").toString());
                    log.debug("userActivity : " + userActivity);
                    if (userActivity.get("user").asText().equals(delegateTask.getAssignee())
                            && (userActivity.get("action").asText().equalsIgnoreCase("claim"))) {
                        log.debug("This user has claimed, email not sent");
                        updateUserAct = false;
                        sendEmail = false;
                    }
                    if (userActivity.get("action").asText().contains("Reassigned")
                            && userActivity.get("action").asText().contains(delegateTask.getAssignee())) {
                        log.debug("This user has been reassigned, email not sent");
                        updateUserAct = false;
                        sendEmail = false;
                    }
                } catch (JsonProcessingException e) {
                    log.error("Error " + e);
                }

            }

            if (updateUserAct) {
                log.debug("updating user activty as claim status");
                delegateTask.setVariable("userActivity", "{\r\n  \"user\":\"" + delegateTask.getAssignee()
                        + "\",\r\n  \"id\":\"" + delegateTask.getId() + "\",\r\n  \"action\":\"Claim\"\r\n}");
            }
            if (sendEmail) {
                User user = identityService.createUserQuery().userId(delegateTask.getAssignee().toString())
                        .singleResult();
                if (user != null) {
                    log.info(user.getEmail());

                    String email = user.getEmail();
                    String comment = "";

                    TaskService taskService = delegateTask.getProcessEngineServices().getTaskService();
                    List<Comment> comments = taskService
                            .getProcessInstanceComments(delegateTask.getProcessInstanceId());
                    if (comments.size() > 0) {
                        try {
                            log.debug(comments.get(0).getFullMessage());
                            comment = new JSONObject(comments.get(0).getFullMessage().toString()).optString("message");
                        } catch (IndexOutOfBoundsException e) {
                            log.info("comment does not exist");
                        } catch (Exception e) {
                            log.error("Error " + e);
                        }
                    }

                    JSONObject emailBody = new JSONObject();

                    JSONArray toEmail = new JSONArray();
                    toEmail.put(email);
                    emailBody.put("toEmail", toEmail);

                    if (props.getProperty("email.cc") != null) {
                        JSONArray ccEmail = new JSONArray(props.getProperty("email.cc").split(";"));
                        emailBody.put("ccEmail", ccEmail);
                    }

                    if (props.getProperty("email.bcc") != null) {
                        JSONArray bccEmail = new JSONArray(props.getProperty("email.bcc").split(";"));
                        emailBody.put("bccEmail", bccEmail);
                    }
                    JSONObject bodyParams = new JSONObject();
                    bodyParams.put("name", user.getFirstName() + user.getLastName());
                    bodyParams.put("comment", comment);
                    bodyParams.put("ticketId", delegateTask.getVariable("TicketID").toString());
                    emailBody.put("bodyParams", bodyParams);

                    JSONObject subjectParams = new JSONObject();
                    subjectParams.put("ticketId", delegateTask.getVariable("TicketID").toString());
                    emailBody.put("subjectParams", subjectParams);

                    emailBody.put("templateid", 3);
                    log.debug("Request body " + emailBody.toString());
                    try (CloseableHttpClient client = HttpClients.custom().setRetryHandler(retryHandler).build()) {
                        HttpPost httpPost = new HttpPost(
                                props.getProperty("uiserver.url") + "/api/v1/testing/email-service/send-email/tenant-id/"+ delegateTask.getTenantId());
                        httpPost.addHeader("Content-Type", "application/json");
                        if (props.getProperty("uiserver.auth.type").equalsIgnoreCase("apikey")) {
                            httpPost.addHeader("X-API-Key", props.getProperty("uiserver.api.key"));
                        } else {
                            httpPost.addHeader("Authorization", TokenUtil.getToken(delegateTask.getTenantId()));
                        }

                        httpPost.setEntity(new StringEntity(emailBody.toString()));
                        try (CloseableHttpResponse response = client.execute(httpPost)) {

                            log.info("response from uiserver for email status code "
                                    + response.getStatusLine().getStatusCode());
                            String resBody = (response.getEntity() != null) ? EntityUtils.toString(response.getEntity())
                                    : "";
                            log.info("response from uiserver for email body " + resBody);
                        } catch (Exception e) {
                            log.error("Exception while sending email " + e);
                        }
                    } catch (Exception e) {
                        log.error("Exception while sending email " + e);
                    }
                } else {
                    log.error("user does not exist in camunda " + delegateTask.getAssignee());
                }
            }
        }
    }

}
