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
import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import java.text.ParseException;
import java.util.Calendar;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


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
//    static String dbUser = "voshod";
//    static String dbPassword = "voshod";
    static Connection dbConn = null;
    //static int testInterval = 10;
    static Properties config = null;
    private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,};

    
    public interface SNet40 extends Library {
        SNet40 INSTANCE = (SNet40) Native.loadLibrary("c:\\windows\\system32\\snet40.dll", SNet40.class);
        int OpenSnet(String str1);
        NativeLong GetCurrentSnDbUser(byte[] str1, IntByReference num1, byte[] str2, IntByReference num2);
    }
    public static void main(String[] args) throws IOException, AWTException {
        init();
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
                checkAsp();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkShare();
            }
        }, 1, Long.parseLong(config.getProperty("testInterval")), TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                cleanOldLogs();
            }
        }, 0, 1, TimeUnit.DAYS);
        
        // Add system tray icon
        final SystemTray systemTray = SystemTray.getSystemTray();
        BufferedImage icon = ImageIO.read(App.class.getResourceAsStream("/images/trayIcon.png"));
        final TrayIcon trayIcon = new TrayIcon(icon.getScaledInstance(16, 16, Image.SCALE_SMOOTH), "Мониторинг");

        PopupMenu popupMenu = new PopupMenu();
        MenuItem itemRestart = new MenuItem("Перезапуск");
        itemRestart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                init();
                trayIcon.displayMessage("Мониторинг перезапущен", "Каталог с журналами: " + config.getProperty("logFolder") + "   ", TrayIcon.MessageType.INFO);
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
        
        trayIcon.displayMessage("Мониторинг запущен", "Каталог с журналами: " + new File( config.getProperty("logFolder") ).getAbsoluteFile() + "   ", TrayIcon.MessageType.INFO);
