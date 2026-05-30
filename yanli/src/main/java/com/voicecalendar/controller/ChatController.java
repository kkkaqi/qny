package com.voicecalendar.controller;

import com.voicecalendar.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(chatService.getRoles());
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body, HttpSession session) {
        String message = body.get("message");
        String roleId = body.getOrDefault("roleId", "default");
        String rolePrompt = body.getOrDefault("rolePrompt", "");

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reply", "请输入消息"));
        }

        try {
            String reply = chatService.send(getUserId(session), roleId, message, rolePrompt);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("reply", "抱歉，出了点问题：" + e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> body, HttpSession session) {
        String roleId = body.getOrDefault("roleId", "default");
        chatService.reset(getUserId(session), roleId);
        return ResponseEntity.ok(Map.of("message", "对话已清空"));
    }

    /** AI 总结 */
    @PostMapping("/summary")
    public ResponseEntity<?> summary(@RequestBody Map<String, String> body, HttpSession session) {
        String period = body.getOrDefault("period", "week");
        String date = body.getOrDefault("date", java.time.LocalDate.now().toString());
        try {
            String summary = chatService.summarize(getUserId(session), period, date);
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("summary", "生成失败"));
        }
    }
}
