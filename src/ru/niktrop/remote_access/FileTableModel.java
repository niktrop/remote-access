package ru.niktrop.remote_access;

import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 9:15
 */
public class FileTableModel extends AbstractTableModel {
  private PseudoFile directory;
  private List<String> columns = new ArrayList<>();
  {
    columns.add("Type");
    columns.add("Name");
  }

  public FileTableModel(PseudoFile directory) {
    this.directory = directory;
  }


  @Override
  public int getRowCount() {
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
    PseudoFile file = directory.getContent().get(rowIndex);
    //First two attributes are obligatory.
    if (columnIndex == 0) return file.getType();
    if (columnIndex == 1) return file.getName();
    return null;
  }
}
