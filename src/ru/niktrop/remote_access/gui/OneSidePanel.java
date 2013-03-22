package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 15:36
 */
public class OneSidePanel extends JPanel{

  private Controller controller;

  private JScrollPane scrlFileTable;
  private FileTable fileTable;

  private JPanel pnlActions;
  private JButton btnParent;
  private JButton btnOpen;
  private JButton btnDelete;
  private JButton btnRename;
  private JButton btnCreateDirectory;

  private FSImageChooser fsImageChooser;

  public FileTable getFileTable() {
    return fileTable;
  }

  public OneSidePanel(FileTable table, Controller controller) {
    this.controller = controller;
    fileTable = table;

    init();

    addListeners();

  }

  private void init() {
    setLayout(new BorderLayout(3, 3));

    scrlFileTable = new JScrollPane(fileTable);
    add(scrlFileTable, BorderLayout.CENTER);

    pnlActions = new JPanel(new FlowLayout());
    add(pnlActions, BorderLayout.NORTH);

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

    fsImageChooser = new FSImageChooser(controller);
    pnlActions.add(fsImageChooser);
  }

  private void addListeners() {

    btnParent.addActionListener(new OpenParentAction(fileTable));

    btnOpen.addActionListener(new OpenDirectoryAction(fileTable, controller));

    btnDelete.addActionListener(new DeleteAction(fileTable, controller));

    btnRename.addActionListener(new RenameAction(fileTable, controller));

    btnCreateDirectory.addActionListener(new CreateDirectoryAction(fileTable, controller));

    fsImageChooser.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FSImageChooser chooser = (FSImageChooser)e.getSource();
        FSImage fsImage = (FSImage)chooser.getSelectedItem();
        PseudoFile dir = new PseudoFile(fsImage, new PseudoPath());
        fileTable.load(dir);
      }
    });

    controller.addListener(fileTable);
    controller.addListener(fsImageChooser);
  }


}



