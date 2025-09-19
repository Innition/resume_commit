package cn.lazylhxzzy.resume_commit.service;

import cn.lazylhxzzy.resume_commit.dto.LoginRequest;
import cn.lazylhxzzy.resume_commit.dto.RegisterRequest;
import cn.lazylhxzzy.resume_commit.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户登录
     */
    String login(LoginRequest loginRequest);
    
    /**
     * 用户注册
     */
    String register(RegisterRequest registerRequest);
    
    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
    
    /**
     * 生成邀请码
     */
    String generateInviteCode(Long userId);
    
    /**
     * 验证邀请码
     */
    boolean validateInviteCode(String inviteCode);
    
    /**
     * 使用邀请码
     */
    void useInviteCode(String inviteCode, Long userId);
}