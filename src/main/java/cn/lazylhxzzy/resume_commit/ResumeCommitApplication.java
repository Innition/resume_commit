package cn.lazylhxzzy.resume_commit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {SqlInitializationAutoConfiguration.class})
@MapperScan("cn.lazylhxzzy.resume_commit.mapper")
@EnableScheduling
public class ResumeCommitApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeCommitApplication.class, args);
	}

}
