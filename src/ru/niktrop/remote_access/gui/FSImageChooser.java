package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.ControllerListener;
import ru.niktrop.remote_access.file_system_model.FSImage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 13.03.13
 * Time: 21:37
 */
public class FSImageChooser extends JComboBox<FSImage> implements ControllerListener{

  final String SEPARATOR = "SEPARATOR";
  final Controller controller;

  public FSImageChooser(Controller controller) {
    this.controller = controller;
    setModel(new FSImageChooserModel(controller));
    setRenderer(new FSImageChooserRenderer());
  }

  @Override
  public void controllerChanged() {
    //if no FSImages yet
    if (getModel().getSize() == 1) {
      this.setModel(new FSImageChooserModel(controller));
    }
  }


  class FSImageChooserModel extends AbstractListModel<FSImage> implements ComboBoxModel<FSImage> {

    private final List<FSImage> local;
    private final List<FSImage> remote;
    private FSImage selection = null;

    FSImageChooserModel(Controller controller, FSImage selection) {
      this(controller);
      setSelectedItem(selection);
    }

    FSImageChooserModel(Controller controller) {
      local = controller.fsImages.getLocal();
      remote = controller.fsImages.getRemote();
      List<FSImage> fsImages = new ArrayList<>();
      fsImages.addAll(local);
      fsImages.addAll(remote);

      if (!fsImages.isEmpty())
        setSelectedItem(fsImages.get(0));
    }

    @Override
    public void setSelectedItem(Object anItem) {
      selection = (FSImage) anItem;
    }

    @Override
    public Object getSelectedItem() {
      return selection;
    }

    @Override
    public int getSize() {
      return local.size() + remote.size() + 1;
    }

    @Override
    public FSImage getElementAt(int index) {
      List<FSImage> allFSImages = new ArrayList<>(local);
      allFSImages.add(null);
      allFSImages.addAll(remote);
      return allFSImages.get(index);
    }

  }

  class FSImageChooserRenderer extends JLabel implements ListCellRenderer {
    JSeparator separator;

    public FSImageChooserRenderer() {
      setOpaque(true);
      setBorder(new EmptyBorder(1, 1, 1, 1));
      separator = new JSeparator(JSeparator.HORIZONTAL);
    }

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      FSImage fsImage = (FSImage) value;
      String str = (fsImage == null) ? "SEPARATOR" : fsImage.getRootAlias();
      if (SEPARATOR.equals(str)) {
        return separator;
      }
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      setFont(list.getFont());
      setText(str);
      return this;
    }
  }
}
