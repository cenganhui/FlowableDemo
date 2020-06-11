package com.example.demo;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class SendRejectionMail implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("发送拒绝邮件给 " + delegateExecution.getVariable("employee") + " ，不准请假。");
    }
}
