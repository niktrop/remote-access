package ru.niktrop.remote_access.commands;

import ru.niktrop.remote_access.CommandManager;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.FileTransferManager;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 21.03.13
 * Time: 11:03
 */

/**
 * Creates empty file of specified size.
 * Executes on the target side of a copy operation.
 * */
public class AllocateSpace implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(AllocateSpace.class.getName());

  private final String fsiUuid;
  private final PseudoPath path;
  private final long size;
  private final String operationUuid;

  public AllocateSpace(String fsiUuid, PseudoPath path, long size, String operationUuid) {
    this.fsiUuid = fsiUuid;
    this.path = path;
    this.size = size;
    this.operationUuid = operationUuid;
  }

  AllocateSpace() {
    this(null, null, 0L, null);
  }

  @Override
  public List<SerializableCommand> execute(Controller controller) {
    FSImage fsi = controller.fsImages.get(fsiUuid);
    CommandManager cm = controller.getCommandManager();
    FileTransferManager ftm = controller.getFileTransferManager();

    if ( !fsi.isLocal()) {
      LOG.log(Level.WARNING, "No such FSImage on the target side");
      return Collections.emptyList();
    }

    Path pathToRoot = fsi.getPathToRoot();
    Path fullPath = pathToRoot.resolve(path.toPath());

    //save memo about this operation on the target side
    ftm.addTarget(operationUuid, fullPath);
    List<SerializableCommand> result = new ArrayList<>(1);
    try (RandomAccessFile f = new RandomAccessFile(fullPath.toFile(), "rw")) {
      f.setLength(size);

      //will send next command to the source
      result.add(new QueryDownload(operationUuid));
    }
    catch (IOException e) {
      String message = String.format("Allocation memory failed: \r\n %s", e.toString());
      LOG.log(Level.WARNING, message, e.getCause());

      Notification failed = Notification.operationFailed(message, operationUuid);
      cm.executeCommand(failed);

      ftm.removeTarget(operationUuid);
    }
    return result;
  }

  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    builder.append(size);
    builder.append(groupSeparator);

    builder.append(operationUuid);
    builder.append(groupSeparator);

    builder.append(path.serializeToString());

    return builder.toString();
  }

  @Override
  public SerializableCommand fromString(String representation) {
    String groupSeparator = "\u001E";

    StringTokenizer st = new StringTokenizer(representation, groupSeparator, false);
    String fsiUuid = st.nextToken();

    long size = Long.parseLong(st.nextToken());

    String sourceId = st.nextToken();

    String pathAsString = st.nextToken();
    PseudoPath path = PseudoPath.deserialize(pathAsString);

    return new AllocateSpace(fsiUuid, path, size, sourceId);
  }
}
