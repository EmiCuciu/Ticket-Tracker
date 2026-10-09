package com.example.demo.controller;

import com.example.demo.security.CurrentUserProvider;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/jobs")
public class JobController {

    private final Scheduler scheduler;
    private final CurrentUserProvider currentUserProvider;

    public JobController(Scheduler scheduler, CurrentUserProvider currentUserProvider) {
        this.scheduler = scheduler;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/ticket-digest/run")
    public ResponseEntity<Void> runTicketDigest() throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("initiatorUserId", currentUserProvider.getCurrentUserId().toString());

        scheduler.triggerJob(JobKey.jobKey("ticketDigestJob"), jobDataMap);
        return ResponseEntity.accepted().build();
    }
}