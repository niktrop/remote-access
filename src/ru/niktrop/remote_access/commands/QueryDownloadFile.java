package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.controller.Controller;
import ru.niktrop.remote_access.controller.FileTransferManager;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 21.03.13
 * Time: 11:39
 */

/**
 * One of the command from a copy operation. Should be sent
 * from the target side to the source side after execution of AllocateSpace command.
 * */
public class QueryDownloadFile implements SerializableCommand {

  String operationUuid;

  //Only for deserialization.
  QueryDownloadFile() {
    this(null);
  }

  public QueryDownloadFile(String operationUuid) {
    this.operationUuid = operationUuid;
  }

  @Override
  public void execute(Controller controller) {
    FileTransferManager ftm = controller.getFileTransferManager();
    ftm.sendFile(operationUuid);
  }

  @Override
  public String getStringRepresentation() {
    return operationUuid;
  }

  @Override
  public SerializableCommand fromString(String representation) {
    return new QueryDownloadFile(representation);
  }
}
