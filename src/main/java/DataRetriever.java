import java.sql.*;
import java.util.*;

public class DataRetriever {

    public Dish findDishById(Integer id) {
        Dish dish = null;
        String dishQuery = "SELECT * FROM Dish WHERE id = ?";
        String ingredientQuery = "SELECT * FROM Ingredient WHERE dish_id = ?";

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement dishStmt = conn.prepareStatement(dishQuery);
             PreparedStatement ingredientStmt = conn.prepareStatement(ingredientQuery)) {

            dishStmt.setInt(1, id);
            ResultSet rsDish = dishStmt.executeQuery();
            if (rsDish.next()) {
                dish = new Dish(
                        rsDish.getInt("id"),
                        rsDish.getString("name"),
                        DishTypeEnum.valueOf(rsDish.getString("dish_type"))
                );

                ingredientStmt.setInt(1, id);
                ResultSet rsIng = ingredientStmt.executeQuery();
                while (rsIng.next()) {
                    Ingredient ingredient = new Ingredient(
                            rsIng.getInt("id"),
                            rsIng.getString("name"),
                            rsIng.getDouble("price"),
                            CategoryEnum.valueOf(rsIng.getString("category")),
                            dish
                    );
                    dish.addIngredient(ingredient);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dish;
    }

    public List<Ingredient> findIngredients(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        String query = "SELECT * FROM Ingredient ORDER BY id LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, size);
            stmt.setInt(2, (page - 1) * size);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ingredients.add(new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        null
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredients;
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        List<Ingredient> createdIngredients = new ArrayList<>();
        String checkQuery = "SELECT COUNT(*) FROM Ingredient WHERE id = ?";
        String insertQuery = "INSERT INTO Ingredient (id, name, price, category, dish_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getDBConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {

                for (Ingredient ing : newIngredients) {
                    checkStmt.setInt(1, ing.getId());
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new RuntimeException("Ingredient deja existe: " + ing.getName());
                    }

                    insertStmt.setInt(1, ing.getId());
                    insertStmt.setString(2, ing.getName());
                    insertStmt.setDouble(3, ing.getPrice());
                    insertStmt.setString(4, ing.getCategory().name());
                    insertStmt.setObject(5, ing.getDish() != null ? ing.getDish().getId() : null);
                    insertStmt.executeUpdate();
                    createdIngredients.add(ing);
                }

                conn.commit();

            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return createdIngredients;
    }

    public Dish saveDish(Dish dishToSave) {
        String checkQuery = "SELECT COUNT(*) FROM Dish WHERE id = ?";
        String insertDish = "INSERT INTO Dish (id, name, dish_type) VALUES (?, ?, ?)";
        String updateDish = "UPDATE Dish SET name = ?, dish_type = ? WHERE id = ?";
        String updateIngredientDish = "UPDATE Ingredient SET dish_id = ? WHERE id = ?";

        try (Connection conn = DBConnection.getDBConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertDish);
                 PreparedStatement updateStmt = conn.prepareStatement(updateDish);
                 PreparedStatement updateIngStmt = conn.prepareStatement(updateIngredientDish)) {

                // Vérifier si le plat existe
                checkStmt.setInt(1, dishToSave.getId());
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                boolean exists = rs.getInt(1) > 0;

                if (!exists) {
                    insertStmt.setInt(1, dishToSave.getId());
                    insertStmt.setString(2, dishToSave.getName());
                    insertStmt.setString(3, dishToSave.getDishType().name());
                    insertStmt.executeUpdate();
                } else {
                    updateStmt.setString(1, dishToSave.getName());
                    updateStmt.setString(2, dishToSave.getDishType().name());
                    updateStmt.setInt(3, dishToSave.getId());
                    updateStmt.executeUpdate();
                }

                // Mettre à jour les ingrédients associés
                for (Ingredient ing : dishToSave.getIngredients()) {
                    updateIngStmt.setInt(1, dishToSave.getId());
                    updateIngStmt.setInt(2, ing.getId());
                    updateIngStmt.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dishToSave;
    }

    public List<Dish> findDishsByIngredientName(String ingredientName) {
        List<Dish> dishes = new ArrayList<>();
        String query = "SELECT DISTINCT d.id, d.name, d.dish_type " +
                "FROM Dish d JOIN Ingredient i ON d.id = i.dish_id " +
                "WHERE i.name ILIKE ?";

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + ingredientName + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Dish dish = new Dish(
                        rs.getInt("id"),
                        rs.getString("name"),
                        DishTypeEnum.valueOf(rs.getString("dish_type"))
                );
                dishes.add(dish);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dishes;
    }

    public List<Ingredient> findIngredientsByCriteria(String ingredientName,
                                                      CategoryEnum category,
                                                      String dishName,
                                                      int page,
                                                      int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT i.*, d.id AS dish_id, d.name AS dish_name, d.dish_type " +
                "FROM Ingredient i LEFT JOIN Dish d ON i.dish_id = d.id WHERE 1=1 ");

        if (ingredientName != null && !ingredientName.isEmpty()) {
            query.append("AND i.name ILIKE ? ");
        }
        if (category != null) {
            query.append("AND i.category = ? ");
        }
        if (dishName != null && !dishName.isEmpty()) {
            query.append("AND d.name ILIKE ? ");
        }

        query.append("ORDER BY i.id LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            int index = 1;
            if (ingredientName != null && !ingredientName.isEmpty()) {
                stmt.setString(index++, "%" + ingredientName + "%");
            }
            if (category != null) {
                stmt.setString(index++, category.name());
            }
            if (dishName != null && !dishName.isEmpty()) {
                stmt.setString(index++, "%" + dishName + "%");
            }

            stmt.setInt(index++, size);
            stmt.setInt(index, (page - 1) * size);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Dish dish = null;
                int dishId = rs.getInt("dish_id");
                if (dishId != 0) {
                    dish = new Dish(
                            dishId,
                            rs.getString("dish_name"),
                            DishTypeEnum.valueOf(rs.getString("dish_type"))
                    );
                }

                ingredients.add(new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        dish
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredients;
    }
}