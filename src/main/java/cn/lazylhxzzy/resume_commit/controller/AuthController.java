package cn.lazylhxzzy.resume_commit.controller;

import cn.lazylhxzzy.resume_commit.dto.LoginRequest;
import cn.lazylhxzzy.resume_commit.dto.RegisterRequest;
import cn.lazylhxzzy.resume_commit.service.UserService;
import cn.lazylhxzzy.resume_commit.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Tag(name = "用户认证", description = "用户登录、注册和邀请码管理")
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Operation(summary = "用户登录", description = "用户使用用户名和密码登录系统")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.login(loginRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("message", "登录成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "用户注册", description = "用户使用邀请码注册新账户")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest registerRequest) {
        try {
            String token = userService.register(registerRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "生成邀请码", description = "ROOT用户生成邀请码供其他用户注册")
    @PostMapping("/generate-invite-code")
    public ResponseEntity<Map<String, Object>> generateInviteCode(@RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            String role = jwtUtil.getRoleFromToken(token.substring(7));
            
            if (!"ROOT".equals(role)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "只有ROOT用户才能生成邀请码");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取用户ID
            var user = userService.getUserByUsername(username);
            String inviteCode = userService.generateInviteCode(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inviteCode", inviteCode);
            response.put("message", "邀请码生成成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}