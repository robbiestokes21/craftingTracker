package com.dstokesncstudio.craftingTracker.db;


import com.dstokesncstudio.craftingTracker.util.ConsoleColor;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.bukkit.Bukkit.getLogger;


public class Database {
    private Connection connection;

    private final String HOST;
    private final int PORT;
    private final String USER;
    private final String PASSWORD;
    private final String DATABASE_NAME;
    private final String TYPE;


    public Database(String HOST, int PORT, String USER, String PASSWORD, String DATABASE_NAME,  String TYPE) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.USER = USER;
        this.PASSWORD = PASSWORD;
        this.DATABASE_NAME = DATABASE_NAME;
        this.TYPE = TYPE;

        // Print colored message
        getLogger().info(ConsoleColor.YELLOW + " Connecting to database..." + ConsoleColor.RESET);

    }

    public Connection getConnection() throws SQLException {

        if(connection != null){
            return connection;
        }
        String url = "jdbc:"+TYPE+"://"+HOST+":"+PORT+"/"+DATABASE_NAME;


        this.connection = DriverManager.getConnection(url, this.USER, this.PASSWORD);

        System.out.println("Connected to database "+ ConsoleColor.GREEN + url + ConsoleColor.RESET);
        return connection;
    }
    public void initializeDatabase() throws SQLException {

        Statement statement = getConnection().createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS crafting_tracker_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "items LONGTEXT NOT NULL)";
        statement.execute(sql);
        statement.close();
        System.out.println(ConsoleColor.GREEN + "Created the table crafting_tracker_stats." + ConsoleColor.RESET);

    }
}
