package reprodcase;

import io.vertx.core.spi.launcher.DefaultCommandFactory;

/**
 * Created by vasquez on 1/2/17.
 */
public class CommandFactory extends DefaultCommandFactory<ServerCommand> {

    public CommandFactory() { super(ServerCommand.class); }
}
