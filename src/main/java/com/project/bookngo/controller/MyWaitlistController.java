package com.project.bookngo.controller;

import com.project.bookngo.model.response.WaitlistedSessionResponse;
import com.project.bookngo.service.WaitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class MyWaitlistController {
    private final WaitingService waitingService;

    @GetMapping("/me")
    public Page<WaitlistedSessionResponse> getMyWaitlistedSessions(
            @PageableDefault(size = 10, sort = {"joinedAt", "entryId"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return waitingService.getMyWaitlistedSessions(pageable);
    }
}
