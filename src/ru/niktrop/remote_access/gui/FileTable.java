package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.ControllerListener;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:52
 */
public class FileTable extends JTable implements ControllerListener {

  private final Controller controller;

  public FileTable(Controller controller, PseudoFile directory) {
    this.controller = controller;
    setUpFileTable(directory);
    FileTableModel model = (FileTableModel) getModel();
    model.setDirectory(directory);
  }

  private void setUpFileTable(PseudoFile directory) {
    FileTableModel fileTableModel = new FileTableModel(directory);

    setModel(fileTableModel);
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

    //if directory became null, show default directory
    if (dir == null) {
      List<FSImage> fsImages = new ArrayList<>(controller.fsImages.getLocal());
      fsImages.addAll(controller.fsImages.getRemote());
      dir = new PseudoFile(fsImages.get(0), new PseudoPath());
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
