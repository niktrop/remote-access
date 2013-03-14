package ru.niktrop.remote_access.gui;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 13.03.13
 * Time: 21:37
 */
public class FSImageChooser extends JComboBox<FSImage> {

  final String SEPARATOR = "SEPARATOR";

  public FSImageChooser(Controller controller) {
    setModel(new FSImageChooserModel(controller));
    setRenderer(new FSImageChooserRenderer());
  }


  class FSImageChooserModel extends AbstractListModel<FSImage> implements ComboBoxModel<FSImage> {

    private final Controller controller;
    private final List<FSImage> local;
    private final List<FSImage> remote;
    private FSImage selection = null;

    FSImageChooserModel(Controller controller) {
      this.controller = controller;
      local = new ArrayList<>();
      remote = new ArrayList<>();
      Iterable<FSImage> fsImages = controller.getFSImages();
      for (FSImage fsImage : fsImages) {
        if (fsImage.isLocal()) {
          local.add(fsImage);
        } else {
          remote.add(fsImage);
        }
      }
      setSelectedItem(local.get(0));
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
      Collections.sort(local, byAlias);
      Collections.sort(remote, byAlias);

      List<FSImage> allFSImages = new ArrayList<>(local);
      allFSImages.add(null);
      allFSImages.addAll(remote);
      return allFSImages.get(index);
    }

    Comparator<FSImage> byAlias = new Comparator<FSImage>() {
      @Override
      public int compare(FSImage o1, FSImage o2) {
        String alias1 = o1.getRootAlias();
        String alias2 = o2.getRootAlias();
        return alias1.compareTo(alias2);
      }
    };
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
