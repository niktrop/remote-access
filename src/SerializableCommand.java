/**
 * Created with IntelliJ IDEA.
 * User: Nikolai Tropin
 * Date: 06.03.13
 * Time: 21:45
 */
public interface SerializableCommand {
  public void execute(CommandContext context);
  String getStringRepresentation();
  SerializableCommand fromString(String string);
}
