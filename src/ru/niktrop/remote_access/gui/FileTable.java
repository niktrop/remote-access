package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.ControllerListener;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:52
 */
public class FileTable extends JTable implements ControllerListener {

  private final Controller controller;

  public FileTable(Controller controller) {
    this.controller = controller;
    setUpFileTable(controller);
  }

  public FileTable(Controller controller, PseudoFile directory) {
    this.controller = controller;
    setUpFileTable(controller);
    FileTableModel model = (FileTableModel) getModel();
    model.setDirectory(directory);
  }

  private void setUpFileTable(Controller controller) {
    PseudoFile defaultDirectory = controller.getDefaultDirectory();
    FileTableModel fileTableModel = new FileTableModel(defaultDirectory);

    setModel(fileTableModel);
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setAutoCreateRowSorter(true);
    getRowSorter().toggleSortOrder(0);

    setShowGrid(false);
  }

  public PseudoFile getDirectory() {
    FileTableModel model = (FileTableModel) getModel();
    return model.getDirectory();
  }

  public void load(PseudoFile directory) {
    FileTableModel model = (FileTableModel) getModel();
    model.setDirectory(directory);
    model.fireTableDataChanged();
  }

  public void update() {
    FileTableModel model = (FileTableModel) getModel();
    model.fireTableDataChanged();
  }

  @Override
  public void controllerChanged() {
    FileTableModel tm = (FileTableModel)getModel();
    PseudoFile dir = tm.getDirectory();

    if (dir != null && dir.exists()) {
      tm.fireTableDataChanged();
      return;
    }

    //if directory was deleted, show nearest ancestor
    if (dir != null && !dir.exists()) {
      while(!dir.exists()) {
        dir = dir.getParent();
      }
    }

    if (dir == null) {
      dir = controller.getDefaultDirectory();
    }

    tm.setDirectory(dir);
    tm.fireTableDataChanged();
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
