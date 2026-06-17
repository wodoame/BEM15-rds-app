package com.wodoame.todoapp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;

import javax.sql.DataSource;

@Configuration
public class RdsIamDataSourceConfig {

    @Value("${DB_HOST}")
    private String dbHost;

    @Value("${DB_PORT}")
    private int dbPort;

    @Value("${DB_NAME}")
    private String dbName;

    @Value("${DB_USERNAME}")
    private String dbUsername;

    @Value("${AWS_REGION}")
    private String awsRegion;

    private HikariDataSource dataSource;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName + "?sslmode=require");
        config.setUsername(dbUsername);
        config.setPassword(generateToken());
        config.setMaxLifetime(600_000);  // 10 min — shorter than the 15-min IAM token TTL
        dataSource = new HikariDataSource(config);
        return dataSource;
    }

    // Refresh the token before it expires so new connections get a valid password.
    @Scheduled(fixedRate = 600_000)
    public void refreshToken() {
        if (dataSource != null) {
            dataSource.getHikariConfigMXBean().setPassword(generateToken());
            dataSource.softEvictConnections();
        }
    }

    private String generateToken() {
        return RdsUtilities.builder()
                .region(Region.of(awsRegion))
                .build()
                .generateAuthenticationToken(r -> r
                        .hostname(dbHost)
                        .port(dbPort)
                        .username(dbUsername));
    }
}
