package com.example.demo.controller;


import com.example.demo.config.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class IndexController {

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @GetMapping("/Snowflake")
    public void Snowflake(){
        snowflakeIdWorker.nextId();
    }

}
