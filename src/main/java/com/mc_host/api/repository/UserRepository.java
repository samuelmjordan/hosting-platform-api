package com.mc_host.api.repository;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.user.ApplicationUser;

@Service
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertUser(ApplicationUser userEntity) {
        try {
            return jdbcTemplate.update(
                """
                INSERT INTO user_ (
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

    public Optional<ApplicationUser> selectUser(String clerkId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    clerk_id,
                    customer_id
                FROM user_
                WHERE clerk_id = ?
                """,
                (rs, rowNum) -> new ApplicationUser(
                    rs.getString("clerk_id"),
                    rs.getString("customer_id")
                ),
                clerkId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch customer ID for clerk ID: " + e.getMessage(), e);
        }
    }

    public Optional<String> selectCustomerIdByClerkId(String clerkId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT customer_id
                FROM user_
                WHERE clerk_id = ?
                """,
                (rs, rowNum) -> rs.getString("customer_id"),
                clerkId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch customer ID for clerk ID: " + e.getMessage(), e);
        }
    }

    public Optional<String> selectClerkIdByCustomerId(String customerId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT clerk_id
                FROM user_
                WHERE customer_id = ?
                """,
                (rs, rowNum) -> rs.getString("clerk_id"),
                customerId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch clerk ID for customer ID: " + e.getMessage(), e);
        }
    }

    public Optional<AcceptedCurrency> selectUserCurrency(String userId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT currency
                FROM user_
                WHERE clerk_id = ?
                """,
                (rs, rowNum) -> AcceptedCurrency.fromCode(rs.getString("currency")),
                userId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch currency for user ID: " + userId, e);
        }
    }
}
