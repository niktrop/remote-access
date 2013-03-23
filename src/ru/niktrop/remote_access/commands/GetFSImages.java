package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 11:17
 */
public class GetFSImages implements SerializableCommand {

  //Only for deserialization.
  public GetFSImages() {
  }

  /**
   * Represents query from client to server to sent all FSImages from server controller.
   * Should be sent when connected to the server first time.
   * */
  @Override
  public List<SerializableCommand> execute(Controller controller) {
    List<SerializableCommand> response = new LinkedList<>();
    for (FSImage fsi : controller.fsImages.getLocal()) {
      String uuid = fsi.getUuid();
      String xml = fsi.toXml();
      FSChange fsChange = new FSChange(ChangeType.NEW_IMAGE, uuid, new PseudoPath(), xml);
      response.add(fsChange);
    }
    return response;
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
