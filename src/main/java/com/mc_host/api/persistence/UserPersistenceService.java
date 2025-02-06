package com.mc_host.api.persistence;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.entity.UserEntity;

@Service
public class UserPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    public UserPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertUser(UserEntity userEntity) {
        try {
            jdbcTemplate.update(
                """
                INSERT INTO users (
                    clerk_id, 
                    customer_id
                )
                VALUES (?, ?)
                """,
                userEntity.clerkId(),
                userEntity.customerId()
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to save subscription to database: " + e.getMessage(), e);
        }
    }   

    public Optional<String> selectCustomerIdByClerkId(String clerkId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT customer_id
                FROM users 
                WHERE clerk_id = ?
                """,
                (rs, rowNum) -> rs.getString("customer_id"),
                clerkId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch customer ID for clerk ID: " + e.getMessage(), e);
        }
    }
}
