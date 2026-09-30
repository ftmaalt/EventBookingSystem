package com.project.bookngo.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
        @GetMapping("/hello")
        public String hello(){
            System.out.println("BookNGo ...");
            return "All Good so far!";
        }
    }

