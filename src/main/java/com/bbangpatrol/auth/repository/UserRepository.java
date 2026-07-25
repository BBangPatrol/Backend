package com.bbangpatrol.auth.repository;

import com.bbangpatrol.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // KakaoId 기반으로 사용자가 존재하는지 탐색
    User findByKakaoId(String kakaoId);
}
