package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 11:17
 */

/**
 * Represents query from client to server to sent all FSImages from server controller.
 * Should be sent when client is connecting to the server.
 * */
public class GetFSImages implements SerializableCommand {

  //Command is stateless, but default constructor is needed for deserialization also.
  public GetFSImages() {
  }


  @Override
  public void execute(Controller controller) {
    CommandManager cm = controller.getCommandManager();

    for (FSImage fsi : controller.fsImages.getLocal()) {
      String uuid = fsi.getUuid();
      String xml = fsi.toXml();
      FSChange fsChange = new FSChange(ChangeType.NEW_IMAGE, uuid, new PseudoPath(), xml);
      cm.sendCommand(fsChange);
    }
  }

  @Override
  public String getStringRepresentation() {
    return "";
  }

  @Override
  public SerializableCommand fromString(String string) {
    return new GetFSImages();
  }

}
