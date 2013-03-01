import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 25.02.13
 * Time: 11:53
 */
public class PseudoPathTest {

  @Test
  public void pseudoPathFromList() {
    List<String> names = new ArrayList<>();
    names.add("a");
    names.add("b");
    PseudoPath pseudoPath = new PseudoPath(names);

    assertThat(pseudoPath.getName(0).equals("a"));
    assertThat(pseudoPath.getName(1).equals("b"));
    assertThat(pseudoPath.getNameCount() == 2);
  }

  @Test
  public void pseudoPathFromEmptyList() {
    PseudoPath emptyPath = new PseudoPath(new ArrayList<String>());
    assertThat(emptyPath.getNameCount() == 0);
  }

  @Test
  public void pseudoPathVarargs() {
    PseudoPath pseudoPath = new PseudoPath("a", "b");

    assertThat(pseudoPath.getName(0).equals("a"));
    assertThat(pseudoPath.getName(1).equals("b"));
  }

  @Test
  public void pseudoPathNoArgs() {
    PseudoPath emptyPath = new PseudoPath();
    assertThat(emptyPath.getNameCount() == 0);
  }

  @Test
  public void pseudoPathToPath() {
    PseudoPath pseudoPath = new PseudoPath("a", "b");
    Path path = pseudoPath.toPath();

    assertThat(path.getName(0).toString().equals("a"));
    assertThat(path.getName(1).toString().equals("b"));
  }

  @Test
  public void pathToPseudoPath() {
    Path path = Paths.get("a", "b");
    PseudoPath pseudoPath = new PseudoPath(path);

    assertThat(pseudoPath.getName(0).equals("a"));
    assertThat(pseudoPath.getName(1).equals("b"));
    assertThat(pseudoPath.getNameCount() == 2);
  }

  @Test
  public void testResolve() {
    PseudoPath pseudoPath = new PseudoPath("a", "b");
    PseudoPath newPath = pseudoPath.resolve("c");

    assertThat(newPath.getNameCount() == 3);
    assertThat(newPath.getName(2).equals("c"));

    //empty pseudopath
    pseudoPath = new PseudoPath();
    newPath = pseudoPath.resolve("c");
    assertThat(newPath.getNameCount() == 1);
    assertThat(newPath.getName(0).equals("c"));
  }

  @Test
  public void testGetParent() throws Exception {
    PseudoPath a_b = new PseudoPath("a", "b");
    PseudoPath a = new PseudoPath("a");
    PseudoPath empty = new PseudoPath();

    assertThat(a_b.getParent()).isEqualTo(a);
    assertThat(a.getParent()).isEqualTo(empty);

    try {
      empty.getParent();
      fail("IllegalArgumentException expected on empty path");
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Empty pseudopath has no parent.");
    }

  }
}
