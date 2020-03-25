package com.example.demo.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemConfig {

    @Bean
    public SnowflakeIdWorker getSnowflakeIdWorker(){
        return new SnowflakeIdWorker();
    }

    @Bean(initMethod = "init")
    public ZookeeperCustor getZookeeperCustor(){
        return new ZookeeperCustor();
    }

}
