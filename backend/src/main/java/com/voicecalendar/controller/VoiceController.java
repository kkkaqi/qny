package com.voicecalendar.controller;

import com.voicecalendar.model.dto.VoiceCommandResult;
import com.voicecalendar.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;

    /**
     * 上传音频文件进行语音识别和指令处理
     * 前端录制 WAV/PCM 音频后上传
     */
    @PostMapping("/audio")
    public ResponseEntity<VoiceCommandResult> processAudio(
            @RequestParam("file") MultipartFile file) {
        try {
            byte[] audioData = file.getBytes();
            String contentType = file.getContentType();

            // 根据 Content-Type 判断格式
            String format = "wav";
            if (contentType != null && contentType.contains("pcm")) {
                format = "pcm";
            }

            VoiceCommandResult result = voiceService.processAudio(audioData, format, 16000);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("语音处理失败", e);
            return ResponseEntity.ok(VoiceCommandResult.builder()
                    .intent("UNKNOWN")
                    .success(false)
                    .errorMessage("语音处理失败: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 直接提交文本指令（前端 Web Speech API 降级 / 手动输入）
     * 前端使用浏览器语音识别得到文字后调用此接口
     */
    @PostMapping("/text")
    public ResponseEntity<VoiceCommandResult> processText(
            @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.ok(VoiceCommandResult.builder()
                    .intent("UNKNOWN")
                    .success(false)
                    .errorMessage("请输入语音内容")
                    .build());
        }

        VoiceCommandResult result = voiceService.processText(text);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取 ASR 服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        // 通过检查 /api/voice/text 是否能正常工作来判断
        return ResponseEntity.ok(Map.of(
                "available", true,
                "message", "语音服务运行中。前端可优先使用浏览器Web Speech API，也可上传音频文件。"
        ));
    }
}
