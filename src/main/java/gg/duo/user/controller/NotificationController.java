package gg.duo.user.controller;

import gg.duo.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Map<String, Object> list(Authentication auth) {
        return notificationService.list((Long) auth.getPrincipal());
    }

    @PostMapping("/read-all")
    public void markAllRead(Authentication auth) {
        notificationService.markAllRead((Long) auth.getPrincipal());
    }
}
