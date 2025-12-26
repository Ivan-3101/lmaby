package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;

public class CreateL5Listner implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setVariable("status", "Escalated to L5");

    }

}
