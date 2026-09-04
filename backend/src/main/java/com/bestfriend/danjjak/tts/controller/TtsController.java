package com.bestfriend.danjjak.tts.controller;

import com.bestfriend.danjjak.tts.dto.TtsDtos.TtsRequest;
import com.bestfriend.danjjak.tts.service.TtsService;
import javax.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping(produces = "audio/mpeg")
    public ResponseEntity<byte[]> createSpeech(@Valid @RequestBody TtsRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.noStore())
                .body(ttsService.createSpeech(request));
    }
}
