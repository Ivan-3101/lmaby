package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;

public class CreatDbTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("listener invoked");

        if(delegateTask.getVariable("dbuser")!=null){
            System.out.println("setting variable");

            // delegateTask.setVariable("userActivity", "{\r\n  \"user\":\""+delegateTask.getVariable("dbuser")+"\",\r\n  \"id\":\""+delegateTask.getId()+"\",\r\n  \"action\":\"Claim\"\r\n}");


            delegateTask.removeVariable("dbuser");

            delegateTask.setVariable("status", "Pending with DB");



        }

    }

}
