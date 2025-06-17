package com.mc_host.api.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public abstract class BaseRepository {
	protected final JdbcTemplate jdbc;

	protected BaseRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	protected <T> Optional<T> selectOne(String sql, RowMapper<T> mapper, Object... params) {
		try {
			return jdbc.query(sql, mapper, params).stream().findFirst();
		} catch (DataAccessException e) {
			throw new RuntimeException("query failed: " + e.getMessage(), e);
		}
	}

	protected <T> List<T> selectMany(String sql, RowMapper<T> mapper, Object... params) {
		try {
			return jdbc.query(sql, mapper, params);
		} catch (DataAccessException e) {
			throw new RuntimeException("query failed: " + e.getMessage(), e);
		}
	}

	protected int execute(String sql, Object... params) {
		try {
			return jdbc.update(sql, params);
		} catch (DataAccessException e) {
			throw new RuntimeException("update failed: " + e.getMessage(), e);
		}
	}

	protected void upsert(String sql, PreparedStatementSetter setter) {
		try {
			jdbc.update(con -> {
				var ps = con.prepareStatement(sql);
				setter.setValues(ps);
				return ps;
			});
		} catch (DataAccessException e) {
			throw new RuntimeException("upsert failed: " + e.getMessage(), e);
		}
	}
}
