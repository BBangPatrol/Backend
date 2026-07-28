package com.bbangpatrol.user.service;

import com.bbangpatrol.auth.repository.UserRepository;
import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.point.entity.PointHistory;
import com.bbangpatrol.point.entity.PointType;
import com.bbangpatrol.point.repository.PointHistoryRepository;
import com.bbangpatrol.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void addPoint(Long userId, Integer point, PointType type, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        user.addPoint(point);
        pointHistoryRepository.save(PointHistory.builder()
                .user(user)
                .type(type)
                .amount(point)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build());
    }
}