package ru.niktrop.remote_access;

import ru.niktrop.remote_access.file_system_model.FSImage;

import java.io.IOException;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 13.03.13
 * Time: 19:03
 */
public class Controllers {
  private static Controller serverController;
  private static Controller clientController;

  public static Controller getServerController() throws IOException {
    if (serverController == null) {
      return new Controller(Controller.ControllerType.SERVER);
    }
    return serverController;
  }

  public static Controller getClientController() throws IOException {
    if (clientController == null) {
      return new Controller(Controller.ControllerType.CLIENT);
    }
    return serverController;
  }

  public static Controller getController() throws IOException {
    return new Controller(null);
  }

  public static Comparator<FSImage> byAlias = new Comparator<FSImage>() {
    @Override
    public int compare(FSImage o1, FSImage o2) {
      String alias1 = o1.getRootAlias();
      String alias2 = o2.getRootAlias();
      if (alias1 == null)
        return -1;
      else if (alias2 == null)
        return 1;
      else
        return alias1.compareTo(alias2);
    }
  };
}
