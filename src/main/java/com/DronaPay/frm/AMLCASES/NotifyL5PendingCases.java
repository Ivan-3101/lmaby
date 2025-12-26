package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NotifyL5PendingCases implements JavaDelegate {

    static final Logger LOGGER = LoggerFactory.getLogger(NotifyL5PendingCases.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(execution.getActivityInstanceId());
        System.out.println("notify l5 about pending cases.");
        System.out.println(execution.getProcessInstanceId());
    }

}
