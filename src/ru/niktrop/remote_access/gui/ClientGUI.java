package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:42
 */
public class ClientGUI extends JFrame {

  private JPanel jContentPane;

  private OneSidePanel pnlLeft;
  private OneSidePanel pnlRight;
  private static ClientGUI instance = null;

  public static ClientGUI instance() {
    if (instance == null) {
      instance = new ClientGUI();
    }
    return instance;
  }

  public void init(Controller controller) {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    FileTable leftFileTable = new FileTable(controller);
    FileTable rightFileTable = new FileTable(controller);
    OneSidePanel leftPanel = new OneSidePanel(leftFileTable, controller);
    OneSidePanel rightPanel = new OneSidePanel(rightFileTable, controller);

    jContentPane = new JPanel(new BorderLayout(3,3));
    jContentPane.add(leftPanel, BorderLayout.WEST);
    jContentPane.add(rightPanel, BorderLayout.EAST);

    setContentPane(jContentPane);

    pack();
    setLocationByPlatform(true);
    setMinimumSize(getSize());
    setVisible(true);
  }



}
