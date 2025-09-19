package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邀请码实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("invite_codes")
public class InviteCode {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String code;
    
    private Long createdBy;
    
    private Long usedBy;
    
    private Boolean isUsed;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime createdAt;
}
