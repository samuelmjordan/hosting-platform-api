package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.service.EncryptionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class UserRepository extends BaseRepository {

    private final EncryptionService encryptionService;

    public UserRepository(JdbcTemplate jdbc, EncryptionService encryptionService) {
        super(jdbc);
        this.encryptionService = encryptionService;
    }

    public int insertUser(ApplicationUser user) {
        String encryptedPassword = encryptionService.encrypt(user.pterodactylPassword());

        return execute("""
            INSERT INTO user_ (
                clerk_id,
                customer_id,
                pterodactyl_user_id,
                pterodactyl_username,
                pterodactyl_password,
                primary_email,
                dummy_email)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """,
            user.clerkId(),
            user.customerId(),
            user.pterodactylUserId(),
            user.pterodactylUsername(),
            encryptedPassword,
            user.primaryEmail(),
            user.dummyEmail());
    }

    public int updatePrimaryEmail(String primaryEmail, String clerkId) {
        return execute("""
        UPDATE user_ SET
            primary_email = ?
        WHERE clerk_id = ?
        """,
        primaryEmail,
        clerkId);
    }

    public int delete(String clerkId) {
        return execute("""
        UPDATE user_ SET
            deleted_at = CURRENT_TIMESTAMP
        WHERE clerk_id = ?
        """,
            clerkId);
    }

    public Optional<ApplicationUser> selectUser(String clerkId) {
        return selectOne("""
                SELECT
                    clerk_id,
                    customer_id,
                    pterodactyl_user_id,
                    pterodactyl_username,
                    pterodactyl_password,
                    primary_email,
                    dummy_email
                FROM user_
                WHERE clerk_id = ?
                """,
            this::mapUser, clerkId);
    }

    public Optional<ApplicationUser> selectUserByCustomerId(String customerId) {
        return selectOne("""
                SELECT
                    clerk_id,
                    customer_id,
                    pterodactyl_user_id,
                    pterodactyl_username,
                    pterodactyl_password,
                    primary_email,
                    dummy_email
                FROM user_
                WHERE customer_id = ?
                """,
            this::mapUser, customerId);
    }

    public boolean usernameExists(String username) {
        return selectOne("SELECT 1 FROM user_ WHERE pterodactyl_username = ?",
            (rs, rowNum) -> true, username)
            .isPresent();
    }

    public Optional<String> selectCustomerIdByClerkId(String clerkId) {
        return selectOne("SELECT customer_id FROM user_ WHERE clerk_id = ?",
            (rs, rowNum) -> rs.getString("customer_id"), clerkId);
    }

    public Optional<AcceptedCurrency> selectUserCurrency(String userId) {
        return selectOne("SELECT currency FROM user_ WHERE clerk_id = ?",
            (rs, rowNum) -> AcceptedCurrency.fromCode(rs.getString("currency")), userId);
    }

    private ApplicationUser mapUser(ResultSet rs, int rowNum) throws SQLException {
        String encryptedPassword = rs.getString("pterodactyl_password");
        String decryptedPassword = encryptionService.decrypt(encryptedPassword);

        return new ApplicationUser(
            rs.getString("clerk_id"),
            rs.getString("customer_id"),
            rs.getLong("pterodactyl_user_id"),
            rs.getString("pterodactyl_username"),
            decryptedPassword,
            rs.getString("primary_email"),
            rs.getString("dummy_email"));
    }
}