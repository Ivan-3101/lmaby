package com.DronaPay.frm.AMLCASES;

import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;

public class CreatBranchTaskListener implements TaskListener{

    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println("listner invoked");

        if(delegateTask.getVariable("branchuser")!=null){
            System.out.println("setting variable");

            // delegateTask.setVariable("userActivity", "{\r\n  \"user\":\""+delegateTask.getVariable("branchuser")+"\",\r\n  \"id\":\""+delegateTask.getId()+"\",\r\n  \"action\":\"Claim\"\r\n}");
            delegateTask.removeVariable("branchuser");

            delegateTask.setVariable("status", "Pending with Branch");
        }



    }

}
