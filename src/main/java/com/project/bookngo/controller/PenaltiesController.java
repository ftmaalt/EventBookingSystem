package com.project.bookngo.controller;

import com.project.bookngo.model.response.GenericMessageResponse;
import com.project.bookngo.service.PenaltiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/penalties")
public class PenaltiesController {
    @Autowired
    private PenaltiesService penaltiesService;

    @PutMapping("/{id}/pay")
    public ResponseEntity<GenericMessageResponse> payPenalty(@PathVariable Long id) {
        return ResponseEntity.ok(penaltiesService.payPenalty(id));
    }
}
