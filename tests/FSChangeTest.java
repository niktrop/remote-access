import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 04.03.13
 * Time: 12:51
 */
public class FSChangeTest {
  @Test
  public void FSChangeToStringAndBack() throws Exception {
    PseudoPath path = new PseudoPath("a", "b");
    FSChange change = new FSChange(ChangeType.CREATE_DIR, "test", new PseudoPath("a", "b"));
    String changeAsString = change.toString();
    FSChange changeFromString = FSChange.fromString(changeAsString);

    assertThat(changeFromString.getType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(changeFromString.getFsAlias()).isEqualTo("test");
    assertThat(changeFromString.getPath()).isEqualTo(path);
  }
}
