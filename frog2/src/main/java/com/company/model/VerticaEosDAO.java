package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.company.util.DBConnection;

public class VerticaEosDAO {

    // Legacy comment normalized during UTF-8 migration.
    public java.util.Date findEosDateByVersion(String versionText) {
        if (versionText == null || versionText.trim().isEmpty()) {
            return null;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();

            // Legacy comment normalized during UTF-8 migration.
            String schemaDetectSql = "SELECT table_schema FROM v_catalog.tables " +
                "WHERE lower(table_name) = 'vertica_eos' ORDER BY CASE lower(table_schema) WHEN 'public' THEN 0 ELSE 1 END LIMIT 1";
            pstmt = conn.prepareStatement(schemaDetectSql);
            rs = pstmt.executeQuery();
            String schemaName = null;
            if (rs.next()) {
                schemaName = rs.getString(1);
            }
            DBConnection.close(rs, pstmt);

            String qualifiedTable = (schemaName != null && !schemaName.isEmpty())
                ? ("\"" + schemaName + "\".\"vertica_eos\"")
                : "vertica_eos";

            // Legacy comment normalized during UTF-8 migration.
            String[] candidateCols = new String[] { "vertica_version", "version" };
            for (String col : candidateCols) {
                String quotedCol = "\"" + col + "\"";
                String sql =
                    "SELECT end_of_service_date, 1 AS p FROM " + qualifiedTable + " WHERE " + quotedCol + " = ? " +
                    "UNION ALL " +
                    "SELECT end_of_service_date, 2 AS p FROM " + qualifiedTable + " WHERE ? ILIKE ('%' || " + quotedCol + " || '%') " +
                    "ORDER BY p, LENGTH(" + quotedCol + ") DESC " +
                    "LIMIT 1";

                try {
                    pstmt = conn.prepareStatement(sql);
                } catch (SQLException prepareEx) {
                    // Legacy comment normalized during UTF-8 migration.
                    DBConnection.close(pstmt);
                    continue;
                }

                pstmt.setString(1, versionText.trim());
                pstmt.setString(2, versionText.trim());
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    Timestamp eosTs = rs.getTimestamp(1);
                    if (eosTs != null) {
                        return new java.util.Date(eosTs.getTime());
                    }
                }
                DBConnection.close(rs, pstmt);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }
        return null;
    }
}


