# 简历投递管理系统

一个基于 Spring Boot 3 + Vue.js 的现代化简历投递记录管理系统，帮助求职者高效管理投递记录，跟踪面试进度。

## ✨ 主要功能

### 📋 投递记录管理
- **多岗位支持** - 同一公司可投递多个岗位，智能分组显示
- **进度跟踪** - 完整记录投递→测评→笔试→面试→OC的完整流程
- **状态管理** - 实时更新当前状态和最终结果
- **时间记录** - 精确记录各个环节的时间节点

### 🔍 智能筛选与搜索
- **多维度筛选** - 按最终结果、当前状态、薪资范围筛选
- **关键词搜索** - 支持公司名称、岗位、地点模糊搜索
- **实时统计** - 显示筛选结果数量和总数

### 📊 数据可视化
- **双视图模式** - 卡片视图（详细展示）和列表视图（快速对比）
- **流程时间线** - 直观展示招聘流程进度
- **泡池时间** - 自动计算并显示距离最后活动的时间

### 💼 Excel 数据管理
- **一键导出** - 支持导出所有记录或筛选结果
- **批量导入** - 三种导入模式：新增、替换、跳过重复
- **格式兼容** - 完全兼容导出格式，支持所有字段导入

### 👥 用户权限管理
- **多用户支持** - 基于邀请码的用户注册系统
- **角色权限** - ROOT用户可查看所有数据，普通用户只能查看自己的记录
- **邀请码管理** - ROOT用户可生成邀请码，支持新用户注册

### 📈 日志监控系统
- **全面日志记录** - 系统日志、访问日志、安全日志、性能日志
- **实时监控** - 错误日志实时监控和告警
- **日志分析** - 支持按时间、级别、用户等维度查询分析
- **数据统计** - 提供日志统计图表和趋势分析

## 🛠️ 技术栈

### 后端技术
- **Spring Boot 3** - 现代化Java框架
- **MyBatis Plus** - 强大的ORM框架
- **Spring Security** - 安全认证与授权
- **JWT** - 无状态身份验证
- **Apache POI** - Excel文件处理
- **SLF4J + Logback** - 日志框架
- **AOP** - 面向切面编程，自动日志记录

### 前端技术
- **原生JavaScript** - 轻量级前端实现
- **Bootstrap 5** - 现代化UI框架
- **Bootstrap Icons** - 丰富的图标库
- **响应式设计** - 支持多设备访问

### 数据库
- **MySQL** - 关系型数据库
- **连接池** - 高效的数据库连接管理

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd resume_commit
   ```

2. **配置数据库**
   ```sql
   -- 创建数据库
   CREATE DATABASE resume_commit;
   
   -- 导入数据库结构
   source sql/init.sql
   ```

3. **配置数据库**
   
   **方式一：使用环境变量（推荐）**
   ```bash
   # 设置环境变量
   export DB_URL="jdbc:mysql://localhost:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
   export DB_USERNAME="your_username"
   export DB_PASSWORD="your_password"
   export JWT_SECRET="your_super_long_secret_key_here_at_least_256_bits"
   ```
   
   **方式二：复制配置文件**
   ```bash
   # 复制示例配置文件
   cp src/main/resources/application-example.properties src/main/resources/application.properties
   # 然后编辑 application.properties 文件
   ```

4. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

5. **访问系统**
   ```
   http://localhost:8080
   ```

## 📖 使用指南

### 首次使用
1. 系统会自动创建ROOT用户（用户名：root，密码：admin123）
2. ROOT用户可以生成邀请码，邀请其他用户注册
3. 普通用户使用邀请码注册后即可开始使用

### 基本操作
1. **添加记录** - 点击"添加记录"按钮，填写公司和岗位信息
2. **更新进度** - 编辑记录，更新当前状态和面试时间
3. **筛选查看** - 使用搜索和筛选功能快速找到目标记录
4. **数据导出** - 导出记录到Excel文件进行备份或分析

## 🔧 配置说明

### 安全配置
- **数据库配置**：使用环境变量，避免敏感信息泄露
- **JWT密钥**：使用强密钥，至少256位
- **生产环境**：请参考 [DEPLOYMENT.md](DEPLOYMENT.md) 进行安全部署

### 日志配置
- 日志文件位置：`logs/` 目录
- 日志级别：DEBUG、INFO、WARN、ERROR
- 日志轮转：按天轮转，保留30天
- 异步写入：提高性能，减少对业务的影响

### 权限配置
- JWT Token有效期：24小时
- 邀请码有效期：24小时
- 密码策略：无特殊要求（可根据需要添加）

## 📝 更新日志

### v0.0.2beta (当前版本)
- ✅ 完善多岗位管理功能
- ✅ 优化Excel导入导出
- ✅ 添加日志监控系统
- ✅ 本地化外部资源依赖
- ✅ 修复分组显示问题
- ✅ 增强UI交互体验

### v0.0.1
- ✅ 基础投递记录管理
- ✅ 用户认证与权限
- ✅ Excel数据导入导出
- ✅ 响应式界面设计

## 🤝 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目！

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件

---

**简历投递管理系统** - 让求职更高效，让管理更简单！ 🎯