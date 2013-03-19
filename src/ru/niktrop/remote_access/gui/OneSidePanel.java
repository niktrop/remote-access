package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.CommandManager;
import ru.niktrop.remote_access.commands.ReloadDirectory;
import ru.niktrop.remote_access.commands.SerializableCommand;
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

  private JToolBar tlbNavigation;
  private JButton btnParent;
  private JButton btnOpen;
  private JButton btnDelete;
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

    tlbNavigation = new JToolBar();
    add(tlbNavigation, BorderLayout.NORTH);

    btnParent = new JButton("Parent");
    tlbNavigation.add(btnParent);

    btnOpen = new JButton("Open");
    tlbNavigation.add(btnOpen);

    btnDelete = new JButton("Delete");
    tlbNavigation.add(btnDelete);

    fsImageChooser = new FSImageChooser(controller);
    tlbNavigation.add(fsImageChooser);
  }

  private void addListeners() {

    btnParent.addActionListener(new OpenParentAction());

    btnOpen.addActionListener(new OpenDirectoryAction());

    btnDelete.addActionListener(new DeleteAction());

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



  private class OpenDirectoryAction extends AbstractAction{
    @Override
    public void actionPerformed(ActionEvent e) {
      int[] selectedRows = fileTable.getSelectedRows();
      if (selectedRows.length == 1) {
        FileTableModel model = (FileTableModel) fileTable.getModel();
        final PseudoFile childDir = model.getPseudoFile(selectedRows[0]);
        if (childDir.isDirectory()) {
          final boolean needWait = (childDir.getDepth() == 0);
          if (!needWait) {
            fileTable.load(childDir);
          }

          SwingWorker worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
              SerializableCommand command = new ReloadDirectory(childDir);
              FSImage fsi = controller.fsImages.get(childDir.getFsiUuid());
              CommandManager cm = CommandManager.instance(controller);
              if (fsi.isLocal())
              {
                //execute locally
                cm.executeCommand(command);
              } else {
                //send to server
                cm.sendCommand(command, controller.getChannel());
              }
              return null;
            }

            @Override
            protected void done() {
              if (needWait) {
                fileTable.load(childDir);
              }
            }
          };

          worker.execute();
        }
      }
    }
  }

  private class OpenParentAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      PseudoFile directory = fileTable.getDirectory();

      PseudoFile parent = directory.getParent();
      if (parent == null) {
        return;
      }
      fileTable.load(parent);
    }
  }

  private class DeleteAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      fileTable.update();
    }
  }

}


