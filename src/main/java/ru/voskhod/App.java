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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    static final Logger logger = LogManager.getLogger(App.class.getName());
    // SRC      START      DURATION     RESULT      INFO
    static final String logFormat = "{}\t{}\t{}\t{}\t{}";
    static final SimpleDateFormat dt = new SimpleDateFormat("HH:mm:ss dd.mm.yyyy");
    
    public static void main(String[] args) throws IOException, AWTException {
        
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkPing();
            }
        }, 1, 2, TimeUnit.SECONDS);
        
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkShare();
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        final SystemTray systemTray = SystemTray.getSystemTray();
        final TrayIcon trayIcon = new TrayIcon(ImageIO.read(new File( App.class.getResource("/trayIcon.png").getFile() )), "Мониторинг");

        PopupMenu popupMenu = new PopupMenu();
        MenuItem itemRestart = new MenuItem("Перезапуск");
        itemRestart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        popupMenu.add(itemRestart);
        
        MenuItem itemExit = new MenuItem("Выход");
        itemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scheduledExecutorService.shutdown();
                systemTray.remove(trayIcon);
            }
        });
        popupMenu.add(itemExit);
        
        trayIcon.setPopupMenu(popupMenu);
        systemTray.add(trayIcon);
        trayIcon.setImageAutoSize(true);
    }

    public static void checkPing() {
        Date date = new Date();
        //System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ping...");
        //int d = (int) Math.random() * 1000;
        long delay = 241;
        try {
            Thread.sleep(delay);
            System.out.println("sleep " + delay);
        } catch (InterruptedException ex) {
            logger.error("*** Ошибка преобразования типов ***");
        }
        logger.info(logFormat, "PING", "date_start", "OK", dt.format( date ) );
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
            logger.info(logFormat, "SHARE", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "OK", "-" );
        } catch (FileNotFoundException ex) {
            logger.error(logFormat, "SHARE ", dt.format( new Date(startDate) ), System.currentTimeMillis() - startDate, "Ошибка", ex.getMessage());
        }
        System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " checkShare end");
    }
    public static void checkIIS() {
        String url = "http://spo-cikd/check.asp";
        String charset = "UTF-8";
        try {
            /*
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", charset);
            InputStream response = connection.getInputStream();
            */
            
            InputStream connection = new URL(url).openStream();
            BufferedReader in = new BufferedReader( new InputStreamReader(connection));
            String line;
            while((line = in.readLine()) != null) {
                System.out.println( line );
            }
            in.close();
        } catch (Exception ex) {
            //Logger.getLogger(MonitoringItems.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void checkHttp() {
        int status = 0;
        try {
            String url = "http://spo-cikd/check.asp";

            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setReadTimeout(1000);
            System.out.println("Request URL ... " + url);

            status = conn.getResponseCode();
            System.out.println("Response Code ... " + status);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer html = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                html.append(inputLine);
            }
            in.close();

            System.out.println("URL Content... \n" + html.toString());
            System.out.println("Done");
            logger.info("HTTP - " + status);
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Error code: " + status);
            logger.warn("HTTP - " + status);
        }
    }
}
