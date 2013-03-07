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
    FSChange change2 =
            new FSChange(ChangeType.CREATE_DIR, "test", new PseudoPath("a", "b"), "<test xml />");

    String changeAsString = change.serializeToString();
    String change2AsString = change2.serializeToString();

    FSChange changeFromString = FSChange.fromString(changeAsString);
    FSChange change2FromString = FSChange.fromString(change2AsString);

    assertThat(changeFromString.getType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(changeFromString.getFsiUuid()).isEqualTo("test");
    assertThat(changeFromString.getPath()).isEqualTo(path);

    assertThat(change2FromString.getType()).isEqualTo(ChangeType.CREATE_DIR);
    assertThat(change2FromString.getFsiUuid()).isEqualTo("test");
    assertThat(change2FromString.getPath()).isEqualTo(path);
    assertThat(change2FromString.getXmlFSImage()).isEqualTo("<test xml />");
  }
}
