package ru.niktrop.remote_access.file_system_model.test;

import org.junit.Test;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoPath;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 01.03.13
 * Time: 9:56
 */
public class FSImageTest {
  private static String testXmlA =
                  "<directory name=\"a\">" +
                    "<directory name=\"b\">" +
                      "<directory name=\"c\" />" +
                    "</directory>" +
                    "<file name=\"f\" />" +
                  "</directory>";
  private static String testXmlB =
                  "<directory name=\"b\">" +
                    "<file name=\"z\" />" +
                  "</directory>";
  private static String testXmlC =
                  "<directory name=\"c\" />";

  @Test
  public void addToNotADirectoryCausesException() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);
    PseudoPath f = new PseudoPath("f");
    FSImage childB = FSImages.getFromXml(testXmlB);
    try {
      fsi.addToDirectory(f, childB);
      fail("Stuff can be added only to directories.");
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Can add files only to directories.");
    }
  }

  @Test
  public void addToDirWithoutReplace() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlB);

    FSImage childC = FSImages.getFromXml(testXmlC);
    PseudoPath pathForB = new PseudoPath();
    fsi.addToDirectory(pathForB, childC);
    String xmlResult =
            "<directory name=\"b\">" +
              "<file name=\"z\" />" +
              "<directory name=\"c\" />" +
            "</directory>";

    assertThat(fsi.toXml()).isEqualTo(xmlResult);
  }

  @Test
  public void addToDirWithReplace() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);

    FSImage childB = FSImages.getFromXml(testXmlB);
    //empty path corresponds to root directory "a"
    PseudoPath pathForB = new PseudoPath();
    fsi.addToDirectory(pathForB, childB);
    String xmlResultB =
            "<directory name=\"a\">" +
              "<directory name=\"b\">" +
                "<file name=\"z\" />" +
              "</directory>" +
              "<file name=\"f\" />" +
            "</directory>";

    assertThat(fsi.toXml()).isEqualTo(xmlResultB);
  }

  @Test
  public void testDeletePath() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);
    PseudoPath b_c = new PseudoPath("b", "c");
    PseudoPath f = new PseudoPath("f");

    fsi.deletePath(b_c);
    String xmlWithoutC =
            "<directory name=\"a\">" +
              "<directory name=\"b\" />" +
              "<file name=\"f\" />" +
            "</directory>";
    assertThat(fsi.toXml()).isEqualTo(xmlWithoutC);

    String xmlWithoutCandF =
            "<directory name=\"a\">" +
              "<directory name=\"b\" />" +
            "</directory>";
    fsi.deletePath(f);

    assertThat(fsi.toXml()).isEqualTo(xmlWithoutCandF);

  }

  @Test
  public void DeleteEmptyPathCausesException() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);
    PseudoPath empty = new PseudoPath();

    try {
      fsi.deletePath(empty);
      fail("IllegalAccessException expected on empty path");
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Empty path can not be deleted.");
    }
  }

  @Test
  public void testContains() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);
    PseudoPath b_c = new PseudoPath("b", "c");
    PseudoPath b_c_d = b_c.resolve("d");
    PseudoPath f = new PseudoPath("f");
    PseudoPath empty = new PseudoPath();
    PseudoPath x = new PseudoPath("x");

    assertThat(fsi.contains(b_c));
    assertThat(fsi.contains(b_c_d)).isFalse();
    assertThat(fsi.contains(f));
    assertThat(fsi.contains(empty));
    assertThat(fsi.contains(x)).isFalse();

  }

  @Test
  public void testAddFile() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);
    PseudoPath b_f = new PseudoPath("b", "f");
    PseudoPath g = new PseudoPath("g");
    PseudoPath empty = new PseudoPath();
    PseudoPath c_f = new PseudoPath("c", "f");

    assertThat(fsi.contains(b_f)).isFalse();
    assertThat(fsi.contains(g)).isFalse();
    assertThat(fsi.contains(c_f)).isFalse();

    fsi.addFile(b_f);
    assertThat(fsi.contains(b_f));

    fsi.addFile(g);
    assertThat(fsi.contains(g));


    try {
      fsi.addFile(empty);
      fail("Parent directory must exist");
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Empty pseudopath has no parent.");
    }

    try {
      fsi.addFile(c_f);
      fail("Parent directory must exist");
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Parent directory does not exist.");
    }
  }
}
