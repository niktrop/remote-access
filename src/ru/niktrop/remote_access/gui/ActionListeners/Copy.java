package ru.niktrop.remote_access.gui.ActionListeners;

import ru.niktrop.remote_access.commands.CopyDirectory;
import ru.niktrop.remote_access.commands.CopyFile;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.gui.FileTable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:34
*/


public class Copy implements ActionListener {
  private FileTable sourceFileTable;
  private FileTable targetFileTable;
  private Controller controller;

  public Copy(FileTable sourceFileTable, FileTable targetFileTable, Controller controller) {
    this.sourceFileTable = sourceFileTable;
    this.targetFileTable = targetFileTable;
    this.controller = controller;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final int[] selectedRows = sourceFileTable.getSortedSelectedRows();
    final PseudoFile targetDir = targetFileTable.getDirectory();
    final CommandManager cm = controller.getCommandManager();
    final FileTable.FileTableModel sourceModel = (FileTable.FileTableModel) sourceFileTable.getModel();

    SwingWorker worker = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {

        for (int i : selectedRows) {
          PseudoFile pseudoFile = sourceModel.getPseudoFile(i);
          SerializableCommand copy;
          if (pseudoFile.isDirectory()) {
            copy = new CopyDirectory(pseudoFile, targetDir);
          } else {
            copy = new CopyFile(pseudoFile, targetDir);
          }
          cm.executeCommand(copy);
        }

        return null;
      }
    };
    worker.execute();
  }
}
