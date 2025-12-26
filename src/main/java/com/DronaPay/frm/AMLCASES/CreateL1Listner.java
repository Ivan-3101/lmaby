package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;

public class CreateL1Listner implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (delegateTask.getVariable("Action3") != null) {
            if (delegateTask.getVariable("Action3").equals("yes")) {
                delegateTask.setVariable("status", "Query Raised");
            }
        }
    }

}
