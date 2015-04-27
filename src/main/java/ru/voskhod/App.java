package ru.voskhod;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    static final Logger logger = LogManager.getLogger(App.class.getName());
    static final String logFormat = "{}\t{}\t{}\t{}\t{}";
    static SimpleDateFormat dt = null;
    //static String webServer = "yahoo.com";
    //static String webServer = "spo-cikd";
    //static String fileServer = "spo-cikd";
    //static String logFolder = ".";
    //static String dbName = "RT0011";
    //static String dbName = "RA00C000";
    static String dbUrl = null;
    static String dbUser = "voshod";
    static String dbPassword = "voshod";
    static Connection dbConn = null;
    //static int testInterval = 10;
    static Properties config = new Properties();
    
    public static void main(String[] args) throws IOException, AWTException {
        readSettings();
        dbUrl = "jdbc:oracle:thin:@" + config.getProperty("dbName");
        dt = new SimpleDateFormat(config.getProperty("dateTimeFormat"));
        //dt = new SimpleDateFormat()
        // Connect to Oracle
        System.setProperty("oracle.net.tns_admin", config.getProperty("tnsAdmin"));
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        try {
            dbConn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // Set logging folder
        System.setProperty("logFolder", config.getProperty("logFolder"));
        org.apache.logging.log4j.core.LoggerContext ctx = 
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        // Prepare executors
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        
        // Run tests
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkDBQuery();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkDBConnect();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkTnsPing();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkPing();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkHttp();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkShare();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        // Add system tray icon
        final SystemTray systemTray = SystemTray.getSystemTray();
        BufferedImage icon = ImageIO.read(App.class.getResourceAsStream("/images/trayIcon.png"));
        final TrayIcon trayIcon = new TrayIcon(icon.getScaledInstance(16, 16, Image.SCALE_SMOOTH), "Мониторинг");

        PopupMenu popupMenu = new PopupMenu();
        MenuItem itemRestart = new MenuItem("Перезапуск");
        itemRestart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        popupMenu.add(itemRestart);
        
        // Add menu
        MenuItem itemExit = new MenuItem("Выход");
        itemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scheduledExecutorService.shutdown();
                systemTray.remove(trayIcon);
                if (dbConn != null) {
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                }

            }
        });
        popupMenu.add(itemExit);
        
        trayIcon.setPopupMenu(popupMenu);
        systemTray.add(trayIcon);
        trayIcon.setImageAutoSize(true);
        //trayIcon.displayMessage("Программа диагностики запущена", "", TrayIcon.MessageType.INFO);
        
        trayIcon.displayMessage("", "Запуск мониторинга.\nКаталог с журналами: " + config.getProperty("logFolder") + "   ", TrayIcon.MessageType.INFO);
    }

    public static void checkPing() {
        long startDate = System.currentTimeMillis();
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkPing start");
        Process p = null;
        try {
            String line;
//            p = Runtime.getRuntime().exec( "ping yahoo.com -n 1" );
            p = Runtime.getRuntime().exec( "ping " + config.getProperty("fileServer") + " -n 1" );
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), "Cp866"));
            String output = "";
            while((line = input.readLine()) != null ) {
                output += line + " ";
            }
            p.destroy();
            int startPos = output.toString().indexOf("время");
            int stopPos = output.toString().indexOf("мс ");
            String duration = "";
            String info = "-";
            if( startPos > 0 && stopPos > 0 ) {
                duration = output.substring(startPos + 6, stopPos);
                //System.err.println("parse: " + duration);
                logger.info(logFormat, "PING     ", dt.format( new Date(startDate) ), duration, "OK", info );
            } else {
                //System.err.println("not found");
                duration = new Long( System.currentTimeMillis() - startDate ).toString();
                info = "Время выполнения процесса";
                logger.warn(logFormat, "PING     ", dt.format( new Date(startDate) ), duration, "Ошибка", output );
            }
            //logger.info(logFormat, "PING", dt.format( new Date(startDate) ), duration, "OK", info );
        } catch (Exception ex) {
            logger.error(logFormat, "PING     ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage());
        }
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkPing end");
    }
    public static void checkTnsPing() {
        long startDate = System.currentTimeMillis();
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkTnsPing start");
        Process p = null;
        try {
            String line;
            p = Runtime.getRuntime().exec( "tnsping " + config.getProperty("dbName") );
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), "Cp866"));
            String output = "";
            while((line = input.readLine()) != null ) {
                output += line + " ";
            }
            p.destroy();
            
            Pattern pattern = Pattern.compile("OK\\s\\((.*?)\\s");
            Matcher m = pattern.matcher(output);

            String duration = "";
            String info = "-";
            if (m.find()) {
                duration = m.group(1);
                logger.info(logFormat, "TNSPING  ", dt.format( new Date(startDate) ), duration, "OK", info );
            } else {
                pattern = Pattern.compile("TNS-(.*?)$");
                m = pattern.matcher(output);
                if (m.find()) {
                    info = m.group(0);
                } else {
                    info = output;
                }
                duration = new Long( System.currentTimeMillis() - startDate ).toString();
                logger.warn(logFormat, "TNSPING  ", dt.format( new Date(startDate) ), duration, "Ошибка", info );
            }
        } catch (Exception ex) {
            logger.error(logFormat, "TNSPING  ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage());
        }
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkTnsPing end");
    }
    public static void checkShare() {
        //Date startDate = new Date();
        long startDate = System.currentTimeMillis();
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkShare start");
        try {
            Date date = new Date();
            SimpleDateFormat dt = new SimpleDateFormat("HH:mm:ss dd.mm.yyyy");
            PrintWriter writer = new PrintWriter(new File("g:\\gas_m\\paip\\CheckShare.txt"));
            writer.println( dt.format( date ) );
            writer.close();
            logger.info(logFormat, "SHARE    ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (FileNotFoundException ex) {
            logger.error(logFormat, "SHARE    ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage());
        }
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkShare end");
    }
    public static void checkHttp() {
        int status = 0;
        long startDate = System.currentTimeMillis();
        StringBuffer html = new StringBuffer();
        try {
            String url = "http://" + config.getProperty("webServer") + "/shell/index.html";

            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setReadTimeout(1000);
            //System.out.println("Request URL ... " + url);

            status = conn.getResponseCode();
            //System.out.println("Response Code ... " + status);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                html.append(inputLine);
            }
            in.close();

            //System.out.println("URL Content... \n" + html.toString());
            //System.out.println("Done");
            if( status == 200 ) {
                logger.info(logFormat, "HTTP     ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
            } else {
                logger.warn(logFormat, "HTTP     ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", status + ": " + html );
            }
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Error code: " + status);
            logger.error(logFormat, "HTTP     ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", status + ": " + e.getMessage() + html);
        }
    }
    public static void checkDBConnect() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        Connection conn = null;
        long startDate = System.currentTimeMillis();
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Connection established");
            logger.info(logFormat, "DBCONNECT", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(logFormat, "DBCONNECT", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage() );
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error(logFormat, "DBCONNECT", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage() );
                }
            }
        }
    }
    public static void checkDBQuery() {
        long startDate = System.currentTimeMillis();
        Statement stmt = null;
        try {
            stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT idarm FROM arm");
            if (rs.next()) {
                System.out.println(rs.getString(1));
            }
            logger.info(logFormat, "DBQUERY  ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(logFormat, "DBQUERY  ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage() );
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                }
            }
        }
    }
    public static boolean readSettings() {
        boolean newFile = false;
        try {
            InputStream in = new FileInputStream(new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent() + File.separator + "config.properties");
            config.load(in);
            in.close();
        } catch (FileNotFoundException ex) {
            // Файл с настройками не найден - применяем настройки по умолчанию
            config.setProperty("dateTimeFormat", "HH:mm:ss dd.MM.yyyy");
            config.setProperty("webServer", "spo-cikd");
            config.setProperty("fileServer", "spo-cikd");
            config.setProperty("logFolder", ".");
            config.setProperty("dbName", "RA00C000");
            config.setProperty("testInterval", "10");
            config.setProperty("tnsAdmin", "c:\\oracle\\product\\11.2.0\\client_1\\network\\admin" );
            newFile = true;
        } catch (IOException ex) {
            logger.error(logFormat, ex.getMessage());
        }
        // Файл с настройками не найден - создаём новый
        if(newFile) {
            try {
                String configPath = new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
                config.store(new FileOutputStream(configPath + File.separator + "config.properties"), "");
                System.out.println("Trying to write settings to " + configPath + File.separator + "config.properties");
            } catch (IOException ex) {
                logger.error(logFormat, ex.getMessage());
            }
        }
        return true;
    }
}
