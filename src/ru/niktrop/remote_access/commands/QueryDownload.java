package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 21.03.13
 * Time: 11:39
 */

/**
 * One of the command from a copy operation. Should be sent
 * from the target side to the source side as a result of AllocateSpace.
 * */
public class QueryDownload implements SerializableCommand {

  String operationUuid;

  public QueryDownload() {
    this(null);
  }

  public QueryDownload(String operationUuid) {
    this.operationUuid = operationUuid;
  }

  @Override
  public List<SerializableCommand> execute(Controller controller) {
    FileTransferManager ftm = controller.getFileTransferManager();
    ftm.sendFile(operationUuid);
    return Collections.emptyList();
  }

  @Override
  public String getStringRepresentation() {
    return operationUuid;
  }

  @Override
  public SerializableCommand fromString(String representation) {
    return new QueryDownload(representation);
  }
}
