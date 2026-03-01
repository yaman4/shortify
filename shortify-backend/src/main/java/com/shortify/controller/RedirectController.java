package com.shortify.controller;

import com.shortify.service.RedirectService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    /**
     * Handles short URL redirection.
     *
     * @param shortCode the short URL code
     * @param response  HTTP response for redirection
     */
    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) {
        try {
            String originalUrl = redirectService.resolve(shortCode);
            response.sendRedirect(originalUrl); // 302 redirect
        } catch (RuntimeException e) {
            // If short URL not found, return 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short URL not found");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Redirection failed");
        }
    }
}
