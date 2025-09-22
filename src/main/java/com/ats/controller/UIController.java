package com.ats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve the React UI
 */
@Controller
public class UIController {

    /**
     * Serve the React app for the root path
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
