package com.example.sample.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/converge")
public class ConvergeSaleController {

    @PostMapping("/sale")
    public ResponseEntity<Map<String, Object>> sale(@RequestBody Map<String, String> form) {
        // Simulate legacy Converge XML flow
        return ResponseEntity.ok(Map.of(
                "ssl_result", "0",
                "ssl_approved", "APPROVED",
                "ssl_approval_code", "123456",
                "ssl_txn_id", "T-001"
        ));
    }
}


