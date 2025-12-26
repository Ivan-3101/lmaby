package com.DronaPay.frm.AMLCASES;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class GenerateIDAndWorkflowNameQc implements JavaDelegate {

    protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateIDAndWorkflowName.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.debug("entered class " + GenerateIDAndWorkflowName.class + " method execute");

        try {
            execution.setVariable("TicketID",  getGeneratedID(execution));
            LOGGER.info("ticket id generated successfully");
        } catch (Exception e) {
            LOGGER.error("Error : " + e + "\nParam : " + execution);
            throw e;
        }
        execution.setVariable("WorkflowName", "QC");


        LOGGER.debug("exiting class " + GenerateIDAndWorkflowName.class + " method execute");

    }

    public long getGeneratedID(DelegateExecution execution) throws Exception {
        LOGGER.debug("entered class " + GenerateIDAndWorkflowName.class + " method getGeneratedID");

        long myId = 0;
        Connection conn = execution.getProcessEngine().getProcessEngineConfiguration().getDataSource().getConnection();
        try {
            PreparedStatement insertStatement=conn.prepareStatement("INSERT INTO ui.ticketidgenerator(processinstanceid)VALUES (?);");
            insertStatement.setString(1,execution.getProcessInstanceId());
            insertStatement.executeUpdate();
            PreparedStatement selectStatement=conn.prepareStatement("Select ticketid from ui.ticketidgenerator where processinstanceid = ? ;");
            selectStatement.setString(1, execution.getProcessInstanceId());

            ResultSet rs = selectStatement.executeQuery();
            if (rs.next())
                myId = rs.getLong(1);
            else
                throw new NullPointerException("Failed to retrieve id");
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("Error : " + e );
            throw e;
        } finally {
            conn.close();
        }
        LOGGER.debug("exiting class " + GenerateIDAndWorkflowName.class + " method getGeneratedID");
        return myId;
    }
}

