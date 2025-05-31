package com.sellsmart.backend.Controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public String welcome() {
        return "SellSmart API is live. Access restricted to authorized clients.";
    }
}

