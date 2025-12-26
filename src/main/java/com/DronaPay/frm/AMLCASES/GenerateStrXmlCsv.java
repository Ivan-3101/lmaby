package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.cibseven.bpm.engine.delegate.JavaDelegate;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GenerateStrXmlCsv implements JavaDelegate {

    static final Logger LOGGER = LoggerFactory.getLogger(GenerateStrXmlCsv.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("Generating str xml csv.");
        execution.setVariable("status", "STR Generated");

    }

}
