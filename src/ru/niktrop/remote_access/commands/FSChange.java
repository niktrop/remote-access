package ru.niktrop.remote_access.commands;

import nu.xom.ParsingException;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
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


  @Override
  public List<SerializableCommand> execute(Controller controller) {
    String fsiUuid = getFsiUuid();
    FSImage fsi = controller.fsImages.get(fsiUuid);
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
    return Collections.emptyList();
  }

  /**
   * Parses object of type FSChange from String, which results in toString()
   * method (without class name prefix).
   */
  @Override
  public FSChange fromString(String changeAsString) {
    String groupSeparator = "\u001E";
    String nul = "\u0000";

    StringTokenizer st = new StringTokenizer(changeAsString, groupSeparator, false);
    ChangeType type = ChangeType.valueOf(st.nextToken());
    String fsiUuid = st.nextToken();
    String pathAsString = st.nextToken();
    String xmlFSImage = st.hasMoreTokens() ? st.nextToken() : null;

    PseudoPath path = new PseudoPath();
    if ( !pathAsString.equals(nul)) {
      path = PseudoPath.deserialize(pathAsString);
    }
    return new FSChange(type, fsiUuid, path, xmlFSImage);
  }

  /**
   * Builds string representation of the FSChange object.
   */
  @Override
  public String getStringRepresentation() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char nul = '\u0000';

    builder.append(changeType.name());
    builder.append(groupSeparator);

    builder.append(fsiUuid);
    builder.append(groupSeparator);

    if (path.getNameCount() == 0) {
      builder.append(nul); //represents empty pseudopath
    }

    builder.append(path.serializeToString());

    if (xmlFSImage != null) {
      builder.append(groupSeparator);
      builder.append(xmlFSImage);
    }
    return builder.toString();
  }


}
