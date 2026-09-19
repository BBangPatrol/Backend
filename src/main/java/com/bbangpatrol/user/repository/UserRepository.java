package com.bbangpatrol.user.repository;

import com.bbangpatrol.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByKakaoId(String kakaoId);

    /**
     * 로그인은 이 메서드를 쓴다. 탈퇴한 행까지 찾아버리면 탈퇴한 계정이 그대로 되살아난다.
     * 같은 카카오 계정으로 다시 로그인하면 새 회원으로 가입되고, 옛 행은 보관 기간이 지나면 파기된다.
     * (kakao_id 에 UNIQUE 가 없어 탈퇴 행과 공존할 수 있다.)
     */
    User findByKakaoIdAndDeletedAtIsNull(String kakaoId);

    boolean existsByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(Long id);
}
