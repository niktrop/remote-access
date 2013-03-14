import nu.xom.ParsingException;
import ru.niktrop.remote_access.Controller;
import ru.niktrop.remote_access.Controllers;
import ru.niktrop.remote_access.file_system_model.FSImage;
import ru.niktrop.remote_access.file_system_model.FSImages;
import ru.niktrop.remote_access.file_system_model.PseudoFile;
import ru.niktrop.remote_access.file_system_model.PseudoPath;
import ru.niktrop.remote_access.gui.FileTable;
import ru.niktrop.remote_access.gui.OneSidePanel;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;


/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 9:19
 */
public class FileTableTest {

  private static Iterable<Path> dirs = FileSystems.getDefault().getRootDirectories();

  public static void main(String[] args) throws IOException {
    final Controller controller = Controllers.getClientController();
    WatchService watcher = controller.getWatcher();
    int maxDepth = controller.getMaxDepth();

    for (Path dir : dirs) {
      if (!Files.isDirectory(dir)) {
        continue;
      }
      FSImage fsi = FSImages.getFromDirectory(dir, maxDepth, watcher);
      controller.addFSImage(fsi);
      System.out.println(dir.toString());
    }

    String testXmlA =
            "<directory name=\"a\" alias=\"test\">" +
                    "<directory name=\"b\">" +
                    "<directory name=\"c\" />" +
                    "</directory>" +
                    "<file name=\"f\" />" +
                    "</directory>";
    try {
      FSImage fsi = FSImages.getFromXml(testXmlA);
      controller.addFSImage(fsi);
    } catch (ParsingException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    FSImage defaultFSImage = controller.getFSImages().iterator().next();
    final PseudoFile dir = new PseudoFile(defaultFSImage, new PseudoPath());

    controller.listenAndHandleFileChanges();

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        JFrame f = new JFrame("Test");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        FileTable fileTable = new FileTable(dir);
        OneSidePanel client = new OneSidePanel(fileTable, controller);

        f.setContentPane(client);

        f.pack();
        f.setLocationByPlatform(true);
        f.setMinimumSize(f.getSize());
        f.setVisible(true);

      }
    });
  }
}
