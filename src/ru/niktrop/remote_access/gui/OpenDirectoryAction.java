package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.ReloadDirectory;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:31
*/
class OpenDirectoryAction extends AbstractAction {

  private FileTable fileTable;
  private Controller controller;

  OpenDirectoryAction(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
  }

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
