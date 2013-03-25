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
  private Controller controller;

  private JButton btnCopy;
  private OneSidePanel pnlLeft;
  private OneSidePanel pnlRight;
  private static ClientGUI instance = null;

  private ClientGUI() {
  }

  public static ClientGUI instance() {
    if (instance == null) {
      instance = new ClientGUI();
    }
    return instance;
  }

  public void init(Controller controller) {
    this.controller = controller;
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    FileTable leftFileTable = new FileTable(controller);
    FileTable rightFileTable = new FileTable(controller);
    pnlLeft = new OneSidePanel(leftFileTable, controller);
    pnlRight = new OneSidePanel(rightFileTable, controller);


    jContentPane = new JPanel(new BorderLayout(3,3));
    jContentPane.add(pnlLeft, BorderLayout.WEST);
    jContentPane.add(pnlRight, BorderLayout.EAST);

    btnCopy = new JButton("Copy");
    btnCopy.addActionListener(new CopyAction(leftFileTable, rightFileTable, controller));
    jContentPane.add(btnCopy, BorderLayout.SOUTH);

    setContentPane(jContentPane);

    pack();
    setLocationByPlatform(true);
    setMinimumSize(getSize());
    setVisible(true);
  }

}
