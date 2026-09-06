package com.bbangpatrol.point.service;

public interface PointService {

    void updatePoint(Long userId, int point, boolean isIncrease);

    // 내역에 남길 사유를 직접 지정한다
    void updatePoint(Long userId, int point, boolean isIncrease, String content);
}
