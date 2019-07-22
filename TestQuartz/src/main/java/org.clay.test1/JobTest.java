package org.clay.test1;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import java.util.Date;

public class JobTest implements Job {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(JobTest.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("JobTest 执行时间: " + new Date());
    }
}