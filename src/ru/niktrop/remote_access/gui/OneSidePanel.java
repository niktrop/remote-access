package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.*;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

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

    btnParent.addActionListener(new OpenParentAction());

    btnOpen.addActionListener(new OpenDirectoryAction());

    btnDelete.addActionListener(new DeleteAction());

    btnRename.addActionListener(new RenameAction());

    btnCreateDirectory.addActionListener(new CreateDirectoryAction());

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
      int[] selectedRows = fileTable.getSortedSelectedRows();
      if (selectedRows.length != 1)
        return;

      FileTableModel model = (FileTableModel) fileTable.getModel();
      final PseudoFile childDir = model.getPseudoFile(selectedRows[0]);

      if ( !childDir.isDirectory())
        return;

      final boolean needWait = (childDir.getDepth() == 0);
      if (!needWait) {
        fileTable.load(childDir);
      }

      SwingWorker worker = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
          SerializableCommand command = new ReloadDirectory(childDir);
          FSImage fsi = controller.fsImages.get(childDir.getFsiUuid());
          CommandManager cm = controller.getCommandManager();
          if (fsi.isLocal())
          {
            //execute locally
            cm.executeCommand(command);
          } else {
            //send to server
            cm.sendCommand(command, cm.getChannel());
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
      final int[] selectedRows = fileTable.getSortedSelectedRows();
      final CommandManager cm = controller.getCommandManager();
      final FileTableModel model = (FileTableModel) fileTable.getModel();

      SwingWorker worker = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {

          List<PseudoFile> toDelete = new LinkedList<>();
          for (int i : selectedRows) {
            toDelete.add(model.getPseudoFile(i));
          }

          for (PseudoFile file : toDelete) {
            SerializableCommand delete = new DeleteFile(file);
            cm.executeCommand(delete);
          }

          return null;
        }
      };

      worker.execute();
    }
  }

  private class RenameAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
      int[] selectedRows = fileTable.getSortedSelectedRows();
      if (selectedRows.length != 1)
        return;

      FileTableModel model = (FileTableModel) fileTable.getModel();
      final PseudoFile pseudoFile = model.getPseudoFile(selectedRows[0]);

      JFrame frame = controller.getNotificationManager().getParentFrame();
      String oldName = pseudoFile.getName();
      String result = "";
      while ("".equals(result)) {
        result = JOptionPane.showInputDialog(frame, "Enter new name:", oldName);
      }

      if (result == null || result.equals(oldName))
        return;

      final String newName = result;

      SwingWorker worker = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
          SerializableCommand rename = new RenameFile(pseudoFile, newName);
          CommandManager cm = controller.getCommandManager();
          cm.executeCommand(rename);
          return null;
        }
      };
      worker.execute();
    }

  }

  private class CreateDirectoryAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {


      JFrame frame = controller.getNotificationManager().getParentFrame();
      String name = "NewDirectory";
      name = JOptionPane.showInputDialog(frame, "Enter new name:", name);
      while ("".equals(name)) {
        name = JOptionPane.showInputDialog(frame, "Enter new name:", name);
      }

      if (name == null) {
        return;
      }

      FileTableModel model = (FileTableModel) fileTable.getModel();
      PseudoFile directory = model.getDirectory();
      final String fsiUuid = directory.getFsiUuid();
      final boolean isDirectory = true;
      final PseudoPath newDirectory = directory.getPseudoPath().resolve(name);

      SwingWorker worker = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
          SerializableCommand create = new CreateFile(fsiUuid, newDirectory, isDirectory);
          CommandManager cm = controller.getCommandManager();
          cm.executeCommand(create);
          return null;
        }
      };
      worker.execute();
    }

  }
}



