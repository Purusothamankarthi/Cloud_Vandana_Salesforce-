package com.AssociateSoftwareEngineer.AssociateSoftware.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Intelligent DataSource configuration that automatically handles:
 * 1. Standard JDBC URLs (jdbc:postgresql:// or jdbc:mysql://)
 * 2. Cloud URI formats from Neon/Heroku/Render (postgresql://user:pass@host/db)
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        if (rawUrl != null && rawUrl.startsWith("postgresql://")) {
            log.info("Detected Neon/PostgreSQL URI format — converting to JDBC format");
            try {
                URI uri = new URI(rawUrl);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath(); // e.g. /neondb
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "");
                dataSource.setJdbcUrl(jdbcUrl);
                dataSource.setDriverClassName("org.postgresql.Driver");

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    dataSource.setUsername(userInfo[0]);
                    if (userInfo.length > 1) {
                        dataSource.setPassword(userInfo[1]);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse PostgreSQL URI as URI object, falling back to prefix: {}", e.getMessage());
                dataSource.setJdbcUrl("jdbc:" + rawUrl);
                dataSource.setDriverClassName("org.postgresql.Driver");
            }
        } else {
            dataSource.setJdbcUrl(rawUrl);
            if (rawUrl != null && rawUrl.startsWith("jdbc:postgresql:")) {
                dataSource.setDriverClassName("org.postgresql.Driver");
            } else if (rawUrl != null && rawUrl.startsWith("jdbc:mysql:")) {
                dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
        }

        if (dataSource.getUsername() == null || dataSource.getUsername().isEmpty()) {
            if (username != null && !username.isEmpty()) {
                dataSource.setUsername(username);
            }
        }

        if (dataSource.getPassword() == null || dataSource.getPassword().isEmpty()) {
            if (password != null && !password.isEmpty()) {
                dataSource.setPassword(password);
            }
        }

        return dataSource;
    }
}
