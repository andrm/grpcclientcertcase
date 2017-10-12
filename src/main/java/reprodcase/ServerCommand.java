package reprodcase;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.launcher.DefaultCommand;

import java.util.function.Consumer;


@SuppressWarnings({"WeakerAccess", "unused"})
@Name("CertServer")
@Summary("Server Engine")
@Description("Starts the Certserver engine.")
public class ServerCommand extends DefaultCommand {
    private Integer webPort;
    @Option(argName="webPort", shortName="w",longName="webPort")
    public void setWebPort(Integer webPort) {
        this.webPort = webPort;
    }

    @Override
    public void run() throws CLIException {
        VertxOptions options = new VertxOptions();
        Vertx v = Vertx.vertx(options);
        this.init(v);
    }

    private void init(Vertx vertx) {

        SharedData sd = vertx.sharedData();
        LocalMap<String, Object> localConfig = sd.getLocalMap("server.config");
        if (webPort == null || webPort == 0) {
            webPort = 8980;
        }
        localConfig.put("WEB_PORT", webPort);
        LaunchVerticle base = new LaunchVerticle();
        Consumer<Vertx> runner = vertx2 -> {
            try {
                vertx2.deployVerticle(base);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };
        runner.accept(vertx);
    }

}
