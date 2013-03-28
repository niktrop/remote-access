package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.ControllerListener;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.03.13
 * Time: 14:11
 */
public class NavigationBar extends JPanel implements ControllerListener{

  private PseudoFile directory;
  private final FileTable fileTable;
  private final Controller controller;
  private final FSImageChooser fsImageChooser;

  public NavigationBar(FileTable fileTable, Controller controller) {
    this.fileTable = fileTable;
    this.controller = controller;
    setLayout(new WrapLayout(FlowLayout.LEFT));

    directory = fileTable.getDirectory();
    FSImage selected = getSelectedFSImage(controller);
    fsImageChooser = new FSImageChooser(controller, selected);

    arrangeElements();

    controller.addListener(this);
  }

  private void initFsImageChooser() {
    fsImageChooser.setAlignmentY(Component.TOP_ALIGNMENT);
    fsImageChooser.setMaximumSize(fsImageChooser.getPreferredSize());
    fsImageChooser.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FSImageChooser chooser = (FSImageChooser)e.getSource();
        FSImage fsImage = (FSImage)chooser.getSelectedItem();
        PseudoFile dir = new PseudoFile(fsImage, new PseudoPath());
        NavigationBar.this.fileTable.load(dir);
      }
    });
  }

  private FSImage getSelectedFSImage(Controller controller) {
    FSImage selected;
    if (directory != null) {
      selected = controller.fsImages.get(directory.getFsiUuid());
    } else {
      selected = null;
    }
    return selected;
  }

  private void arrangeElements() {

    removeAll();

    initFsImageChooser();
    add(fsImageChooser);

    Deque<PseudoFile> directories = new ArrayDeque<>();
    PseudoFile tempDir = directory;

    while (tempDir != null) {
      directories.push(tempDir);
      tempDir = tempDir.getParent();
    }

    while ( !directories.isEmpty()) {
      tempDir = directories.pop();
      String name = tempDir.getName();
      name = (name == null ? "root" : name);
      JButton button = new JButton(name);
      button.setMargin(new Insets(1,1,1,1));
      button.addActionListener(new OpenAction(tempDir, fileTable, controller));
      add(button);
    }
  }

  @Override
  public void controllerChanged() {
    PseudoFile newDirectory = fileTable.getDirectory();
    if (directory != newDirectory) {
      directory = newDirectory;
      arrangeElements();
      this.revalidate();
      this.repaint();
    }
  }
}
