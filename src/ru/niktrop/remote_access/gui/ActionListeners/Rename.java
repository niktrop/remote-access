package ru.niktrop.remote_access.gui.ActionListeners;

import ru.niktrop.remote_access.commands.RenameFile;
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
* Time: 23:27
*/
public class Rename implements ActionListener {

  private FileTable fileTable;
  private Controller controller;

  public Rename(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    int[] selectedRows = fileTable.getSortedSelectedRows();
    if (selectedRows.length != 1)
      return;

    FileTable.FileTableModel model = (FileTable.FileTableModel) fileTable.getModel();
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
