# 部署配置指南

## 🔒 安全配置

### 环境变量配置

为了确保数据库连接信息的安全性，本项目使用环境变量来配置敏感信息。

#### 必需的环境变量

```bash
# 数据库配置
DB_URL=jdbc:mysql://your-server:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password

# JWT配置
JWT_SECRET=your_super_long_secret_key_here_at_least_256_bits_long
JWT_EXPIRATION=86400000
```

### 部署方式

#### 方式一：Docker 部署（推荐）

1. **创建 docker-compose.yml**
```yaml
version: '3.8'
services:
  app:
    image: your-registry/resume-commit:latest
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:mysql://mysql:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      - DB_USERNAME=resume_user
      - DB_PASSWORD=your_secure_password
      - JWT_SECRET=your_super_long_secret_key_here_at_least_256_bits_long
    depends_on:
      - mysql
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root_password
      - MYSQL_DATABASE=resume_commit
      - MYSQL_USER=resume_user
      - MYSQL_PASSWORD=your_secure_password
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    restart: unless-stopped

volumes:
  mysql_data:
```

2. **启动服务**
```bash
docker-compose up -d
```

#### 方式二：传统部署

1. **设置环境变量**
```bash
# Linux/macOS
export DB_URL="jdbc:mysql://localhost:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_password"
export JWT_SECRET="your_super_long_secret_key_here_at_least_256_bits_long"

# Windows
set DB_URL=jdbc:mysql://localhost:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
set DB_USERNAME=your_username
set DB_PASSWORD=your_password
set JWT_SECRET=your_super_long_secret_key_here_at_least_256_bits_long
```

2. **启动应用**
```bash
java -jar resume-commit.jar
```

#### 方式三：使用配置文件

1. **创建外部配置文件**
```properties
# config/application-prod.properties
spring.datasource.url=jdbc:mysql://your-server:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password
jwt.secret=your_super_long_secret_key_here_at_least_256_bits_long
```

2. **启动时指定配置文件**
```bash
java -jar resume-commit.jar --spring.config.location=classpath:/application.properties,file:./config/application-prod.properties
```

### 安全建议

#### 1. 密码安全
- 使用强密码，包含大小写字母、数字和特殊字符
- 定期更换密码
- 不要在代码中硬编码密码

#### 2. JWT密钥安全
- JWT密钥至少256位
- 使用随机生成的密钥
- 定期更换JWT密钥

#### 3. 数据库安全
- 限制数据库访问IP
- 使用SSL连接
- 定期备份数据

#### 4. 服务器安全
- 使用防火墙限制端口访问
- 定期更新系统和依赖
- 监控日志文件

### 生产环境检查清单

- [ ] 数据库连接信息已通过环境变量配置
- [ ] JWT密钥已设置为强密钥
- [ ] 数据库用户权限已最小化
- [ ] SSL证书已配置（如需要）
- [ ] 防火墙规则已配置
- [ ] 日志文件已配置轮转
- [ ] 备份策略已制定
- [ ] 监控告警已配置

### 故障排除

#### 常见问题

1. **数据库连接失败**
   - 检查环境变量是否正确设置
   - 确认数据库服务是否运行
   - 检查网络连接和防火墙设置

2. **JWT认证失败**
   - 检查JWT_SECRET环境变量
   - 确认JWT密钥一致性

3. **端口冲突**
   - 检查端口是否被占用
   - 修改server.port配置

### 支持

如有部署问题，请查看：
- [README.md](README.md) - 项目介绍
- [日志文件](logs/) - 应用日志
- [Issues](https://github.com/your-repo/issues) - 问题反馈
