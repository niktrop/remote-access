package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.Notification;
import ru.niktrop.remote_access.commands.ReloadDirectory;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:31
*/
class OpenAction implements ActionListener {

  private final FileTable fileTable;
  private final PseudoFile selected;
  private final Controller controller;

  OpenAction(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
    selected = getSelected();
  }

  OpenAction(PseudoFile selected, FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
    this.selected = selected;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (selected == null)
      return;

    if (selected.isDirectory()) {
      openDirectory();
    } else {
      openFile();
    }
  }

  private void openDirectory() {
    int depth = selected.getDepth();
    //if depth == 0, subfiles was not loaded yet
    final boolean needWait = (depth == 0);
    if (!needWait) {
      fileTable.load(selected);
    }

    //if depth == maxDepth, no need to reload
    if (depth == controller.getMaxDepth()) {
      return;
    }

    //reload selected in worker thread if  depth < maxDepth
    SwingWorker worker = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {
        SerializableCommand command = new ReloadDirectory(selected);
        FSImage fsi = controller.fsImages.get(selected.getFsiUuid());
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
          fileTable.load(selected);
        }
      }
    };

    worker.execute();
  }

  private void openFile() {
    if (selected.getFsImage().isLocal()) {
      //open file with default application in background
      SwingWorker worker = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
          Desktop dt = Desktop.getDesktop();
          FSImage fsImage = selected.getFsImage();
          Path fullPath = fsImage.getPathToRoot().resolve(selected.getPseudoPath().toPath());
          try {
            dt.open(fullPath.toFile());
          } catch (IOException e1) {
            String message = String.format("Could not open file: %s", e1.getMessage());
            Notification warning = Notification.warning(message);
            controller.getCommandManager().executeCommand(warning);
          }
          return null;
        }
      };
      worker.execute();
    } else {
      String message = "You must download file first.";
      Notification warning = Notification.warning(message);
      controller.getCommandManager().executeCommand(warning);
    }
  }

  private PseudoFile getSelected() {
    int[] selectedRows = fileTable.getSortedSelectedRows();

    if (selectedRows.length != 1)
      return null;

    FileTableModel model = (FileTableModel) fileTable.getModel();
    final PseudoFile selected = model.getPseudoFile(selectedRows[0]);

    return selected;
  }
}
