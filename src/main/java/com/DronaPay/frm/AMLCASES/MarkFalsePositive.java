package com.DronaPay.frm.AMLCASES;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringEscapeUtils;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkFalsePositive  implements JavaDelegate {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MarkFalsePositive.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.debug("entered class " + MarkFalsePositive.class + " method execute");
        Properties props = TenantPropertiesUtil.getTenantProps(execution.getTenantId());
        List<String> reqidList = new LinkedList<String>();
        String transaction = (String) execution.getVariable("Transaction").toString();
        try {
            JSONObject transJSON = new JSONObject(transaction);
            System.out.println("Req id is " + transJSON.getString("reqid"));
            reqidList.add(transJSON.getString("reqid"));
        } catch (JSONException e) {
            JSONArray transJSONArr = new JSONArray(transaction);
            for(int i = 0; i < transJSONArr.length(); i++) {
                JSONObject transJSON = new JSONObject(transJSONArr.get(i).toString());
                System.out.println("Req id is " + transJSON.getString("reqid"));
                reqidList.add(transJSON.getString("reqid"));
            }
        }
        String remark = "";

        if(execution.getVariable("Remarks") != null)
            remark = execution.getVariable("Remarks").toString();

        Connection conn = execution.getProcessEngine().getProcessEngineConfiguration().getDataSource().getConnection();
        remark = StringEscapeUtils.escapeJava(remark);
        String jsonquery = "select CONCAT ('{\"reason\": \"',?,'\", \"Date\" : \"',(SELECT NOW() ), '\"}')";

        System.out.println("first query to be executed after escaping is " + jsonquery);
        String markfalsepositive = "";
        try {

            PreparedStatement stmt = conn.prepareStatement(jsonquery);
            stmt.setString(1, remark);
            ResultSet r = stmt.executeQuery();

            r.next();
            jsonquery = r.getString("concat");
            // jsonquery = StringEscapeUtils.escapeJava(jsonquery);
            // System.out.println("json query output of first query after escaping " + jsonquery);
            LOGGER.debug("json format query executed : "+jsonquery);

            StringBuilder sbReq = new StringBuilder();
            for(int i = 0; i < reqidList.size(); i++) {
                sbReq.append(reqidList.get(i));
                if(i != (reqidList.size() - 1)) {
                    sbReq.append(",");
                }
            }

            Integer itenantid = Integer.parseInt(execution.getTenantId());

            markfalsepositive = "CALL analytics.override_batch_risk(cast(? as varchar[]), 1::SMALLINT, cast(? as jsonb), cast(? as integer))";
            PreparedStatement preparedStatement = conn.prepareStatement(markfalsepositive);
            preparedStatement.setString(1, "{" + sbReq + "}");
            preparedStatement.setString(2, jsonquery);
            preparedStatement.setInt(3, itenantid);
            preparedStatement.executeUpdate();


        } catch (SQLException e) {
            LOGGER.error("Error : " + e + "\nParam : " +
                    "database url : " + props.getProperty("jdbc.analytics.url")
                    + "database username : " + props.getProperty("jdbc.analytics.username")
                    + "database password : " + props.getProperty("jdbc.analytics.password"));
            throw e;
        }
        finally {
            conn.close();
        }
        LOGGER.info("Executed script " + markfalsepositive);
        LOGGER.debug("exiting class " + MarkFalsePositive.class + " method execute");
    }
}