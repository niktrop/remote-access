package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.gui.ActionListeners.Copy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:42
 */
public class ClientGUI extends JFrame {

  private Controller controller;

  private OneSidePanel pnlLeft;
  private OneSidePanel pnlRight;
  private JPanel pnlCenter;
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
    setResizable(false);

    initSidePanels();

    initCopyButtons(controller);

    JPanel jContentPane = new JPanel();
    jContentPane.setLayout(new BoxLayout(jContentPane, BoxLayout.X_AXIS));
    jContentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

    jContentPane.add(pnlLeft);
    jContentPane.add(Box.createHorizontalStrut(5));
    jContentPane.add(pnlCenter);
    jContentPane.add(Box.createHorizontalStrut(5));
    jContentPane.add(pnlRight);

    setContentPane(jContentPane);

    pack();
    setLocationByPlatform(true);
    setMinimumSize(getSize());
    setVisible(true);
  }

  private void initCopyButtons(Controller controller) {
    JButton btnCopyToRight = new JButton("=>");
    btnCopyToRight.addActionListener(new Copy(pnlLeft.getFileTable(), pnlRight.getFileTable(), controller));
    btnCopyToRight.setSize(btnCopyToRight.getMinimumSize());

    JButton btnCopyToLeft = new JButton("<=");
    btnCopyToLeft.addActionListener(new Copy(pnlRight.getFileTable(), pnlLeft.getFileTable(), controller));
    btnCopyToLeft.setSize(btnCopyToLeft.getMinimumSize());

    pnlCenter = new JPanel();
    pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
    pnlCenter.add(Box.createVerticalGlue());
    pnlCenter.add(btnCopyToRight);
    pnlCenter.add(Box.createVerticalStrut(3));
    pnlCenter.add(btnCopyToLeft);
    pnlCenter.add(Box.createVerticalGlue());
    pnlCenter.add(Box.createVerticalGlue());
    pnlCenter.add(Box.createVerticalGlue());
  }

  private void initSidePanels() {
    PseudoFile defaultDirectory = controller.fsImages.defaultDirectory();

    pnlLeft = new OneSidePanel(controller, defaultDirectory);
    pnlRight = new OneSidePanel(controller, defaultDirectory);
  }

}
