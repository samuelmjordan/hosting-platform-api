package com.mc_host.api.repository;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.ServerSpecification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class GameServerSpecRepository extends BaseRepository {

    public GameServerSpecRepository(JdbcTemplate jdbc) { super(jdbc); }

    public Optional<String> convertPrice(String priceId, AcceptedCurrency currency) {
        return selectOne("""
            SELECT plan2_.price_id FROM plan_ plan1_
            JOIN plan_ plan2_ ON plan1_.specification_id = plan2_.specification_id
            JOIN price_ ON plan2_.price_id = price_.price_id
            WHERE plan1_.price_id = ? AND price_.currency = ?
            """, (rs, rowNum) -> rs.getString("price_id"), priceId, currency.name());
    }

    public Optional<ServerSpecification> selectSpecification(String specId) {
        return selectOne("""
            SELECT
                specification_id,
                title,
                caption,
                ram_gb,
                vcpu,
                ssd_gb
            FROM game_server_specification_
            WHERE specification_id = ?
            """,
            this::mapSpecification,
            specId
        );
    }

    private ServerSpecification mapSpecification(ResultSet rs, int rowNum) throws SQLException {
        return new ServerSpecification(
            rs.getString("specification_id"),
            rs.getString("title"),
            rs.getString("caption"),
            //TODO: db schema should be int
            Integer.valueOf(rs.getString("ram_gb")),
            Integer.valueOf(rs.getString("vcpu")),
            Integer.valueOf(rs.getString("ssd_gb")));
    }
}
