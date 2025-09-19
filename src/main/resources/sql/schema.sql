-- 创建数据库
CREATE DATABASE IF NOT EXISTS resume_commit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE resume_commit;

-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('ROOT', 'USER') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 邀请码表
CREATE TABLE invite_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    created_by BIGINT NOT NULL,
    used_by BIGINT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (used_by) REFERENCES users(id)
);

-- 投递记录表
CREATE TABLE resume_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    position VARCHAR(100) NOT NULL,
    base_location VARCHAR(50),
    apply_time DATETIME NOT NULL,
    test_time DATETIME,
    written_exam_time DATETIME,
    current_status ENUM('已投递', '已测评', '已笔试', '已面试') NULL,
    current_status_date DATETIME NULL,
    final_result ENUM('简历挂', '测评挂', '笔试挂', '面试挂', 'OC', 'PENDING') DEFAULT 'PENDING',
    expected_salary_type ENUM('总包', '月薪', '待商议') NULL,
    expected_salary_value VARCHAR(50) NULL,
    remarks TEXT,
    company_url VARCHAR(500),
    company_group_id VARCHAR(50) NULL, -- 公司分组ID，相同公司的记录使用相同ID
    is_primary BOOLEAN DEFAULT FALSE, -- 是否为主要岗位（用于前端显示）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 面试记录表
CREATE TABLE interview_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_record_id BIGINT NOT NULL,
    interview_type ENUM('AI面', '一面', '二面', '三面', '四面', '五面', '六面', '七面', '八面', '九面', '十面') NOT NULL,
    interview_time DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_record_id) REFERENCES resume_records(id) ON DELETE CASCADE
);

-- 插入默认ROOT用户 (密码: admin123)
INSERT INTO users (username, password, email, role) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'admin@lazylhxzzy.cn', 'ROOT');
