package com.bbangpatrol.user.repository;

import com.bbangpatrol.user.entity.User;
import com.bbangpatrol.user.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    UserImage findByUser(User user);
}
