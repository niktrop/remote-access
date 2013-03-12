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
  JPanel pnlFiles;
  private JScrollPane scrlFiles;
  private JTable tblFiles;
  private PseudoFile directory;

  private FileTablePanel() {
    pnlFiles = new JPanel();
    tblFiles = new JTable();
    tblFiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    tblFiles.setAutoCreateRowSorter(true);
    tblFiles.setShowGrid(false);
    scrlFiles = new JScrollPane(tblFiles);
    pnlFiles.add(scrlFiles);
  }

  void populateTable(PseudoFile directory) {
    this.directory = directory;
    FileTableModel fileTableModel = new FileTableModel(directory);
    tblFiles.setModel(fileTableModel);
  }
}
