package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.DeleteFile;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:32
*/
class DeleteAction implements ActionListener {
  private FileTable fileTable;
  private Controller controller;

  DeleteAction(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final int[] selectedRows = fileTable.getSortedSelectedRows();
    final CommandManager cm = controller.getCommandManager();
    final FileTableModel model = (FileTableModel) fileTable.getModel();

    SwingWorker worker = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {

        List<PseudoFile> toDelete = new ArrayList<>();
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
