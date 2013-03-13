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
import java.nio.file.Path;
import java.nio.file.WatchService;


/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 11.03.13
 * Time: 9:19
 */
public class FileTableTest {

  public static void main(String[] args) throws IOException {
    Path discC = FileSystems.getDefault().getPath("C:\\\\");
    final Controller controller = Controllers.getClientController();
    WatchService watcher = controller.getWatcher();
    int maxDepth = controller.getMaxDepth();
    FSImage fsi = FSImages.getFromDirectory(discC, maxDepth, watcher);
    controller.addFSImage(fsi);
    final PseudoFile dir = new PseudoFile(fsi, new PseudoPath());

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
