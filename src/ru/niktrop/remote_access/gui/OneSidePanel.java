package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

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
public class OneSidePanel extends JPanel{

  private Controller controller;
  private PseudoFile directory;

  private JScrollPane scrlFileTable;
  private FileTable fileTable;

  private JPanel pnlActions;
  private JButton btnParent;
  private JButton btnOpen;
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

    navigationBar = new NavigationBar(fileTable, controller);
    add(navigationBar);

    scrlFileTable = new JScrollPane(fileTable);
    add(scrlFileTable);

  }

  private void initPnlActions() {
    btnParent = new JButton("Parent");
    pnlActions.add(btnParent);

    btnOpen = new JButton("Open");
    pnlActions.add(btnOpen);

    btnDelete = new JButton("Delete");
    pnlActions.add(btnDelete);

    btnRename = new JButton("Rename");
    pnlActions.add(btnRename);

    btnCreateDirectory = new JButton("Create");
    pnlActions.add(btnCreateDirectory);
  }

  private void addListeners() {

    //open directories by double click
    fileTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          //just any action event
          ActionEvent ae = new ActionEvent(fileTable, 0, "open");
          new OpenDirectoryAction(fileTable, controller).actionPerformed(ae);
        }
      }
    });

    btnParent.addActionListener(new OpenParentAction(fileTable, controller));
    btnOpen.addActionListener(new OpenDirectoryAction(fileTable, controller));
    btnDelete.addActionListener(new DeleteAction(fileTable, controller));
    btnRename.addActionListener(new RenameAction(fileTable, controller));
    btnCreateDirectory.addActionListener(new CreateDirectoryAction(fileTable, controller));

    controller.addListener(fileTable);
  }


}



