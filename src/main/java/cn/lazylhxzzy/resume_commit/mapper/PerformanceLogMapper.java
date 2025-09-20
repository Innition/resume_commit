package cn.lazylhxzzy.resume_commit.mapper;

import cn.lazylhxzzy.resume_commit.entity.PerformanceLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 性能监控日志Mapper
 */
@Mapper
public interface PerformanceLogMapper extends BaseMapper<PerformanceLog> {
}
