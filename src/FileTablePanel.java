import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 9:33
 */
public enum FileTablePanel {
  ON_CLIENT,
  ON_SERVER;
  JPanel filesPanel;
  private JScrollPane tableScroll;
  private JTable filesTable;
  private PseudoFile directory;

  private FileTablePanel() {
    filesPanel = new JPanel();
    filesTable = new JTable();
    filesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    filesTable.setAutoCreateRowSorter(true);
    filesTable.setShowGrid(false);
    tableScroll = new JScrollPane(filesTable);
    filesPanel.add(tableScroll);
  }

  void populateTable(PseudoFile directory) {
    this.directory = directory;
    FileTableModel fileTableModel = new FileTableModel(directory);
    System.out.println(fileTableModel.getColumnName(0));
    System.out.println(fileTableModel.getColumnName(1));
    filesTable.setModel(fileTableModel);
  }
}
