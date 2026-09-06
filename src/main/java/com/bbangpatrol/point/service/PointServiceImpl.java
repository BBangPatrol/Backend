package com.bbangpatrol.point.service;

import com.bbangpatrol.common.exception.ApiException;
import com.bbangpatrol.common.util.code.ErrorCode;
import com.bbangpatrol.point.entity.Point;
import com.bbangpatrol.point.entity.PointType;
import com.bbangpatrol.point.repository.PointRepository;
import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;
    private final PointRepository pointRepository;

    @Transactional
    @Override
    public void updatePoint(Long userId, int point, boolean isIncrease) {
        updatePoint(userId, point, isIncrease, isIncrease ? "포인트 적립" : "포인트 사용");
    }

    @Transactional
    @Override
    public void updatePoint(Long userId, int point, boolean isIncrease, String content) {
        log.info("[Point Service] {}의 {}포인트 {}!", userId, point, isIncrease ? "적립" : "소비");

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.USER_NOT_FOUND)
                );

        PointType type;
        int amount;

        if(isIncrease) { // true일 경우 적립
            user.addPoint(point);

            type = PointType.earn;
            amount = point;
        } else { // false일 경우 소비
            user.usePoint(point);

            type = PointType.spend;
            amount = -point;
        }

        Point history = Point.builder() // type에 맞게 포인트 적립과 사용
                .type(type)
                .content(content)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        pointRepository.save(history);
        log.info("[Point Service] {}의 {}포인트 {} 성공~", userId, point, isIncrease ? "적립" : "소비");
    }
}
