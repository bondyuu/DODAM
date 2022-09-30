package com.team1.dodam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1.dodam.domain.ChatRoom;
import com.team1.dodam.domain.Notification;
import com.team1.dodam.domain.User;
import com.team1.dodam.dto.response.NotificationResponseDto;
import com.team1.dodam.dto.response.ResponseDto;
import com.team1.dodam.repository.NotificationRepository;
import com.team1.dodam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private static final Map<Long, SseEmitter> sseEmitters = new HashMap<>();
    public SseEmitter subscribe(Long userId) {

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        sseEmitters.put(userId, emitter);

        emitter.onCompletion(() -> sseEmitters.remove(userId));
        emitter.onTimeout(() -> sseEmitters.remove(userId));
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new IllegalArgumentException("회원 정보를 찾을 수 없습니다.")
        );
        List<Notification> notificationList = notificationRepository.findAllByUser(user);
        if (notificationList.size() != 0){
            notificationList.forEach(notification -> sendToClient(emitter, String.valueOf(userId), NotificationResponseDto.from(notification)));
        }

        return emitter;
    }


    private void sendToClient(SseEmitter emitter, String id, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(id)
                    .name("message")
                    .data(data,MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            Long userId = Long.parseLong(id);
            sseEmitters.remove(userId);
            throw new RuntimeException("연결 오류!");
        }
    }

    @Transactional
    public ResponseDto<?> changeIsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                ()-> new IllegalArgumentException("알림을 찾을 수 없습니다.")
        );
        notification.changeIsRead();
        return ResponseDto.success(String.valueOf(notification.getIsRead()));
    }
}