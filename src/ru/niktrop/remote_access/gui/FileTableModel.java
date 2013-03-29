package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.ControllerListener;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 15:37
 */
public class FileTableModel extends AbstractTableModel implements ControllerListener {
  private Controller controller;
  private PseudoFile directory;
  private List<String> columns = new ArrayList<>();

  {
    columns.add("Type");
    columns.add("Name");
  }

  public FileTableModel(Controller controller, PseudoFile directory) {
    this.directory = directory;
    this.controller = controller;
  }

  public PseudoFile getPseudoFile(int index) {
    return directory.getContent().get(index);
  }

  public void setDirectory(PseudoFile directory) {
    this.directory = directory;
  }

  public PseudoFile getDirectory() {
    return directory;
  }

  @Override
  public int getRowCount() {
    if (directory == null)
      return 0;
    return directory.getContent().size();
  }

  @Override
  public int getColumnCount() {
    return columns.size();
  }

  @Override
  public String getColumnName(int index) {
    return columns.get(index);
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (directory == null) return null;

    PseudoFile pseudoFile = directory.getContent().get(rowIndex);
//    Path path = pseudoFile.getPseudoPath().toPath();
//    FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    if (columnIndex == 0) return pseudoFile.getType();
    if (columnIndex == 1) return pseudoFile.getName();
    return null;
  }

  @Override
  public void controllerChanged() {

    //if directory was deleted, show nearest ancestor
    if (directory != null && !directory.exists()) {
      while(!directory.exists()) {
        directory = directory.getParent();
      }
    }

    //if directory became null, show default directory
    if (directory == null) {
      directory = controller.fsImages.defaultDirectory();
    }

    setDirectory(directory);
    fireTableDataChanged();
  }
}
