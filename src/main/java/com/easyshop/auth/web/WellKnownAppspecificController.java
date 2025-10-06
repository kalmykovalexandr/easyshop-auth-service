package com.easyshop.auth.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/.well-known")
public class WellKnownAppspecificController {

    private final List<String> allowedOrigins;

    public WellKnownAppspecificController(
            @Value("${easyshop.auth.allowed-origins:http://localhost:5173}") String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(WellKnownAppspecificController::normalizeOrigin)
                .collect(Collectors.toList());
    }

    @GetMapping("/appspecific/{file}")
    public ResponseEntity<Void> handleAppspecific(
            @PathVariable("file") String file,
            @RequestParam(name = "continue", required = false) String continueUrl
    ) {
        if (!StringUtils.hasText(continueUrl)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            URI target = new URI(continueUrl);
            if (!target.isAbsolute() || !isAllowed(target)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
        } catch (URISyntaxException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean isAllowed(URI target) {
        String origin = normalizeOrigin(target.getScheme() + "://" + target.getAuthority());
        return allowedOrigins.contains(origin);
    }

    private static String normalizeOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return origin;
        }
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
