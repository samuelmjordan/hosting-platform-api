package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.user.ApplicationUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class UserRepository extends BaseRepository {

    public UserRepository(JdbcTemplate jdbc) { super(jdbc); }

    public int insertUser(ApplicationUser user) {
        return execute("INSERT INTO user_ (clerk_id, customer_id) VALUES (?, ?)",
                user.clerkId(), user.customerId());
    }

    public Optional<ApplicationUser> selectUser(String clerkId) {
        return selectOne("SELECT clerk_id, customer_id FROM user_ WHERE clerk_id = ?",
                this::mapUser, clerkId);
    }

    public Optional<String> selectCustomerIdByClerkId(String clerkId) {
        return selectOne("SELECT customer_id FROM user_ WHERE clerk_id = ?",
                (rs, rowNum) -> rs.getString("customer_id"), clerkId);
    }

    public Optional<String> selectClerkIdByCustomerId(String customerId) {
        return selectOne("SELECT clerk_id FROM user_ WHERE customer_id = ?",
                (rs, rowNum) -> rs.getString("clerk_id"), customerId);
    }

    public Optional<AcceptedCurrency> selectUserCurrency(String userId) {
        return selectOne("SELECT currency FROM user_ WHERE clerk_id = ?",
                (rs, rowNum) -> AcceptedCurrency.fromCode(rs.getString("currency")), userId);
    }

    private ApplicationUser mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new ApplicationUser(rs.getString("clerk_id"), rs.getString("customer_id"));
    }
}
