package com.ats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
    
    /**
     * Serve the React app for all other paths (except API routes)
     */
    @GetMapping("/{path:^(?!api).*}")
    public String serveUI(@PathVariable String path) {
        return "forward:/index.html";
    }
}
