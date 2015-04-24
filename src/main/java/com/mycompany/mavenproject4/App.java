package com.mycompany.mavenproject4;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class App {

    public static void main(String[] args) throws IOException, AWTException {
        
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                checkPing();
            }
        }, 1, 2, TimeUnit.SECONDS);
        
        //new App().createTrayIcon();
        
//        System.out.println( App.class.getResource("/trayIcon.png").getFile() );
//        System.out.println( App.class.getResource("/trayIcon.png").getPath() );
        
        final SystemTray systemTray = SystemTray.getSystemTray();
        final TrayIcon trayIcon = new TrayIcon(ImageIO.read(new File( App.class.getResource("/trayIcon.png").getFile() )), "Tray test application");

        PopupMenu popupMenu = new PopupMenu();
        MenuItem itemRestart = new MenuItem("Restart");
        itemRestart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        popupMenu.add(itemRestart);
        
        MenuItem itemExit = new MenuItem("Exit");
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
        System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " ping...");
    }

    public void createTrayIcon() {
        TrayIcon trayIcon = null;

        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            ClassLoader classLoader = getClass().getClassLoader();
            //File file = new File(classLoader.getResource("ut.icons.png").getFile());
            Image image = Toolkit.getDefaultToolkit().getImage("f:\\@works\\Java\\mavenproject4\\src\\main\\resources\\uticons.png");
            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    /*
                    switch (e.getActionCommand()) {
                        case "EXIT":
                            System.exit(0);
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                            */
                }
            };
            // create a popup menu
            PopupMenu popup = new PopupMenu();
            // create menu item for the default action
            MenuItem defaultItem = new MenuItem("Выход");
            defaultItem.setActionCommand("EXIT");
            defaultItem.addActionListener(listener);
            popup.add(defaultItem);
        /// ... add other items
            // construct a TrayIcon
//            trayIcon = new TrayIcon(image, "Tray Demo", popup);
            trayIcon = new TrayIcon( new ImageIcon("f:\\@works\\Java\\mavenproject4\\src\\main\\resources\\uticons.png").getImage());
            // set the TrayIcon properties
            trayIcon.addActionListener(listener);
        // ...
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println(e);
            }
            // ...
        } else {
         // disable tray option in your application or
            // perform other actions
            //    ...
        }
    }
}
