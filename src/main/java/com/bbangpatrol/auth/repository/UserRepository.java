package com.bbangpatrol.auth.repository;

import com.bbangpatrol.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // KakaoId 기반으로 사용자가 존재하는지 탐색
    User findByKakaoId(String kakaoId);
}
