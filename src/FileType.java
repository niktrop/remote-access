/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 16:28
 */
public enum FileType {
  ROOT("root"),
  DIR("directory"),
  FILE("file"),
  SYMLINK("symlink"),
  OTHER("other");

  private FileType(String name) {
    this.name = name;
  }

  private String name;

  public String getName() {
    return this.name;
  }
}
