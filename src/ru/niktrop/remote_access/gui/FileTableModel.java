package ru.niktrop.remote_access.gui;

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
public class FileTableModel extends AbstractTableModel {
  private PseudoFile directory;
  private List<String> columns = new ArrayList<>();
  {
    columns.add("ControllerType");
    columns.add("Name");
  }

  public FileTableModel() {}

  public FileTableModel(PseudoFile directory) {
    this.directory = directory;
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

    PseudoFile file = directory.getContent().get(rowIndex);
    if (columnIndex == 0) return file.getType();
    if (columnIndex == 1) return file.getName();
    return null;
  }
}
