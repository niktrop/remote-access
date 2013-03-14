package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.ControllerListener;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 16:52
 */
public class FileTable extends JTable implements ControllerListener {

  private PseudoFile directory;

  public FileTable(Controller controller) {
    this(controller.getDefaultDirectory());
  }

  public FileTable(PseudoFile directory) {
    TableModel fileTableModel = new FileTableModel(directory);
    this.directory = directory;
    setModel(fileTableModel);
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setAutoCreateRowSorter(true);
    setShowGrid(false);
  }

  public PseudoFile getDirectory() {
    return directory;
  }

  public void load(PseudoFile directory) {
    FileTableModel fileTableModel = new FileTableModel(directory);
    this.directory = directory;
    this.setModel(fileTableModel);
  }

  public void update() {
    FileTableModel fileTableModel = new FileTableModel(directory);
    this.setModel(fileTableModel);
  }

  @Override
  public void controllerChanged() {
    AbstractTableModel tm = (AbstractTableModel)getModel();
    tm.fireTableDataChanged();
  }
}
