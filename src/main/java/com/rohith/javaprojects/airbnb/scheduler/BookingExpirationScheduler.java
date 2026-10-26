package com.rohith.javaprojects.airbnb.scheduler;

import com.rohith.javaprojects.airbnb.entity.enums.BookingStatus;
import com.rohith.javaprojects.airbnb.repository.BookingRepository;
import com.rohith.javaprojects.airbnb.service.BookingExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class BookingExpirationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BookingRepository bookingRepository;
    private final BookingExpirationService expirationService;


    public BookingExpirationScheduler(BookingRepository bookingRepository, BookingExpirationService expirationService) {
        this.bookingRepository = bookingRepository;
        this.expirationService = expirationService;
    }

    @Scheduled(
            scheduler = "bookingExpiryScheduler",
            fixedDelayString = "${app.booking.expiration-delay-ms:30000}")
    public void expireReservations() {

        List<Long> bookingIds =
                bookingRepository.findExpirationCandidates(
                        BookingStatus.RESERVED,
                        Instant.now(),
                        PageRequest.of(0,100));

        for (Long bookingId : bookingIds) {
            try {
                expirationService.expireReservation(bookingId);
            } catch(Exception exception) {
                log.error(
                        "Failed to expire booking {}",
                        bookingId,
                        exception);
            }
        }

    }
}
