package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 15:36
 */
public enum FileTablePanel {
  LEFT,
  RIGHT;
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

  public void populateTable(PseudoFile directory) {
    this.directory = directory;
    FileTableModel fileTableModel = new FileTableModel(directory);
    tblFiles.setModel(fileTableModel);
  }

  public JPanel getPanel() {
    return pnlFiles;
  }
}


