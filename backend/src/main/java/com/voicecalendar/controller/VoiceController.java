package com.voicecalendar.controller;

import com.voicecalendar.llm.LlmProvider;
import com.voicecalendar.model.dto.VoiceCommandResult;
import com.voicecalendar.service.VoiceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;
    private final LlmProvider llmProvider;

    private Long getUserId(HttpSession session) { return (Long) session.getAttribute("userId"); }

    @PostMapping("/audio")
    public ResponseEntity<VoiceCommandResult> processAudio(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            byte[] audioData = file.getBytes();
            VoiceCommandResult result = voiceService.processAudio(getUserId(session), audioData, "wav", 16000);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("语音处理失败", e);
            return ResponseEntity.ok(VoiceCommandResult.builder().intent("UNKNOWN").success(false).errorMessage("处理失败: " + e.getMessage()).build());
        }
    }

    @PostMapping("/text")
    public ResponseEntity<VoiceCommandResult> processText(@RequestBody Map<String, String> body, HttpSession session) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.ok(VoiceCommandResult.builder().intent("UNKNOWN").success(false).errorMessage("请输入内容").build());
        }
        return ResponseEntity.ok(voiceService.processText(getUserId(session), text));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "available", true,
                "message", "语音服务正常",
                "llmProvider", llmProvider.getProviderName(),
                "llmAvailable", llmProvider.isAvailable()
        ));
    }
}
