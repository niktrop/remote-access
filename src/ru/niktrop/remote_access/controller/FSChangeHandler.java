package ru.niktrop.remote_access.controller;

import ru.niktrop.remote_access.commands.FSChange;


/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 18.03.13
 * Time: 15:32
 */

/**
 * Processes all FSChanges from FileSystemWatcher.
 * */
public class FSChangeHandler {

  private final FileSystemWatcher fsWatcher;
  private final Controller controller;

  public FSChangeHandler(FileSystemWatcher fsWatcher, Controller controller) {
    this.fsWatcher = fsWatcher;
    this.controller = controller;
  }

  public void runHandler() {
    Thread fsChangeHandler = new Thread("FSChange handler") {
      @Override
      public void run() {
        CommandManager cm = controller.getCommandManager();
        while(true) {
          FSChange fsChange = fsWatcher.takeFSChange();
          if (fsChange == null)
            continue;

          cm.executeCommand(fsChange);

      if ( !controller.isClient()) {
        cm.sendCommand(fsChange);
      }
        }
      }
    };
    fsChangeHandler.start();

  }
}
