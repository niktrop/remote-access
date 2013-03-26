package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
* Created with IntelliJ IDEA.
* User: Nikolai Tropin
* Date: 22.03.13
* Time: 23:32
*/
class OpenParentAction implements ActionListener {
  private final FileTable fileTable;
  private final Controller controller;

  OpenParentAction(FileTable fileTable, Controller controller) {
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
