package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.gui.ActionListeners.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 15:36
 */

/**
 * Represents one half of the user interface, displaying one
 * directory in some FSImage.
 * */
public class OneSidePanel extends JPanel{

  private Controller controller;
  private PseudoFile directory;

  private JScrollPane scrlFileTable;
  private FileTable fileTable;

  private JPanel pnlActions;
  private JButton btnParent;
  private JButton btnDelete;
  private JButton btnRename;
  private JButton btnCreateDirectory;

  private NavigationBar navigationBar;

  public FileTable getFileTable() {
    return fileTable;
  }

  public OneSidePanel(Controller controller, PseudoFile directory) {
    this.controller = controller;
    this.directory = directory;

    fileTable = new FileTable(controller, directory);

    init();

    addListeners();
  }

  private void init() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    pnlActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
    initPnlActions();
    add(pnlActions);
    add(Box.createVerticalStrut(2));

    navigationBar = new NavigationBar(fileTable, controller);
    add(navigationBar);
    add(Box.createVerticalStrut(2));

    scrlFileTable = new JScrollPane(fileTable);
    scrlFileTable.setPreferredSize(new Dimension(400,600));
    add(scrlFileTable);

  }

  private void initPnlActions() {
    btnParent = new JButton("Parent");
    pnlActions.add(btnParent);

    btnDelete = new JButton("Delete");
    pnlActions.add(btnDelete);

    btnRename = new JButton("Rename");
    pnlActions.add(btnRename);

    btnCreateDirectory = new JButton("New directory");
    pnlActions.add(btnCreateDirectory);
  }

  private void addListeners() {

    //open directories by double click
    fileTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          //just any action event
          ActionEvent ae = new ActionEvent(fileTable, 0, "open");
          new Open(fileTable, controller).actionPerformed(ae);
        }
      }
    });

    btnParent.addActionListener(new OpenParent(fileTable, controller));
    btnDelete.addActionListener(new Delete(fileTable, controller));
    btnRename.addActionListener(new Rename(fileTable, controller));
    btnCreateDirectory.addActionListener(new CreateDirectory(fileTable, controller));
  }


}



