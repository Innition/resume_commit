package cn.lazylhxzzy.resume_commit.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * 首页控制器
 */
@Controller
public class IndexController {
    
    /**
     * 首页重定向到index.html
     */
    @GetMapping("/resume")
    public String index() {
        return "forward:/index.html";
    }
}
