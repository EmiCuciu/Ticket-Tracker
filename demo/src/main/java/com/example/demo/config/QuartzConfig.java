package com.example.demo.config;

import com.example.demo.job.CleanupJob;
import com.example.demo.job.TicketDigestJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.TimeZone;

/**
 * Configures Quartz Scheduler for background job execution, using a
 * JDBC-backed, clustered JobStore.
 *
 * <p>Quartz was chosen over Spring's {@code @Scheduled}/{@code @EnableScheduling}
 * for this learning exercise to explore capabilities that simple in-memory
 * scheduling doesn't offer:
 * <ul>
 *   <li>Persistent job/trigger state (JDBC JobStore) surviving app restarts</li>
 *   <li>Cluster-safe execution: a job fires exactly once even when the app
 *       runs on multiple instances (row-locking in the shared store),
 *       unlike {@code @Scheduled} which would fire independently per instance</li>
 *   <li>Explicit misfire-handling policies per trigger</li>
 *   <li>Support for dynamic, programmatic (re)scheduling at runtime</li>
 * </ul>
 *
 */
@Configuration
public class QuartzConfig {

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        return new SpringBeanJobFactory();
    }

    @Bean
    public JobDetail tokenBlacklistCleanupJobDetail() {
        return JobBuilder.newJob(CleanupJob.class)
                .withIdentity("tokenBlacklistCleanupJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger tokenBlacklistCleanupTrigger(JobDetail tokenBlacklistCleanupJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(tokenBlacklistCleanupJobDetail)
                .withIdentity("tokenBlacklistCleanupTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 * * ?")
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public JobDetail ticketDigestJobDetail() {
        return JobBuilder.newJob(TicketDigestJob.class)
                .withIdentity("ticketDigestJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger ticketDigestTrigger(JobDetail ticketDigestJobDetail,
                                       @Value("${app.timezone}") String zone) {
        return TriggerBuilder.newTrigger()
                .forJob(ticketDigestJobDetail)
                .withIdentity("ticketDigestTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 * * ?")
                        .inTimeZone(TimeZone.getTimeZone(zone))
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}