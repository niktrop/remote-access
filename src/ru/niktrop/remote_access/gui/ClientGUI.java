package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:42
 */
public class ClientGUI extends JFrame {
  private static final Logger LOG = Logger.getLogger(ClientGUI.class.getName());

  private JPanel jContentPane;
  private Controller controller;

  private JButton btnCopyToRight;
  private JButton btnCopyToLeft;
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

    initSidePanels();

    initCopyButtons(controller);

    jContentPane = new JPanel();
    jContentPane.setLayout(new BoxLayout(jContentPane, BoxLayout.X_AXIS));
    jContentPane.setBorder(new EmptyBorder(5,5,5,5));

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
    btnCopyToRight = new JButton("=>");
    btnCopyToRight.addActionListener(new CopyAction(pnlLeft.getFileTable(), pnlRight.getFileTable(), controller));
    btnCopyToRight.setSize(btnCopyToRight.getMinimumSize());

    btnCopyToLeft = new JButton("<=");
    btnCopyToLeft.addActionListener(new CopyAction(pnlRight.getFileTable(), pnlLeft.getFileTable(), controller));
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
    PseudoFile leftDir = controller.getDefaultDirectory(controller.fsImages.getLocal());
    PseudoFile rightDir = controller.getDefaultDirectory(controller.fsImages.getRemote());

    pnlLeft = new OneSidePanel(controller, leftDir);
    pnlRight = new OneSidePanel(controller, rightDir);
  }

}
