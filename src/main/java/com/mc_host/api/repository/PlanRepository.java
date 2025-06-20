package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.ContentPrice;
import com.mc_host.api.model.plan.Plan;
import com.mc_host.api.model.plan.ServerSpecification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class PlanRepository extends BaseRepository {

    public PlanRepository(JdbcTemplate jdbc) { super(jdbc); }

    public List<Plan> selectJavaServerPlans() {
        return selectMany("""
            SELECT
                price_.price_id,
                price_.product_id,
                price_.active,
                price_.currency,
                price_.minor_amount,
                jss_.specification_id,
                jss_.title, jss_.caption,
                jss_.ram_gb, jss_.vcpu, jss_.ssd_gb
            FROM plan_
            JOIN price_ ON price_.price_id = plan_.price_id
            JOIN game_server_specification_ jss_ ON jss_.specification_id = plan_.specification_id
            """, this::mapPlan);
    }

    public Optional<String> selectSpecificationId(String priceId) {
        return selectOne("SELECT specification_id FROM plan_ WHERE price_id = ?",
                (rs, rowNum) -> rs.getString("specification_id"), priceId);
    }

    public Optional<String> selectPriceId(String specId, AcceptedCurrency currency) {
        return selectOne("""
            SELECT price_.price_id FROM plan_
            JOIN price_ ON price_.price_id = plan_.price_id
            WHERE plan_.specification_id = ? AND price_.currency = ?
            """, (rs, rowNum) -> rs.getString("price_id"), specId, currency.name());
    }

    public Optional<String> selectPlanIdFromPriceId(String priceId) {
        return selectOne("SELECT plan_id FROM plan_ WHERE price_id = ?",
                (rs, rowNum) -> rs.getString("plan_id"), priceId);
    }

    private Plan mapPlan(ResultSet rs, int rowNum) throws SQLException {
        return new Plan(
            new ServerSpecification(
                rs.getString("specification_id"),
                rs.getString("title"),
                rs.getString("caption"),
                Integer.valueOf(rs.getString("ram_gb")),
                Integer.valueOf(rs.getString("vcpu")),
                Integer.valueOf(rs.getString("ssd_gb"))),
            new ContentPrice(
                rs.getString("price_id"),
                rs.getString("product_id"),
                rs.getBoolean("active"),
                AcceptedCurrency.fromCode(rs.getString("currency")),
                rs.getLong("minor_amount")));
    }
}
