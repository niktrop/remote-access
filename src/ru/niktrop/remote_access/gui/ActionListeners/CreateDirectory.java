package ru.niktrop.remote_access.gui.ActionListeners;

import ru.niktrop.remote_access.commands.CreateFile;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.controller.CommandManager;
import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;
import ru.niktrop.remote_access.gui.FileTable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:29
*/
public class CreateDirectory implements ActionListener {

  private FileTable fileTable;
  private Controller controller;

  public CreateDirectory(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
  }

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

    FileTable.FileTableModel model = (FileTable.FileTableModel) fileTable.getModel();
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
