-- 添加公司分组字段到投递记录表
USE resume_commit;

-- 添加公司分组相关字段
ALTER TABLE resume_records 
ADD COLUMN company_group_id VARCHAR(50) NULL COMMENT '公司分组ID，相同公司的记录使用相同ID' AFTER company_url,
ADD COLUMN is_primary BOOLEAN DEFAULT FALSE COMMENT '是否为主要岗位（用于前端显示）' AFTER company_group_id;

-- 为现有数据生成公司分组ID
UPDATE resume_records 
SET company_group_id = CONCAT('group_', user_id, '_', company_name, '_', UNIX_TIMESTAMP(created_at))
WHERE company_group_id IS NULL;

-- 为每个公司分组设置一个主要岗位（选择最新的记录）
UPDATE resume_records r1
SET is_primary = TRUE
WHERE r1.id = (
    SELECT r2.id 
    FROM resume_records r2 
    WHERE r2.company_group_id = r1.company_group_id 
    ORDER BY r2.updated_at DESC 
    LIMIT 1
);
