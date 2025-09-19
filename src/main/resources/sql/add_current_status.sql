-- 添加当前状态字段到投递记录表
USE resume_commit;

-- 添加当前状态相关字段
ALTER TABLE resume_records 
ADD COLUMN current_status ENUM('已投递', '已测评', '已笔试', '已面试') NULL AFTER written_exam_time,
ADD COLUMN current_status_date DATETIME NULL AFTER current_status;

-- 添加注释
ALTER TABLE resume_records 
MODIFY COLUMN current_status ENUM('已投递', '已测评', '已笔试', '已面试') NULL COMMENT '当前状态',
MODIFY COLUMN current_status_date DATETIME NULL COMMENT '当前状态日期';
