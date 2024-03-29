package ru.niktrop.remote_access.file_system_model.test;

import org.junit.Test;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

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

    assertThat(pseudoPath.getName(0)).isEqualTo("a");
    assertThat(pseudoPath.getName(1)).isEqualTo("b");
    assertThat(pseudoPath.getNameCount()).isEqualTo(2);
  }

  @Test
  public void pseudoPathFromEmptyList() {
    PseudoPath emptyPath = new PseudoPath(new ArrayList<String>());
    assertThat(emptyPath.getNameCount()).isEqualTo(0);
  }

  @Test
  public void pseudoPathVarargs() {
    PseudoPath pseudoPath = new PseudoPath("a", "b");

    assertThat(pseudoPath.getName(0)).isEqualTo("a");
    assertThat(pseudoPath.getName(1)).isEqualTo("b");
  }

  @Test
  public void pseudoPathNoArgs() {
    PseudoPath emptyPath = new PseudoPath();
    assertThat(emptyPath.getNameCount()).isEqualTo(0);
  }

  @Test
  public void pseudoPathToPath() {
    PseudoPath pseudoPath = new PseudoPath("a", "b");
    Path path = pseudoPath.toPath();

    assertThat(path.getName(0).toString()).isEqualTo("a");
    assertThat(path.getName(1).toString()).isEqualTo("b");
  }

  @Test
  public void pathToPseudoPath() {
    Path path = Paths.get("a", "b");
    PseudoPath pseudoPath = new PseudoPath(path);

    assertThat(pseudoPath.getName(0)).isEqualTo("a");
    assertThat(pseudoPath.getName(1)).isEqualTo("b");
    assertThat(pseudoPath.getNameCount()).isEqualTo(2);
  }

  @Test
  public void testResolve() {
    PseudoPath pseudoPath = new PseudoPath("a", "b");
    PseudoPath newPath = pseudoPath.resolve("c");

    assertThat(newPath.getNameCount()).isEqualTo(3);
    assertThat(newPath.getName(2)).isEqualTo("c");

    //empty pseudopath
    pseudoPath = new PseudoPath();
    newPath = pseudoPath.resolve("c");
    assertThat(newPath.getNameCount()).isEqualTo(1);
    assertThat(newPath.getName(0)).isEqualTo("c");
  }

  @Test
  public void testGetParent() throws Exception {
    PseudoPath a_b = new PseudoPath("a", "b");
    PseudoPath a = new PseudoPath("a");
    PseudoPath empty = new PseudoPath();

    assertThat(a_b.getParent()).isEqualTo(a);
    assertThat(a.getParent()).isEqualTo(empty);

    assertThat(empty.getParent()).isNull();

  }
}
