package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UpdateCommentsStatus implements JavaDelegate {

    static final Logger LOGGER = LoggerFactory.getLogger(UpdateCommentsStatus.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        System.out.println(execution.getActivityInstanceId());
        System.out.println("update comments status class called");
        System.out.println(execution.getProcessInstanceId());
    }

}
