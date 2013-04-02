package ru.niktrop.remote_access.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 02.04.13
 * Time: 8:51
 */

/**
 * Simple window, that confirms start of the server and allows to shut down it.
 * */
public class ServerGUI extends JFrame {

  private static ServerGUI instance;

  private ServerGUI(String title) throws HeadlessException {
    super(title);
  }

  public static ServerGUI instance() {
    if (instance == null) {
      instance = new ServerGUI("Remote access server.");
    }
    return instance;
  }

  public void init() {
    String message = "Remote access server is running.";
    JLabel jLabel = new JLabel(message);

    JButton btnExit = new JButton("Exit");
    btnExit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });


    JPanel jContentPane = new JPanel(new BorderLayout());
    jContentPane.add(jLabel, BorderLayout.CENTER);
    jContentPane.add(btnExit, BorderLayout.SOUTH);
    jContentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
    setContentPane(jContentPane);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setResizable(false);

    pack();
    setLocationByPlatform(true);
    setMinimumSize(getSize());
    setVisible(true);
  }
}
