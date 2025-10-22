package com.easyshop.auth.job;

import com.easyshop.auth.repository.OtpStateRepository;
import com.easyshop.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class UnverifiedUserCleanupJob {

    private final UserRepository userRepository;
    private final OtpStateRepository otpStateRepository;
    private final long retentionHours;

    // TODO move unverified-retention-hours to config file
    public UnverifiedUserCleanupJob(UserRepository userRepository,
                                    OtpStateRepository otpStateRepository,
                                    @Value("${easyshop.auth.unverified-retention-hours:24}") long retentionHours) {
        this.userRepository = userRepository;
        this.otpStateRepository = otpStateRepository;
        this.retentionHours = retentionHours > 0 ? retentionHours : 24L;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${easyshop.auth.cleanup.interval-ms:3600000}",
               initialDelayString = "${easyshop.auth.cleanup.initial-delay-ms:60000}")
    public void removeStaleUnverifiedUsers() {
        // TODO move cleanup.interval-ms and cleanup.initial-delay-ms into config file
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);

        List<String> emails = userRepository.findEmailsOfUnverifiedOlderThan(cutoff);

        int removed = userRepository.deleteUnverifiedOlderThan(cutoff);

        for (String email : emails) {
            try {
                otpStateRepository.delete(email);
            } catch (Exception ignored) {
            }
        }

        if (removed > 0) {
            log.info("Cleanup: removed {} unverified users and purged {} OTP keys (>{}h).",
                    removed, emails.size(), retentionHours);
        } else {
            log.debug("Cleanup: no unverified users older than {}h to remove.", retentionHours);
        }
    }
}
