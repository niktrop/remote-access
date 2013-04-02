package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.controller.ControllerListener;
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

/**
 * Component for choosing one of the FSImages from the controller.
 * One can determine whether the FSImage is local or not by the color.
 * */
public class FSImageChooser extends JComboBox<FSImage> implements ControllerListener{

  private final Controller controller;

  //background colors
  private final Color colorLocal = new Color(200, 255, 200);
  private final Color colorRemote = new Color(200, 200, 255);

  public FSImageChooser(Controller controller, FSImage selected) {
    this.controller = controller;
    setModel(new FSImageChooserModel(controller, selected));
    setBackground(selected.isLocal() ? colorLocal : colorRemote);
    setRenderer(new FSImageChooserRenderer());
    controller.addListener(this);
  }

  @Override
  public void controllerChanged() {
    //Number of FSImages + jSeparator
    int newSize = controller.fsImages.size() + 1;
    if (getModel().getSize() != newSize) {
      setModel(new FSImageChooserModel(controller));
      revalidate();
      repaint();
    }
  }

  @Override
  protected void selectedItemChanged() {
    super.selectedItemChanged();
    FSImage selected = (FSImage)getSelectedItem();
    setBackground(selected.isLocal() ? colorLocal : colorRemote);
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
    JSeparator jSeparator;

    public FSImageChooserRenderer() {
      setOpaque(true);
      setBorder(new EmptyBorder(1, 1, 1, 1));
      jSeparator = new JSeparator(JSeparator.HORIZONTAL);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      FSImage fsImage = (FSImage) value;
      String separator = "SEPARATOR";
      String str = (fsImage == null) ? separator : fsImage.getRootAlias();
      if (separator.equals(str)) {
        return jSeparator;
      }
      if (fsImage != null && fsImage.isLocal()) {
        setBackground(colorLocal);
      } else {
        setBackground(colorRemote);
      }
      setForeground(list.getSelectionForeground());
      setFont(list.getFont());
      setText(str);
      return this;
    }
  }
}
