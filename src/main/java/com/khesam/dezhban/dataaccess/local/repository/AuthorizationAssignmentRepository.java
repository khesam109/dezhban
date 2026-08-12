package com.khesam.dezhban.dataaccess.local.repository;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class AuthorizationAssignmentRepository {

    private final JdbcOperations jdbc;

    public AuthorizationAssignmentRepository(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    public Set<String> findUserRoles(long userId) {
        return queryCodes("""
                SELECT R.CODE
                  FROM DEZHBAN.AUTHORIZATION_ROLE R
                  JOIN DEZHBAN.END_USER_AUTHORIZATION_ROLE UR ON UR.ROLE_ID = R.ID
                 WHERE UR.USER_ID = ?
                 ORDER BY R.CODE
                """, userId);
    }

    public Set<String> findClientRoles(long clientId) {
        return queryCodes("""
                SELECT R.CODE
                  FROM DEZHBAN.AUTHORIZATION_ROLE R
                  JOIN DEZHBAN.CLIENT_AUTHORIZATION_ROLE CR ON CR.ROLE_ID = R.ID
                 WHERE CR.CLIENT_ID = ?
                 ORDER BY R.CODE
                """, clientId);
    }

    public Set<String> findUserPermissions(long userId) {
        return queryCodes("""
                SELECT DISTINCT S.CODE
                  FROM DEZHBAN.AUTHORIZATION_SCOPE S
                  JOIN DEZHBAN.AUTHORIZATION_ROLE_SCOPE RS ON RS.SCOPE_ID = S.ID
                  JOIN DEZHBAN.END_USER_AUTHORIZATION_ROLE UR ON UR.ROLE_ID = RS.ROLE_ID
                 WHERE UR.USER_ID = ?
                 ORDER BY S.CODE
                """, userId);
    }

    public Set<String> findClientPermissions(long clientId) {
        return queryCodes("""
                SELECT DISTINCT S.CODE
                  FROM DEZHBAN.AUTHORIZATION_SCOPE S
                  JOIN DEZHBAN.AUTHORIZATION_ROLE_SCOPE RS ON RS.SCOPE_ID = S.ID
                  JOIN DEZHBAN.CLIENT_AUTHORIZATION_ROLE CR ON CR.ROLE_ID = RS.ROLE_ID
                 WHERE CR.CLIENT_ID = ?
                 ORDER BY S.CODE
                """, clientId);
    }

    public Set<String> findClientScopes(long clientId) {
        return queryCodes("""
                SELECT S.CODE
                  FROM DEZHBAN.AUTHORIZATION_SCOPE S
                  JOIN DEZHBAN.CLIENT_AUTHORIZATION_SCOPE CS ON CS.SCOPE_ID = S.ID
                 WHERE CS.CLIENT_ID = ?
                 ORDER BY S.CODE
                """, clientId);
    }

    public void replaceUserRoles(long userId, Set<Long> roleIds) {
        jdbc.update("DELETE FROM DEZHBAN.END_USER_AUTHORIZATION_ROLE WHERE USER_ID = ?", userId);
        roleIds.forEach(roleId -> jdbc.update("""
                INSERT INTO DEZHBAN.END_USER_AUTHORIZATION_ROLE (USER_ID, ROLE_ID)
                VALUES (?, ?)
                """, userId, roleId));
    }

    public void replaceClientRoles(long clientId, Set<Long> roleIds) {
        jdbc.update("DELETE FROM DEZHBAN.CLIENT_AUTHORIZATION_ROLE WHERE CLIENT_ID = ?", clientId);
        roleIds.forEach(roleId -> jdbc.update("""
                INSERT INTO DEZHBAN.CLIENT_AUTHORIZATION_ROLE (CLIENT_ID, ROLE_ID)
                VALUES (?, ?)
                """, clientId, roleId));
    }

    public void replaceClientScopes(long clientId, Set<Long> scopeIds) {
        jdbc.update("DELETE FROM DEZHBAN.CLIENT_AUTHORIZATION_SCOPE WHERE CLIENT_ID = ?", clientId);
        scopeIds.forEach(scopeId -> jdbc.update("""
                INSERT INTO DEZHBAN.CLIENT_AUTHORIZATION_SCOPE (CLIENT_ID, SCOPE_ID)
                VALUES (?, ?)
                """, clientId, scopeId));
    }

    private Set<String> queryCodes(String sql, long id) {
        List<String> codes = jdbc.query(sql, (resultSet, rowNum) -> resultSet.getString(1), id);
        return new LinkedHashSet<>(codes);
    }
}
