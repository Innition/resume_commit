package cn.lazylhxzzy.resume_commit.service.impl;

import cn.lazylhxzzy.resume_commit.annotation.BusinessLog;
import cn.lazylhxzzy.resume_commit.annotation.SecurityLog;
import cn.lazylhxzzy.resume_commit.dto.LoginRequest;
import cn.lazylhxzzy.resume_commit.dto.RegisterRequest;
import cn.lazylhxzzy.resume_commit.entity.InviteCode;
import cn.lazylhxzzy.resume_commit.entity.User;
import cn.lazylhxzzy.resume_commit.mapper.InviteCodeMapper;
import cn.lazylhxzzy.resume_commit.mapper.UserMapper;
import cn.lazylhxzzy.resume_commit.service.UserService;
import cn.lazylhxzzy.resume_commit.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private InviteCodeMapper inviteCodeMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    @SecurityLog(value = "用户登录", riskLevel = "MEDIUM", operation = "LOGIN")
    public String login(LoginRequest loginRequest) {
        User user = getUserByUsername(loginRequest.getUsername());
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }
    
    @Override
    @SecurityLog(value = "用户注册", riskLevel = "HIGH", operation = "REGISTER")
    public String register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (getUserByUsername(registerRequest.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 验证邀请码
        if (!validateInviteCode(registerRequest.getInviteCode())) {
            throw new RuntimeException("邀请码无效或已过期");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        
        // 使用邀请码
        useInviteCode(registerRequest.getInviteCode(), user.getId());
        
        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }
    
    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
    
    @Override
    @BusinessLog(value = "生成邀请码", module = "USER", operation = "GENERATE_INVITE_CODE")
    public String generateInviteCode(Long userId) {
        // 检查用户是否为ROOT
        User user = userMapper.selectById(userId);
        if (!"ROOT".equals(user.getRole())) {
            throw new RuntimeException("只有ROOT用户才能生成邀请码");
        }
        
        // 生成6位随机邀请码
        String code = generateRandomCode(6);
        
        // 检查邀请码是否已存在
        QueryWrapper<InviteCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        while (inviteCodeMapper.selectOne(queryWrapper) != null) {
            code = generateRandomCode(6);
        }
        
        // 创建邀请码记录
        InviteCode inviteCode = new InviteCode();
        inviteCode.setCode(code);
        inviteCode.setCreatedBy(userId);
        inviteCode.setIsUsed(false);
        inviteCode.setExpiresAt(LocalDateTime.now().plusDays(1)); // 1天过期
        inviteCode.setCreatedAt(LocalDateTime.now());
        
        inviteCodeMapper.insert(inviteCode);
        
        return code;
    }
    
    @Override
    public boolean validateInviteCode(String inviteCode) {
        QueryWrapper<InviteCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", inviteCode)
                   .eq("is_used", false)
                   .gt("expires_at", LocalDateTime.now());
        return inviteCodeMapper.selectOne(queryWrapper) != null;
    }
    
    @Override
    public void useInviteCode(String inviteCode, Long userId) {
        QueryWrapper<InviteCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", inviteCode)
                   .eq("is_used", false)
                   .gt("expires_at", LocalDateTime.now());
        
        InviteCode code = inviteCodeMapper.selectOne(queryWrapper);
        if (code == null) {
            throw new RuntimeException("邀请码无效或已过期");
        }
        
        code.setUsedBy(userId);
        code.setIsUsed(true);
        inviteCodeMapper.updateById(code);
    }
    
    /**
     * 生成随机邀请码
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}