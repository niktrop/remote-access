import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 12:39
 */
public class FSChange {
  private final ChangeType type;
  private final String fsAlias;
  private final PseudoPath path;

  public FSChange(ChangeType type, String fsAlias, PseudoPath path) {
    this.type = type;
    this.fsAlias = fsAlias;
    this.path = path;
  }

  public ChangeType getType() {
    return type;
  }

  public String getFsAlias() {
    return fsAlias;
  }

  public PseudoPath getPath() {
    return path;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    char groupSeparator = '\u001E';
    char unitSeparator = '\u001F';
    builder.append(type.name());
    builder.append(groupSeparator);
    builder.append(fsAlias);
    builder.append(groupSeparator);
    for (int i = 0; i < path.getNameCount(); i++) {
      builder.append(path.getName(i));
      builder.append(unitSeparator);
    }
    return builder.toString();
  }

  public static FSChange fromString(String changeAsString) {
    String groupSeparator = "\u001E";
    String unitSeparator = "\u001F";
    StringTokenizer st = new StringTokenizer(changeAsString, groupSeparator, false);
    ChangeType type = ChangeType.valueOf(st.nextToken());
    String fsAlias = st.nextToken();
    String pathAsString = st.nextToken();
    st = new StringTokenizer(pathAsString, unitSeparator, false);
    PseudoPath path = new PseudoPath();
    while (st.hasMoreTokens()) {
      path = path.resolve(st.nextToken());
    }
    return new FSChange(type, fsAlias, path);
  }

}
