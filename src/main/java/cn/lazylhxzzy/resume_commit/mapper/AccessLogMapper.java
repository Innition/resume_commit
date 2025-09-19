package cn.lazylhxzzy.resume_commit.mapper;

import cn.lazylhxzzy.resume_commit.entity.AccessLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问日志Mapper
 */
@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {
}
