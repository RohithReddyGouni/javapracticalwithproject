package com.rohith.javaprojects.airbnb.service;

import com.rohith.javaprojects.airbnb.dto.HotelOverviewDTO;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.rohith.javaprojects.airbnb.repository.HotelRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class HotelOverviewService {

    private final HotelRepository hotelRepository;
    private final HotelOverviewQueries overviewQueries;
    private final Executor executor;
    private final Semaphore slots;

    public HotelOverviewService(
            HotelRepository hotelRepository,
            HotelOverviewQueries overviewQueries,
            @Qualifier("hotelOverviewExecutor") Executor executor,
            @Value("${app.overview.max-concurrent:4}")
            int maxConcurrent) {

        if (maxConcurrent < 1) {
            throw new IllegalArgumentException(
                    "Overview concurrency must be positive");
        }

        this.hotelRepository = hotelRepository;
        this.overviewQueries = overviewQueries;
        this.executor = executor;
        this.slots = new Semaphore(maxConcurrent);
    }

    public CompletableFuture<HotelOverviewDTO> getOverview(
            Long ownerId,
            Long hotelId) {

        if (!slots.tryAcquire()) {
            throw new TaskRejectedException(
                    "Hotel overview capacity is full");
        }

        List<CompletableFuture<?>> submitted =
                new ArrayList<>(2);

        CompletableFuture<Long> categories;
        CompletableFuture<Long> bookings;

        try {
            if (!hotelRepository.existsByIdAndOwner_Id(
                    hotelId,
                    ownerId)) {

                throw new ResourceNotFoundException(
                        "Hotel not found with ID: " + hotelId);
            }

            categories = CompletableFuture.supplyAsync(
                    () -> overviewQueries.countRoomCategories(
                            ownerId,
                            hotelId),
                    executor);

            submitted.add(categories);

            bookings = CompletableFuture.supplyAsync(
                    () -> overviewQueries.countConfirmedBookings(
                            ownerId,
                            hotelId),
                    executor);

            submitted.add(bookings);

        } catch (RuntimeException | Error exception) {
            releaseWhenFinished(submitted);
            throw exception;
        }

        releaseWhenFinished(submitted);

        return categories
                .thenCombine(
                        bookings,
                        (categoryCount, bookingCount) ->
                                new HotelOverviewDTO(
                                        hotelId,
                                        categoryCount,
                                        bookingCount))
                .orTimeout(3, TimeUnit.SECONDS);
    }

    private void releaseWhenFinished(
            List<CompletableFuture<?>> submitted) {

        CompletableFuture.allOf(
                        submitted.toArray(
                                new CompletableFuture<?>[0]))
                .whenComplete(
                        (ignored, exception) -> slots.release());
    }
}