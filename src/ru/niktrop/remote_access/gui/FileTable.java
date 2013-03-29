package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:52
 */
public class FileTable extends JTable {

  private final Controller controller;

  public FileTable(Controller controller, PseudoFile directory) {
    this.controller = controller;
    setUpFileTable(directory);
    FileTableModel model = (FileTableModel) getModel();
    model.setDirectory(directory);
  }

  private void setUpFileTable(PseudoFile directory) {
    FileTableModel fileTableModel = new FileTableModel(controller, directory);
    setModel(fileTableModel);
    controller.addListener(fileTableModel);

    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setAutoCreateRowSorter(true);
    getRowSorter().toggleSortOrder(0);
    getColumnModel().getColumn(0).setMaxWidth(80);

    setShowGrid(false);
  }

  public PseudoFile getDirectory() {
    FileTableModel model = (FileTableModel) getModel();
    return model.getDirectory();
  }

  public void load(PseudoFile directory) {
    FileTableModel model = (FileTableModel) getModel();
    model.setDirectory(directory);
    controller.fireControllerChange();
  }


  /**
   * Returns selected rows indexes in terms of view.
   * */
  public int[] getSortedSelectedRows() {
    int[] selectedRows = super.getSelectedRows();
    for(int i = 0; i < selectedRows.length; i++) {
      int row = selectedRows[i];
      selectedRows[i] = convertRowIndexToModel(row);
    }
    return selectedRows;
  }
}
