import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 12:39
 */
public class FSChange {
  private final ChangeType type;
  private final String fsiUuid;
  private final PseudoPath path;

  //only for type CREATE_DIR
  private final String xmlFSImage;

  public FSChange(ChangeType type, String fsiUuid, PseudoPath path) {
    this.type = type;
    this.fsiUuid = fsiUuid;
    this.path = path;
    this.xmlFSImage = null;
  }

  public FSChange(ChangeType type, String fsiUuid, PseudoPath path, String xmlFSImage) {
    this.type = type;
    this.fsiUuid = fsiUuid;
    this.path = path;
    this.xmlFSImage = xmlFSImage;
  }

  public ChangeType getType() {
    return type;
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

  public String serializeToString() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';
    builder.append(type.name());
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

  public static FSChange fromString(String changeAsString) {
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
