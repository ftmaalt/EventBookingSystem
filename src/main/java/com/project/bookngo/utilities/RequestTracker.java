package com.project.bookngo.utilities;

import java.util.LinkedList;
import java.util.Queue;

public class RequestTracker {

    private final Queue<Long> requestTimes = new LinkedList<>();


    public Queue<Long> getRequestTimes() {
        return requestTimes;

    }


    public void addRequest(long timestamp) {

        requestTimes.add(timestamp);

    }


    public int getRequestCount() {

        return requestTimes.size();

    }


    public long getOldestRequest() {

        return requestTimes.peek();

    }


    public boolean isEmpty() {

        return requestTimes.isEmpty();

    }


    public void removeOldestRequest() {

        requestTimes.remove();

    }
}