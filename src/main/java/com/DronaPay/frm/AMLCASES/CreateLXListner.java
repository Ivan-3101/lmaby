package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;

public class CreateLXListner implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {

        if (delegateTask.getName().equals("Review Case By L2")) {
            delegateTask.setVariable("status", "Escalated to L2");
        }

        if (delegateTask.getName().equals("Review Case By L3")) {
            delegateTask.setVariable("status", "Escalated to L3");

        }
        if (delegateTask.getName().equals("Review Case By L4")) {
            delegateTask.setVariable("status", "Escalated to L4");

        }

        if (delegateTask.getName().equals("Review Case By L5")) {
            delegateTask.setVariable("status", "Escalated to L5");

        }

    }

}
