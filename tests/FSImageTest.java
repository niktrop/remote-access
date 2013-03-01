import org.junit.Test;

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
  public void addToNotADirectory() throws Exception {
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
    FSImage result = fsi.addToDirectory(pathForB, childC);
    String xmlResult =
            "<directory name=\"b\">" +
            "<file name=\"z\" />" +
            "<directory name=\"c\" />" +
            "</directory>";

    assertThat(result.toXml()).isEqualTo(xmlResult);
  }

  @Test
  public void addToDirWithReplace() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);

    FSImage childB = FSImages.getFromXml(testXmlB);
    //empty path corresponds to root directory "a"
    PseudoPath pathForB = new PseudoPath();
    FSImage resultB = fsi.addToDirectory(pathForB, childB);
    String xmlResultB =
            "<directory name=\"a\">" +
            "<directory name=\"b\">" +
            "<file name=\"z\" />" +
            "</directory>" +
            "<file name=\"f\" />" +
            "</directory>";

    FSImage childC = FSImages.getFromXml(testXmlC);
    PseudoPath pathForC = new PseudoPath("b");
    FSImage resultC = fsi.addToDirectory(pathForC, childC);
    String xmlResultC =
            "<directory name=\"a\">" +
            "<directory name=\"b\">" +
            "<directory name=\"c\" />" +
            "</directory>" +
            "<file name=\"f\" />" +
            "</directory>";

    assertThat(resultB.toXml()).isEqualTo(xmlResultB);
    assertThat(resultC.toXml()).isEqualTo(xmlResultC);
  }

  @Test
  public void testDeletePath() throws Exception {
    FSImage fsi = FSImages.getFromXml(testXmlA);
    PseudoPath b_c = new PseudoPath("b", "c");
    PseudoPath f = new PseudoPath("f");

    FSImage withoutB_C = fsi.deletePath(b_c);
    FSImage withoutF = fsi.deletePath(f);

    String xmlWithoutB_C =
            "<directory name=\"a\">" +
            "<directory name=\"b\" />" +
            "<file name=\"f\" />" +
            "</directory>";
    String xmlWithoutF =
            "<directory name=\"a\">" +
            "<directory name=\"b\">" +
            "<directory name=\"c\" />" +
            "</directory>" +
            "</directory>";

    assertThat(withoutB_C.toXml()).isEqualTo(xmlWithoutB_C);
    assertThat(withoutF.toXml()).isEqualTo(xmlWithoutF);

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
}
