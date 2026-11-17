package com.bookngo.complianceservice.controller;

import com.bookngo.complianceservice.dto.response.GenericMessageResponse;
import com.bookngo.complianceservice.dto.response.PenaltyResponse;
import com.bookngo.complianceservice.service.PenaltiesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalties")
public class PenaltiesController {

    private final PenaltiesService penaltiesService;

    public PenaltiesController(PenaltiesService penaltiesService) {
        this.penaltiesService = penaltiesService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<PenaltyResponse>> getMine() {
        return ResponseEntity.ok(penaltiesService.getMine());
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<GenericMessageResponse> payPenalty(@PathVariable Long id) {
        return ResponseEntity.ok(penaltiesService.payPenalty(id));
    }
}
