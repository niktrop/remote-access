import nu.xom.ParsingException;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 28.02.13
 * Time: 13:43
 */
public class PseudoFileTest {
  /*
    a/b/f.txt
    c/d/
    f.txt
   */
  private final static String testXml =
          "<root name=\"test\">\n" +
          "\t<directory name=\"a\">\n" +
          "\t\t<directory name=\"b\">\n" +
          "\t\t\t<file name=\"f.txt\"/>\n" +
          "\t\t</directory>\n" +
          "\t</directory>\n" +
          "\t<directory name=\"c\">\n" +
          "\t\t<directory name=\"d\"/>\n" +
          "\t</directory>\n" +
          "\t<file name=\"f.txt\"/>" +
          "</root>";

  @Test
  public void testGetType() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXml);
    PseudoPath root = new PseudoPath();
    PseudoPath a = new PseudoPath("a");
    PseudoPath f = new PseudoPath("f.txt");
    PseudoPath c_d = new PseudoPath("c", "d");

    assertThat(new PseudoFile(fsi, root).getType()).isEqualTo(FileType.ROOT.getName());
    assertThat(new PseudoFile(fsi, a).getType()).isEqualTo(FileType.DIR.getName());
    assertThat(new PseudoFile(fsi, f).getType()).isEqualTo(FileType.FILE.getName());
    assertThat(new PseudoFile(fsi, c_d).getType()).isEqualTo(FileType.DIR.getName());
  }

  @Test
  public void testGetContentNames() throws ParsingException, IOException {
    FSImage fsi = FSImages.getFromXml(testXml);
    List<String> rootContent = (new PseudoFile(fsi, new PseudoPath())).getContentNames();
    List<String> a_Content = (new PseudoFile(fsi, new PseudoPath("a"))).getContentNames();
    List<String> c_d_Content = (new PseudoFile(fsi, new PseudoPath("c","d"))).getContentNames();
    List<String> a_b_f_Content = (new PseudoFile(fsi, new PseudoPath("a","b","f.txt"))).getContentNames();

    assertThat(rootContent).hasSize(3)
            .containsExactly("a","c","f.txt");
    assertThat(a_Content).containsExactly("b");
    assertThat(c_d_Content).isEmpty();
    assertThat(a_b_f_Content).isEmpty();
  }

  @Test
  public void testGetContent() throws ParsingException, IOException {
    FSImage fsi = FSImages.getFromXml(testXml);
    List<PseudoFile> rootContent = (new PseudoFile(fsi, new PseudoPath())).getContent();
    PseudoFile a = new PseudoFile(fsi, new PseudoPath("a"));
    PseudoFile c = new PseudoFile(fsi, new PseudoPath("c"));
    PseudoFile f = new PseudoFile(fsi, new PseudoPath("f.txt"));

    assertThat(rootContent).hasSize(3)
            .containsExactly(a, c, f);
  }

  @Test
  public void testGetName() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXml);

    PseudoPath root = new PseudoPath();
    PseudoPath a = new PseudoPath("a");
    PseudoPath f = new PseudoPath("f.txt");
    PseudoPath c_d = new PseudoPath("c", "d");

    assertThat(new PseudoFile(fsi, root).getName()).isNull();
    assertThat(new PseudoFile(fsi, a).getName()).isEqualTo("a");
    assertThat(new PseudoFile(fsi, f).getName()).isEqualTo("f.txt");
    assertThat(new PseudoFile(fsi, c_d).getName()).isEqualTo("d");
  }

  @Test
  public void testAsStringAndBack() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXml);
    PseudoPath a_b = new PseudoPath("a", "b");
    PseudoFile pseudoFile = new PseudoFile(fsi, a_b);

    String serialized = pseudoFile.serializeToString();
    Map<String,FSImage> fsImageMap = new HashMap<>();
    PseudoFile fromString = PseudoFile.fromSerializedString(serialized, fsImageMap);

    assertThat(fromString).isNull();

    fsImageMap.put(fsi.getUuid(), fsi);
    fromString = PseudoFile.fromSerializedString(serialized, fsImageMap);

    assertThat(fromString).isEqualTo(pseudoFile);

  }
}
