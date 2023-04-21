package com.ghostchu.web.hikarispk.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeneralController {
    @GetMapping("/")
    @ResponseBody
    public String hello() {
        return "Hikari-SPK, another Synology Package Center written by Java.";
    }
}
