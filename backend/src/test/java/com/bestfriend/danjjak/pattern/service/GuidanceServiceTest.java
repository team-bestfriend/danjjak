package com.bestfriend.danjjak.pattern.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.pattern.mapper.GuidanceMapper;
import com.bestfriend.danjjak.pattern.model.GuidanceRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class GuidanceServiceTest {
    @TempDir Path directory;
    private GuidanceMapper mapper;
    private VoiceFileStore files;
    private GuidanceService service;
    private GuidanceRecord previous;

    @BeforeEach
    void setUp() throws Exception {
        mapper = mock(GuidanceMapper.class);
        files = new VoiceFileStore(directory.toString());
        service = new GuidanceService(mapper, files);
        previous = new GuidanceRecord();
        previous.setTarget("start");
        previous.setText("안내입니다.");
        previous.setVoiceFilePath("previous");
        previous.setVoiceContentType("audio/webm");
        Files.write(directory.resolve("previous"), new byte[] {1, 2});
        when(mapper.findAll(1L, 2L)).thenReturn(List.of(previous));
        when(mapper.updateAudio(anyLong(), anyLong(), anyString(), anyString(), anyString())).thenReturn(1);
    }

    private MockMultipartFile recording() {
        return new MockMultipartFile("file", "../../family.webm", "audio/webm;codecs=opus", new byte[] {3, 4});
    }

    @Test
    void uploadExposesOnlyAuthenticatedUrlAndRemovesOldFileAfterSuccess() throws Exception {
        var response = service.upload(1, 2, "start", recording());
        assertEquals("/api/patterns/2/guidance/start/audio", response.audioUrl());
        assertFalse(Files.exists(directory.resolve("previous")));
        try (var paths = Files.list(directory)) { assertEquals(1, paths.count()); }
        verify(mapper).updateAudio(eq(1L), eq(2L), eq("start"), matches("[a-f0-9-]{36}"), eq("audio/webm"));
    }

    @Test
    void databaseFailureKeepsPreviousFileAndRemovesNewFile() throws Exception {
        when(mapper.updateAudio(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("database failure"));
        assertThrows(IllegalStateException.class, () -> service.upload(1, 2, "start", recording()));
        assertTrue(Files.exists(directory.resolve("previous")));
        try (var paths = Files.list(directory)) { assertEquals(1, paths.count()); }
    }

    @Test
    void rollbackAfterServiceReturnsKeepsPreviousFile() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(1, 2, "start", recording());
            assertTrue(Files.exists(directory.resolve("previous")));
            for (var callback : TransactionSynchronizationManager.getSynchronizations()) {
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertTrue(Files.exists(directory.resolve("previous")));
            try (var paths = Files.list(directory)) { assertEquals(1, paths.count()); }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void otherUsersAndInvalidTargetsCannotUploadOrRead() {
        assertThrows(ApiException.class, () -> service.upload(99, 2, "start", recording()));
        assertThrows(ApiException.class, () -> service.audio(1, 2, "wrong-step"));
        verify(mapper, never()).updateAudio(anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void rejectsEmptyUnsupportedAndOversizedFiles() {
        assertEquals("VOICE_EMPTY", assertThrows(ApiException.class,
                () -> files.save(new MockMultipartFile("file", new byte[0]))).getCode());
        assertEquals("VOICE_TYPE_UNSUPPORTED", assertThrows(ApiException.class,
                () -> files.save(new MockMultipartFile("file", "x.html", "text/html", new byte[] {1}))).getCode());
        var oversized = mock(org.springframework.web.multipart.MultipartFile.class);
        when(oversized.getSize()).thenReturn(VoiceFileStore.MAX_BYTES + 1);
        assertEquals("VOICE_TOO_LARGE", assertThrows(ApiException.class, () -> files.save(oversized)).getCode());
    }

    @Test
    void storageFailureDoesNotChangeDatabase() throws Exception {
        Path occupied = directory.resolve("not-a-directory");
        Files.writeString(occupied, "file");
        var unavailable = new GuidanceService(mapper, new VoiceFileStore(occupied.toString()));
        assertEquals("VOICE_STORAGE_UNAVAILABLE", assertThrows(ApiException.class,
                () -> unavailable.upload(1, 2, "start", recording())).getCode());
        verify(mapper, never()).updateAudio(anyLong(), anyLong(), anyString(), anyString(), anyString());
        assertTrue(Files.exists(directory.resolve("previous")));
    }
}
