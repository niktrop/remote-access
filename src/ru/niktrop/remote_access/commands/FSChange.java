package ru.niktrop.remote_access.commands;

import nu.xom.ParsingException;
import org.jboss.netty.channel.ChannelHandlerContext;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 12:39
 */
public class FSChange implements SerializableCommand {
  private static final Logger LOG = Logger.getLogger(FSChange.class.getName());

  private final ChangeType changeType;
  private final String fsiUuid;
  private final PseudoPath path;

  //only for changeType CREATE_DIR and NEW_IMAGE
  private final String xmlFSImage;

  //Only for deserialization purposes.
  FSChange() {
    changeType = null;
    fsiUuid = null;
    path = null;
    xmlFSImage = null;
  }

  public static FSChange getEmptyInstance() {
    return new FSChange();
  }

  public FSChange(ChangeType changeType, String fsiUuid, PseudoPath path) {
    this.changeType = changeType;
    this.fsiUuid = fsiUuid;
    this.path = path;
    this.xmlFSImage = null;
  }

  public FSChange(ChangeType changeType, String fsiUuid, PseudoPath path, String xmlFSImage) {
    this.changeType = changeType;
    this.fsiUuid = fsiUuid;
    this.path = path;
    this.xmlFSImage = xmlFSImage;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getFsiUuid() {
    return fsiUuid;
  }

  public PseudoPath getPath() {
    return path;
  }

  public String getXmlFSImage() {
    return xmlFSImage;
  }

  /**
   * Builds string representation of the ru.niktrop.remote_access.commands.FSChange object.
   */
  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';

    builder.append(changeType.name());
    builder.append(groupSeparator);

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    for (int i = 0; i < path.getNameCount(); i++) {
      builder.append(path.getName(i));
      builder.append(unitSeparator);
    }

    if (xmlFSImage != null) {
      builder.append(groupSeparator);
      builder.append(xmlFSImage);
    }
    return builder.toString();
  }

  @Override
  public void execute(Controller controller, ChannelHandlerContext ctx) {
    String fsiUuid = getFsiUuid();
    FSImage fsi = controller.getFSImage(fsiUuid);
    PseudoPath pseudoPath = getPath();
    switch (getChangeType()) {
      case CREATE_DIR:
        FSImage createdDirFsi = null;
        try {
          createdDirFsi = FSImages.getFromXml(getXmlFSImage());
        } catch (ParsingException e) {
          LOG.log(Level.WARNING, null, e.getCause());
        } catch (IOException e) {
          LOG.log(Level.WARNING, null, e.getCause());
        }
        fsi.addToDirectory(pseudoPath.getParent(), createdDirFsi);
        break;
      case CREATE_FILE:
        fsi.addFile(pseudoPath);
        break;
      case DELETE:
        fsi.deletePath(pseudoPath);
        break;
      case NEW_IMAGE:
        FSImage newFsi = null;
        try {
          newFsi = FSImages.getFromXml(getXmlFSImage());
          controller.addFSImage(newFsi);
        } catch (ParsingException e) {
          LOG.log(Level.WARNING, null, e.getCause());
        } catch (IOException e) {
          LOG.log(Level.WARNING, null, e.getCause());
        }
        break;
      default:
        break;
    }
  }

  /**
   * Parses object of type ru.niktrop.remote_access.commands.FSChange from String, which results in toString()
   * method (without class name prefix).
   *
   */
  @Override
  public FSChange fromString(String changeAsString) {
    String groupSeparator = "\u001E";
    String unitSeparator = "\u001F";

    StringTokenizer st = new StringTokenizer(changeAsString, groupSeparator, false);
    ChangeType type = ChangeType.valueOf(st.nextToken());
    String fsiUuid = st.nextToken();
    String pathAsString = st.nextToken();
    String xmlFSImage = st.hasMoreTokens() ? st.nextToken() : null;

    st = new StringTokenizer(pathAsString, unitSeparator, false);
    PseudoPath path = new PseudoPath();
    while (st.hasMoreTokens()) {
      path = path.resolve(st.nextToken());
    }
    return new FSChange(type, fsiUuid, path, xmlFSImage);
  }

}
