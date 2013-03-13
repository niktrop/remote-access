package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.commands.QueryReloadDirectory;
import ru.niktrop.remote_access.commands.SerializableCommand;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 12.03.13
 * Time: 15:36
 */
public class OneSidePanel extends JPanel{

  private Controller controller;

  private JScrollPane scrlFileTable;
  private FileTable tblFiles;

  private JToolBar tlbNavigation;
  private JButton btnParent;
  private JButton btnOpen;
  private JButton btnUpdate;

  public OneSidePanel(FileTable table, Controller controller) {
    this.controller = controller;
    tblFiles = table;

    init();

    addListeners();

  }

  private void addListeners() {

    btnParent.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        PseudoFile directory = tblFiles.getDirectory();

        PseudoFile parent = directory.getParent();
        if (parent == null) {
          return;
        }
        tblFiles.load(parent);
      }
    });

    btnOpen.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] selectedRows = tblFiles.getSelectedRows();
        if (selectedRows.length == 1) {
          FileTableModel model = (FileTableModel) tblFiles.getModel();
          final PseudoFile childDir = model.getPseudoFile(selectedRows[0]);
          if (childDir.isDirectory()) {
            final boolean needWait = (childDir.getDepth() == 0);
            if (!needWait) {
              tblFiles.load(childDir);
            }

            SwingWorker worker = new SwingWorker<Void, Void>() {
              @Override
              public Void doInBackground() {
                SerializableCommand command = new QueryReloadDirectory(childDir);
                FSImage fsi = controller.getFSImage(childDir.getFsiUuid());
                if (fsi.isLocal())
                {
                  //execute locally
                  controller.executeCommand(command);
                } else {
                  //send to server
                  controller.sendCommand(command);
                }
                return null;
              }

              @Override
              protected void done() {
                if (needWait) {
                  tblFiles.load(childDir);
                }
              }
            };

            worker.execute();
          }
        }
      }
    });

    btnUpdate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tblFiles.update();
      }
    });

    tblFiles.getModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        tblFiles.update();
      }
    });
  }

  private void init() {
    setLayout(new BorderLayout(3,3));

    scrlFileTable = new JScrollPane(tblFiles);
    add(scrlFileTable, BorderLayout.CENTER);

    tlbNavigation = new JToolBar();
    add(tlbNavigation, BorderLayout.NORTH);

    btnParent = new JButton("Parent");
    tlbNavigation.add(btnParent);

    btnOpen = new JButton("Open");
    tlbNavigation.add(btnOpen);

    btnUpdate = new JButton("Update");
    tlbNavigation.add(btnUpdate);
  }


}


