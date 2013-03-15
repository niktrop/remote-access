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

  private PseudoFile directory;
  private Controller controller;

  public FileTable(Controller controller) {
    PseudoFile defaultDirectory = controller.getDefaultDirectory();
    if (defaultDirectory != null) {
      this.directory = defaultDirectory;
    }
    setUpFileTable(controller);
  }

  public FileTable(Controller controller, PseudoFile directory) {
    this.directory = directory;
    setUpFileTable(controller);
  }
   //TODO Разобраться с инициализацией таблицы, если у контроллера пустой список образов.
  private void setUpFileTable(Controller controller) {
    this.controller = controller;
    FileTableModel fileTableModel = new FileTableModel();
    fileTableModel.setDirectory(directory);
    setModel(fileTableModel);
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setAutoCreateRowSorter(true);
    setShowGrid(false);
  }

  public PseudoFile getDirectory() {
    return directory;
  }

  public void load(PseudoFile directory) {
    this.directory = directory;
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
    PseudoFile dir = getDirectory();
    if (dir == null) {
      FileTableModel newModel = new FileTableModel(controller.getDefaultDirectory());
      setModel(newModel);
    } else {
      tm.fireTableDataChanged();
    }
  }
}