//        cleanOldLogs();
    }

    public static void checkPing() {
        long startDate = System.currentTimeMillis();
        //System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkPing start");
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
            logger.error(logFormat, "PING     ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage().trim());
        }
        //System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkPing end");
    }
    public static void checkTnsPing() {
        long startDate = System.currentTimeMillis();
        //System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkTnsPing start");
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
            logger.error(logFormat, "TNSPING  ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage().trim());
        }
        //System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkTnsPing end");
    }
    public static void checkShare() {
        //Date startDate = new Date();
        long startDate = System.currentTimeMillis();
        //System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkShare start");
        try {
            Date date = new Date();
            PrintWriter writer = new PrintWriter(new File(config.getProperty("shareFile")));
            writer.println( dt.format( date ) );
            writer.close();
            logger.info(logFormat, "SHARE    ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (FileNotFoundException ex) {
            logger.error(logFormat, "SHARE    ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage().trim());
        }
        //System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkShare end");
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
            logger.error(logFormat, "HTTP     ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", status + ": " + e.getMessage().trim() + html);
        }
    }
    public static void checkAsp() {
        int status = 0;
        long startDate = System.currentTimeMillis();
        StringBuffer html = new StringBuffer();
        try {
            String url = "http://" + config.getProperty("webServer") + "/shell/check.asp?sDBName=" + config.getProperty("dbName");
            if(config.getProperty("encryption").equals("1")) {
                url = url + "&DBUser=" + decrypt(config.getProperty("dbUser"));
                url = url + "&DBPass=" + decrypt(config.getProperty("dbPassword"));
            } else {
                url = url + "&DBUser=" + config.getProperty("dbUser");
                url = url + "&DBPass=" + config.getProperty("dbPassword");
            }


            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setReadTimeout(1000);

            status = conn.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                html.append(inputLine);
            }
            in.close();

            String aspOK = "CreateObject: OK. OpenDatabase: OK. CreateDynaset: OK. ";
            if( status == 200 && aspOK.equals(new String(html))) {
                logger.info(logFormat, "ASP      ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", html );
            } else if (status == 200) {
                logger.warn(logFormat, "ASP      ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", html );
            } else {
                logger.warn(logFormat, "ASP      ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", status + ": " + html );
            }
        } catch (Exception e) {
            logger.error(logFormat, "ASP      ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", status + ": " + e.getMessage().trim() + html);
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
            if(config.getProperty("encryption").equals("1")) {
                conn = DriverManager.getConnection(dbUrl, decrypt(config.getProperty("dbUser")), decrypt(config.getProperty("dbPassword")));
            } else {
                conn = DriverManager.getConnection(dbUrl, config.getProperty("dbUser"), config.getProperty("dbPassword"));
            }
            //System.out.println("Connection established");
            logger.info(logFormat, "DBCONNECT", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error(logFormat, "DBCONNECT", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage().trim() );
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error(logFormat, "DBCONNECT", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage().trim() );
                }
            }
        }
    }
    public static void checkDBQuery() {
        long startDate = System.currentTimeMillis();
        Statement stmt = null;
        try {
            if(dbConn == null) {
                throw new Exception("Отсутствует соединение с БД");
            }
            stmt = dbConn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + config.getProperty("dbTable") + " WHERE ROWNUM=1");
            if (rs.next()) {
                //System.out.println(rs.getString(1));
            }
            logger.info(logFormat, "DBQUERY  ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (Exception e) {
            logger.error(logFormat, "DBQUERY  ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage().trim() );
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
        config = getDefaultConfig();
        System.err.println(config.getProperty("dbName"));
        try {
            InputStream in = new FileInputStream(new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent() + File.separator + "config.properties");
            config.load(in);
            in.close();
        } catch (FileNotFoundException ex) {
            // Файл с настройками не найден
            newFile = true;
        } catch (IOException ex) {
            logger.error(logFormat, ex.getMessage().trim());
        }
        
        if(newFile && config.size() > 0) {
            // Файл с настройками не найден - создаём новый
            try {
                String configPath = new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
                config.store(new FileOutputStream(configPath + File.separator + "config.properties"), "");
                System.out.println("Trying to write settings to " + configPath + File.separator + "config.properties");
            } catch (IOException ex) {
                logger.error(logFormat, ex.getMessage().trim());
            }
        }
        return true;
    }
    public static void init() {
        readSettings();
        dt = new SimpleDateFormat(config.getProperty("dateTimeFormat"));
        // Set logging folder
        System.setProperty("logFolder", config.getProperty("logFolder"));
        org.apache.logging.log4j.core.LoggerContext ctx = 
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
        
        // Connect to Oracle
        dbUrl = "jdbc:oracle:thin:@" + config.getProperty("dbName");
        long startDate = System.currentTimeMillis();
        System.setProperty("oracle.net.tns_admin", config.getProperty("tnsAdmin"));
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        try {
            if(dbConn != null ) {
                dbConn.close();
            }
            if(config.getProperty("encryption").equals("1")) {
                dbConn = DriverManager.getConnection(dbUrl, decrypt(config.getProperty("dbUser")), decrypt(config.getProperty("dbPassword")));
            } else {
                dbConn = DriverManager.getConnection(dbUrl, config.getProperty("dbUser"), config.getProperty("dbPassword"));
            }

        } catch (Exception e) {
//            System.err.println("Date1: " + dt.format( new Date(startDate) ));
//            System.err.println("Date2: " + (System.currentTimeMillis() - startDate));
//            System.err.println("MSG: " + e.getMessage());
            //System.err.println("MSGt: " + e.getMessage().trim());
            logger.error(logFormat, "*** INIT ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", e.getMessage().trim() );
        }
    }
    public static Properties getDefaultConfig() {
        byte[] str1 = new byte[256];
        byte[] str2 = new byte[256];
        IntByReference num1 = new IntByReference(256);
        IntByReference num2 = new IntByReference(256);
        Properties defaultConfig = new Properties();
        defaultConfig.setProperty("dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
        defaultConfig.setProperty("webServer", "spo-cikd");
        defaultConfig.setProperty("fileServer", "spo-cikd");
        defaultConfig.setProperty("logFolder", "c:\\gas_m\\pochta\\file");
        defaultConfig.setProperty("dbName", "RA00C000");
        defaultConfig.setProperty("testInterval", "10");
        defaultConfig.setProperty("tnsAdmin", "c:\\oracle\\product\\11.2.0\\client_1\\network\\admin" );
        defaultConfig.setProperty("shareFile", "g:\\gas_m\\paip\\CheckShare.txt" );
        defaultConfig.setProperty("rollingInterval", "30" );
        defaultConfig.setProperty("dbTable", "arm" );
        defaultConfig.setProperty("encryption", "1" );
        SNet40.INSTANCE.OpenSnet("");
        SNet40.INSTANCE.GetCurrentSnDbUser(str1, num1, str2, num2);
        String login = new String(str1);
        String password = new String( str2 );
        login = login.replace("\u0000", "");
        login = login.replace("\\u0000", "");
        password = password.replace("\u0000", "");
        password = password.replace("\\u0000", "");
        /*
        String login = "voshod";
        String password = "voshod";
        */
        try {
            defaultConfig.setProperty("dbUser", encrypt(login));
            defaultConfig.setProperty("dbPassword", encrypt(password));
            defaultConfig.setProperty("encryption", "1" );
        } catch (Exception ex) {
            logger.error(logFormat, ex.getMessage().trim());
            defaultConfig.setProperty("dbUser", login);
            defaultConfig.setProperty("dbPassword", password);
            defaultConfig.setProperty("encryption", "0" );
        }

        return defaultConfig;
    }
    public static void cleanOldLogs() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1 * Integer.parseInt(config.getProperty("rollingInterval")));
        Date cleanDate = c.getTime();
        File[] listOfFiles = new File(config.getProperty("logFolder")).listFiles();
        for(File f : listOfFiles) {
            if(f.isFile()) {
                if(f.getName().startsWith("monitoring-")) {
                    try {
                        Date d = dateFormat.parse(f.getName().substring(11, 21));
                        if( d.compareTo(cleanDate) < 0 ) {
                            f.delete();
                        }
                    } catch (Exception ex) {
                        logger.error(logFormat, ex.getMessage().trim());
                    }
                    
                }
            }
        }
    }
    private static String encrypt(String property) throws GeneralSecurityException, UnsupportedEncodingException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
    }

    private static String base64Encode(byte[] bytes) {
        // NB: This class is internal, and you probably should use another impl
        return new BASE64Encoder().encode(bytes);
    }

    private static String decrypt(String property) throws GeneralSecurityException, IOException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    private static byte[] base64Decode(String property) throws IOException {
        // NB: This class is internal, and you probably should use another impl
        return new BASE64Decoder().decodeBuffer(property);
    }
}
