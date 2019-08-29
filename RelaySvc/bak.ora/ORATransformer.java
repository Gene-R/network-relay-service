/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relaysvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import oracle.jdbc.pool.OracleDataSource;

/**
 *
 * @author eugener
 */
public class ORATransformer {

    protected final Logger logger = Logger.getInstance();
    private File config;
    private Connection connection;
    private final Properties props;
    private String dbUrl = "";
    private String dbUser = "";
    private String dbPass = "";

    public ORATransformer(File config) {
        this.config = config;
        this.props = new Properties();
        try {
            OracleDataSource ods = new OracleDataSource();
            InputStream propsStream = new FileInputStream(config);
            this.props.load(propsStream);
            Enumeration keys = props.keys();
            while (keys.hasMoreElements()) {
                String value = ((String) keys.nextElement()).toUpperCase();
                if (value.equals("DBURL")) {
                    this.dbUrl = props.getProperty(value);
                } else if (value.equals("DBUSER")) {
                    this.dbUser = props.getProperty(value);
                } else if (value.equals("DBPASS")) {
                    this.dbPass = props.getProperty(value);
                }
            }
            if (dbUrl.isEmpty() || dbUser.isEmpty() || dbPass.isEmpty()) {
                throw new IllegalArgumentException("Incomplete database connectivity configuration in " + config.getAbsolutePath());
            }
            ods.setURL(this.dbUrl);
            ods.setUser(this.dbUser);
            ods.setPassword(this.dbPass);
            this.connection = ods.getConnection();
        } catch (Exception ex) {
            logger.error("ORATransformer(" + this.config.getAbsolutePath() + "): " + ex.getMessage());
        }

    }

    public void send(String sql) {
        try {
            if (connection != null) {
                logger.debug("\n\n==> Sending SQL transformation to [" + dbUrl + "] ... \n" + sql + "\n");
                Statement stmt = connection.createStatement();
                //stmt.executeQuery(sql); // curently disabled
            }
        } catch (SQLException ex) {
            logger.error("ORATransformer.send(" + sql + "): " + ex.getMessage());
        }

    }

}
