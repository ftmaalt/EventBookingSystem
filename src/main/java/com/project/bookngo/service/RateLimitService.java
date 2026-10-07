package com.project.bookngo.service;

import com.project.bookngo.config.RateLimitConfig;
import com.project.bookngo.exception.RateLimitExceededException;
import com.project.bookngo.utilities.RequestTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    @Autowired
    private RateLimitConfig rateLimitConfig;



    private final Map<String, RequestTracker> ipRequests = new ConcurrentHashMap<>();
    private final Map<String, RequestTracker> emailRequests = new ConcurrentHashMap<>();


    public void checkRateLimit(String ipAddress, String email) {
        String normalizedIp = normalizeIp(ipAddress);
        String normalizedEmail = normalizeEmail(email);

        if (!allowRequest(ipRequests, ipAddress) || !(allowRequest(emailRequests, email))) {
            throw new RateLimitExceededException("Sorry. You cannot access the system at the moment, please try again in a moment");
        }

    }


    private boolean allowRequest(Map<String, RequestTracker> requests, String key) {
        long now= System.currentTimeMillis();

        RequestTracker tracker = requests.computeIfAbsent(key, k -> createTracker());

        synchronized (tracker){
            removeExpiredRequests(tracker, now);

            if (tracker.getRequestTimes().size()>= rateLimitConfig.getCapacity()){
                return false;
            }
            recordRequest(tracker, now);
            return true;
        }
    }


    private void removeExpiredRequests(RequestTracker tracker, long now) {
        Queue<Long> requestTimes = tracker.getRequestTimes();

        while (!requestTimes.isEmpty()
                && isExpired(requestTimes.peek(), now)) {

            requestTimes.poll();

        }
    }

    private boolean isExpired(long requestTime, long now) {
        return(now - requestTime) >= rateLimitConfig.getLeakRateMs();
    }


    private void recordRequest(RequestTracker tracker, long now) {
        tracker.getRequestTimes().add(now);
    }


    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }


    private String normalizeIp(String ipAddress) {
        if (ipAddress == null) {
            return "";
        }

        return ipAddress.trim();
    }


    private RequestTracker createTracker() {

        return new RequestTracker();


    }
}