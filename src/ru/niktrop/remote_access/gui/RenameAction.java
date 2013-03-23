package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.RenameFile;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:27
*/
class RenameAction implements ActionListener {

  private FileTable fileTable;
  private Controller controller;

  RenameAction(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
  }

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
