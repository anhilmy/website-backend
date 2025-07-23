package com.anhilmy.pos.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.anhilmy.pos.db.DbConnection;
import com.anhilmy.pos.model.MenuItem;

public class MenuRepository {
    public MenuItem findById(int id) {
        try (Connection conn = DbConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM menu_items WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                MenuItem row = new MenuItem(rs.getString("name"), rs.getString("description"),
                        rs.getBigDecimal("price"), rs.getInt("category_id"),
                        rs.getBoolean("is_available"));
                row.setItemId(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<MenuItem> findAll() {
        ArrayList<MenuItem> menuItems = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM menu_items");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MenuItem row = new MenuItem(rs.getString("name"), rs.getString("description"),
                        rs.getBigDecimal("price"), rs.getInt("category_id"),
                        rs.getBoolean("is_available"));
                row.setItemId(rs.getInt("id"));
                menuItems.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return menuItems;
    }
}
