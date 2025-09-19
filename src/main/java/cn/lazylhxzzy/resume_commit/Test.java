package cn.lazylhxzzy.resume_commit;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Test {
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public static void main(String[] args) {
        System.out.println(passwordEncoder.encode("Lovemel1keyoudo."));
//        String url = "jdbc:mysql://47.119.112.13:3306/resume_commit?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
//        String username = "root";
//        String password = "root";
//
//        try {
//            System.out.println("Connecting to database...");
//            Connection connection = DriverManager.getConnection(url, username, password);
//            System.out.println("Database connection successful!");
//
//            Statement statement = connection.createStatement();
//            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM users");
//
//            if (resultSet.next()) {
//                int count = resultSet.getInt("count");
//                System.out.println("Users table has " + count + " records");
//            }
//
//            resultSet.close();
//            statement.close();
//            connection.close();
//            System.out.println("Database connection test completed!");
//
//        } catch (Exception e) {
//            System.err.println("Database connection failed: " + e.getMessage());
//            e.printStackTrace();
//        }
    }
}
