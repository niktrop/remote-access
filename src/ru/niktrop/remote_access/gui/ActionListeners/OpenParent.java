package ru.niktrop.remote_access.gui.ActionListeners;

import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.gui.FileTable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:32
*/
public class OpenParent implements ActionListener {
  private final FileTable fileTable;
  private final Controller controller;

  public OpenParent(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
  }

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
