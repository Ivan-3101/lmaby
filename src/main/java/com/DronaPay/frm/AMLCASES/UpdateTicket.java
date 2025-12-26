package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpdateTicket implements JavaDelegate {

    static final Logger LOGGER = LoggerFactory.getLogger(UpdateTicket.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(execution.getActivityInstanceId());
        System.out.println("update ticket service class called");
        System.out.println(execution.getProcessInstanceId());
    }

}
