package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.ReloadDirectory;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:31
*/
class OpenDirectoryAction implements ActionListener {

  private final FileTable fileTable;
  private final PseudoFile directory;
  private final int maxDepth;
  private final Controller controller;

  OpenDirectoryAction(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
    maxDepth = controller.getMaxDepth();
    directory = getSelectedDirectory();
  }

  OpenDirectoryAction(PseudoFile directory, FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
    this.directory = directory;
    maxDepth = controller.getMaxDepth();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (directory == null)
      return;

    int depth = directory.getDepth();
    //if depth == 0, subfiles was not loaded yet
    final boolean needWait = (depth == 0);
    if (!needWait) {
      fileTable.load(directory);
    }

    //if depth == maxDepth, no need to reload
    if (depth == controller.getMaxDepth()) {
      return;
    }

    //reload directory in worker thread if 0 <= depth < maxDepth
    SwingWorker worker = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {
        SerializableCommand command = new ReloadDirectory(directory);
        FSImage fsi = controller.fsImages.get(directory.getFsiUuid());
        CommandManager cm = controller.getCommandManager();
        if (fsi.isLocal())
        {
          //execute locally
          cm.executeCommand(command);
        } else {
          //send to server
          cm.sendCommand(command);
        }
        return null;
      }

      @Override
      protected void done() {
        if (needWait) {
          fileTable.load(directory);
        }
      }
    };

    worker.execute();
  }

  private PseudoFile getSelectedDirectory() {
    int[] selectedRows = fileTable.getSortedSelectedRows();

    if (selectedRows.length != 1)
      return null;

    FileTableModel model = (FileTableModel) fileTable.getModel();
    final PseudoFile selected = model.getPseudoFile(selectedRows[0]);

    if ( !selected.isDirectory())
      return null;

    return selected;
  }
}
