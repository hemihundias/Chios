/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ad.chios;

/**
 *
 * @author Hemihundias
 */
public class BaseDatos {
    private String address;
    private String username;
    private int port;
    private String dbname;
    private String password;

    public BaseDatos() {
    }

    
    public BaseDatos(String address, String username, int port, String dbname, String password) {
        this.address = address;
        this.username = username;
        this.port = port;
        this.dbname = dbname;
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbname() {
        return dbname;
    }

    public void setDbname(String dbname) {
        this.dbname = dbname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
}
