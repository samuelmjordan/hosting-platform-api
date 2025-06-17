package com.mc_host.api.repository;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.Mode;
import com.mc_host.api.model.provisioning.Status;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.subscription.MarketingRegion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class ServerExecutionContextRepository extends BaseRepository {

    public ServerExecutionContextRepository(JdbcTemplate jdbc) { super(jdbc); }

    public void upsertSubscription(Context context) {
        upsert("""
            INSERT INTO server_execution_context_ (
                subscription_id,
                step_type,
                mode,
                execution_status,
                region,
                specification_id,
                title,
                caption,
                node_id,
                a_record_id,
                pterodactyl_node_id,
                allocation_id,
                pterodactyl_server_id,
                c_name_record_id,
                new_node_id,
                new_a_record_id,
                new_pterodactyl_node_id,
                new_allocation_id,
                new_pterodactyl_server_id,
                new_c_name_record_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (subscription_id) DO UPDATE SET
                step_type = EXCLUDED.step_type,
                mode = EXCLUDED.mode,
                execution_status = EXCLUDED.execution_status,
                region = EXCLUDED.region,
                specification_id = EXCLUDED.specification_id,
                title = EXCLUDED.title,
                caption = EXCLUDED.caption,
                node_id = EXCLUDED.node_id,
                a_record_id = EXCLUDED.a_record_id,
                pterodactyl_node_id = EXCLUDED.pterodactyl_node_id,
                allocation_id = EXCLUDED.allocation_id,
                pterodactyl_server_id = EXCLUDED.pterodactyl_server_id,
                c_name_record_id = EXCLUDED.c_name_record_id,
                new_node_id = EXCLUDED.new_node_id,
                new_a_record_id = EXCLUDED.new_a_record_id,
                new_pterodactyl_node_id = EXCLUDED.new_pterodactyl_node_id,
                new_allocation_id = EXCLUDED.new_allocation_id,
                new_pterodactyl_server_id = EXCLUDED.new_pterodactyl_server_id,
                new_c_name_record_id = EXCLUDED.new_c_name_record_id
            """, ps -> setContextParams(ps, context));
    }

    public void insertOrIgnoreSubscription(Context context) {
        upsert("""
            INSERT INTO server_execution_context_ (
                subscription_id,
                step_type, mode,
                execution_status,
                region,
                specification_id,
                title,
                caption,
                node_id,
                a_record_id,
                pterodactyl_node_id,
                allocation_id,
                pterodactyl_server_id,
                c_name_record_id,
                new_node_id,
                new_a_record_id,
                new_pterodactyl_node_id,
                new_allocation_id,
                new_pterodactyl_server_id,
                new_c_name_record_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (subscription_id) DO NOTHING
            """, ps -> setContextParams(ps, context));
    }

    public void updateSubscription(Context context) {
        upsert("""
            UPDATE server_execution_context_ SET
                step_type = ?,
                mode = ?,
                execution_status = ?,
                region = ?,
                specification_id = ?,
                title = ?,
                caption = ?,
                node_id = ?,
                a_record_id = ?,
                pterodactyl_node_id = ?,
                allocation_id = ?,
                pterodactyl_server_id = ?,
                c_name_record_id = ?,
                new_node_id = ?,
                new_a_record_id = ?,
                new_pterodactyl_node_id = ?,
                new_allocation_id = ?,
                new_pterodactyl_server_id = ?,
                new_c_name_record_id = ?
            WHERE subscription_id = ?
            """, ps -> {
            setContextParams(ps, context);
            ps.setString(20, context.getSubscriptionId()); // for WHERE clause
        });
    }

    public Optional<Context> selectSubscription(String subId) {
        return selectOne("""
            SELECT
            subscription_id,
            step_type, mode,
            execution_status,
            region,
            specification_id,
            title,
            caption,
            node_id,
            a_record_id,
            pterodactyl_node_id,
            allocation_id,
            pterodactyl_server_id,
            c_name_record_id,
            new_node_id,
            new_a_record_id,
            new_pterodactyl_node_id,
            new_allocation_id,
            new_pterodactyl_server_id,
            new_c_name_record_id
            FROM server_execution_context_ WHERE subscription_id = ?
            """,
            this::mapContext,
            subId
        );
    }

    public void updateTitle(String subId, String title) {
        execute("UPDATE server_execution_context_ SET title = ? WHERE subscription_id = ?", title, subId);
    }

    public void updateCaption(String subId, String caption) {
        execute("UPDATE server_execution_context_ SET caption = ? WHERE subscription_id = ?", caption, subId);
    }

    public void updateRegion(String subId, MarketingRegion region) {
        execute("UPDATE server_execution_context_ SET region = ? WHERE subscription_id = ?", region.name(), subId);
    }

    public void updateSpecification(String subId, String specId) {
        execute("UPDATE server_execution_context_ SET specification_id = ? WHERE subscription_id = ?", specId, subId);
    }

    private void setContextParams(PreparedStatement ps, Context context) throws SQLException {
        ps.setString(1, context.getSubscriptionId());
        ps.setString(2, context.getStepType().name());
        ps.setString(3, context.getMode().name());
        ps.setString(4, context.getStatus().name());
        ps.setString(5, context.getRegion().name());
        ps.setString(6, context.getSpecificationId());
        ps.setString(7, context.getTitle());
        ps.setString(8, context.getCaption());
        ps.setObject(9, context.getNodeId());
        ps.setString(10, context.getARecordId());
        ps.setObject(11, context.getPterodactylNodeId());
        ps.setObject(12, context.getAllocationId());
        ps.setObject(13, context.getPterodactylServerId());
        ps.setObject(14, context.getCNameRecordId());
        ps.setObject(15, context.getNewNodeId());
        ps.setString(16, context.getNewARecordId());
        ps.setObject(17, context.getNewPterodactylNodeId());
        ps.setObject(18, context.getNewAllocationId());
        ps.setObject(19, context.getNewPterodactylServerId());
        ps.setObject(20, context.getNewCNameRecordId());
    }

    private Context mapContext(ResultSet rs, int rowNum) throws SQLException {
        return new Context(
            rs.getString("subscription_id"),
            StepType.valueOf(rs.getString("step_type")),
            Mode.valueOf(rs.getString("mode")),
            Status.valueOf(rs.getString("execution_status")),
            MarketingRegion.valueOf(rs.getString("region")),
            rs.getString("specification_id"),
            rs.getString("title"),
            rs.getString("caption"),
            (Long) rs.getObject("node_id"),
            rs.getString("a_record_id"),
            (Long) rs.getObject("pterodactyl_node_id"),
            (Long) rs.getObject("allocation_id"),
            (Long) rs.getObject("pterodactyl_server_id"),
            rs.getString("c_name_record_id"),
            (Long) rs.getObject("new_node_id"),
            rs.getString("new_a_record_id"),
            (Long) rs.getObject("new_pterodactyl_node_id"),
            (Long) rs.getObject("new_allocation_id"),
            (Long) rs.getObject("new_pterodactyl_server_id"),
            rs.getString("new_c_name_record_id"));
    }
}
