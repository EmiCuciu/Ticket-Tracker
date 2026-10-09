package com.example.demo.job;

import com.example.demo.repository.EmailRepository;
import com.example.demo.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@DisallowConcurrentExecution
@RequiredArgsConstructor
public class CleanupJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(CleanupJob.class);
    private final EmailRepository emailRepository;

    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public void execute(JobExecutionContext context) {
        int deleted = tokenBlacklistService.cleanupExpiredTokens();
        if (deleted > 0) {
            log.info("Token blacklist cleanup: deleted {} expired tokens", deleted);
        }

        long deletedEmails = emailRepository.deleteFinishedBefore(
                Instant.now().minus(30, ChronoUnit.DAYS));
        if (deletedEmails > 0) {
            log.info("Email cleanup: deleted {} old emails", deletedEmails);
        }
    }
}