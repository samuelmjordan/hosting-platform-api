package com.mc_host.api.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.mc_host.api.model.Currency;
import com.mc_host.api.model.Plan;
import com.mc_host.api.model.entity.PriceEntity;
import com.mc_host.api.model.specification.JavaServerSpecification;

@Service
public class PlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Plan> selectJavaServerPlans() {
        try {
            return jdbcTemplate.query(
                """
                SELECT 
                    price_.price_id, 
                    price_.product_id,
                    price_.active, 
                    price_.currency, 
                    price_.minor_amount,
                    jss_.specification_id,
                    jss_.title,
                    jss_.caption,
                    jss_.ram_gb,
                    jss_.vcpu,
                    jss_.ssd_gb
                FROM plan_
                JOIN price_ ON price_.price_id = plan_.price_id
                JOIN game_server_specification_ jss_ ON jss_.specification_id = plan_.specification_id
                """,
                (rs, rowNum) -> new Plan(
                    new JavaServerSpecification(
                        rs.getString("specification_id"),
                        rs.getString("title"),
                        rs.getString("caption"),
                        rs.getString("ram_gb"),
                        rs.getString("vcpu"),
                        rs.getString("ssd_gb")
                    ), 
                    new PriceEntity(
                        rs.getString("price_id"),
                        rs.getString("product_id"),
                        rs.getBoolean("active"),
                        Currency.fromCode(rs.getString("currency")),
                        rs.getLong("minor_amount")
                    )
                )
            );
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch java server plans", e);
        }
    }

    public Optional<String> selectSpecificationId(String priceId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT specification_id
                FROM plan_
                WHERE price_id = ?
                """,
                (rs, rowNum) -> rs.getString("specification_id"),
                priceId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch spec id for price id: " + priceId, e);
        }
    }

    public Optional<String> selectPlanIdFromPriceId(String priceId) {
        try {
            return jdbcTemplate.query(
                """
                SELECT plan_id
                FROM plan_
                WHERE price_id = ?
                """,
                (rs, rowNum) -> rs.getString("plan_id"),
                priceId
            ).stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to fetch plan id for price id: " + priceId, e);
        }
    }
    
}
