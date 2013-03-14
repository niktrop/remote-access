package ru.niktrop.remote_access;

import java.io.IOException;

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


}
