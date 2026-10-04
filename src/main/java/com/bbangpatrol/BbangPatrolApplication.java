package com.bbangpatrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 보관 기간이 지난 탈퇴 회원·삭제 리뷰 파기 (AccountPurgeScheduler)
public class BbangPatrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(BbangPatrolApplication.class, args);
    }

}
