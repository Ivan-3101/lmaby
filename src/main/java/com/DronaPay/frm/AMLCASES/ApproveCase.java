package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ApproveCase implements JavaDelegate {

    static final Logger LOGGER = LoggerFactory.getLogger(ApproveCase.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("Approving case.");
        execution.setVariable("status", "STR Approved");
    }

}
