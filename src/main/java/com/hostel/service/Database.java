package com.hostel.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.*;
@Component
public class Database {
    public final JdbcTemplate jdbc;
    public Database(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Map<String,Object> one(String sql, Object... args) {
        var rows=rows(sql,args);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");
        return rows.getFirst();
    }
    public List<Map<String,Object>> rows(String sql,Object... args) {
        var result=jdbc.queryForList(sql,args);
        result.forEach(row -> row.replaceAll((key,value) -> value instanceof java.sql.Date date ? date.toLocalDate() : value));
        return result;
    }
    public long insert(String table, String columns, Object... values) {
        String placeholders=String.join(",",Collections.nCopies(values.length,"?"));
        return jdbc.queryForObject("insert into "+table+" ("+columns+") values ("+placeholders+") returning id",Long.class,values);
    }
    public static long id(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    public static BigDecimal money(Map<String,Object> row,String key) { return (BigDecimal)row.get(key); }
    public static void require(boolean condition,String message) {
        if(!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
}
