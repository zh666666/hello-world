package com.showtime.appactivitidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {
        //使用springboot2.x版本时请打开下面一个注释
        org.activiti.spring.boot.SecurityAutoConfiguration.class
})
public class AppActivitidemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppActivitidemoApplication.class, args);
    }

}
