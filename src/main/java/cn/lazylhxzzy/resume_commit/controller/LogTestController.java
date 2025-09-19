package cn.lazylhxzzy.resume_commit.controller;

import cn.lazylhxzzy.resume_commit.util.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志测试控制器
 */
@Tag(name = "日志测试", description = "日志系统测试接口")
@RestController
@RequestMapping("/log-test")
public class LogTestController {
    
    @Autowired
    private LogUtil logUtil;
    
    @Operation(summary = "测试系统日志", description = "测试系统日志记录功能")
    @GetMapping("/system")
    public String testSystemLog() {
        logUtil.info("LOG_TEST", "测试系统日志", "这是一条测试系统日志");
        logUtil.warn("LOG_TEST", "测试警告日志", "这是一条测试警告日志");
        return "系统日志测试完成";
    }
    
    @Operation(summary = "测试业务日志", description = "测试业务日志记录功能")
    @GetMapping("/business")
    public String testBusinessLog() {
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("operation", "测试业务操作");
        businessData.put("data", "测试数据");
        businessData.put("timestamp", System.currentTimeMillis());
        
        logUtil.business("测试业务操作", "业务日志测试", businessData);
        return "业务日志测试完成";
    }
    
    @Operation(summary = "测试安全日志", description = "测试安全日志记录功能")
    @GetMapping("/security")
    public String testSecurityLog() {
        logUtil.security("LOGIN", "用户登录测试", "LOW");
        logUtil.security("PERMISSION_DENIED", "权限拒绝测试", "MEDIUM");
        return "安全日志测试完成";
    }
    
    @Operation(summary = "测试性能日志", description = "测试性能日志记录功能")
    @GetMapping("/performance")
    public String testPerformanceLog() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("memory_usage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        metrics.put("thread_count", Thread.activeCount());
        
        logUtil.performance("LOG_TEST", "testPerformanceLog", 1500L, metrics);
        return "性能日志测试完成";
    }
    
    @Operation(summary = "测试错误日志", description = "测试错误日志记录功能")
    @GetMapping("/error")
    public String testErrorLog() {
        try {
            // 故意抛出一个异常来测试错误日志
            throw new RuntimeException("这是一个测试异常");
        } catch (Exception e) {
            logUtil.error("LOG_TEST", "测试错误日志", "TEST_ERROR_001", 
                         "测试错误信息", e.getStackTrace().toString());
        }
        return "错误日志测试完成";
    }
}
