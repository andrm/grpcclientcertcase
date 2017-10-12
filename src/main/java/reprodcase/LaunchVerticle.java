package reprodcase;

import io.grpc.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.TCPSSLOptions;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaunchVerticle extends AbstractVerticle {
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(LaunchVerticle.class);
    private static final Context.Key<String> SSL_COMMON_NAME = Context.key("SSLCOMMONNAME");

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.debug("Starting Launch Verticle");
        HlwGreeterService service = new HlwGreeterService();
        ServerServiceDefinition sd = ServerInterceptors.intercept(service,
                new ServerInterceptor() {

            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata,
                                                                         ServerCallHandler<ReqT, RespT> serverCallHandler) {
                LOGGER.debug("We're in");
                SSLSession sslSession = serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
                if (sslSession == null) {
                    return serverCallHandler.startCall(serverCall, metadata);
                }

                String cn = "";
                try {
                    Certificate[] certs = sslSession.getPeerCertificates();
                    if (certs == null) LOGGER.warn("No peer certs");
                    LOGGER.debug("Certs:"+certs.length);
                    Principal principal = sslSession.getPeerPrincipal();
                    if (!(principal instanceof X500Principal)) {
                        LOGGER.warn("No certificate principal information");
                        return serverCallHandler.startCall(serverCall, metadata);
                    }
                    X500Principal x509principal = (X500Principal)principal;
                    Pattern p = Pattern.compile("(^|,)CN=([^,]*)(,|$)");
                    //cn = p.matcher(principal.getName()).find().group(2);
                    Matcher matcher = p.matcher(x509principal.getName());
                    if(matcher.find()) cn = matcher.group(2);
                    LOGGER.debug("CN:"+cn);
                } catch (SSLPeerUnverifiedException e) {
                    LOGGER.warn("Peer is not verified:"+e.getMessage(),e);
                    return serverCallHandler.startCall(serverCall, metadata);
                }
                return Contexts.interceptCall(
                        Context.current().withValue(SSL_COMMON_NAME, cn), serverCall, metadata, serverCallHandler);
            }
        });

        VertxServer server = VertxServerBuilder.
                forPort(vertx,  8080)
                //.useTransportSecurity(new File("TestServerChain.pem"),
                //        new File("TestServer.pem"))
                .useSsl(this::setOpts)
                .addService(sd).build();
        server.start(ar -> {
            if (ar.succeeded()) {
                LOGGER.debug("gRPC configuration service started");
            } else {
                LOGGER.debug("Could not start server " + ar.cause().getMessage());
                System.exit(1);
            }
        });
    }

    private void setOpts(TCPSSLOptions tcpsslOptions) {
        //JksOptions trustOptions = new JksOptions()
        //        .setPath("certs/truststore.jks")
        //        .setPassword("testpw");
        PemTrustOptions trustOptions = new PemTrustOptions()
               .addCertPath("TestCA.crt");
        //PfxOptions trustOptions = new PfxOptions()
        //        .setPath("certs/testclient.p12")
        //        .setPassword("testpw");
        HttpServerOptions options = (HttpServerOptions)tcpsslOptions;
        options.setSsl(true)
                .setUseAlpn(true)
                .setClientAuth(ClientAuth.REQUIRED)
                //.setClientAuthRequired(true)
                .setSni(true)
                //.setTrustStoreOptions(trustOptions)
                .setTrustOptions(trustOptions)
                .setKeyStoreOptions(new JksOptions()
                        .setPath("server-keystore.jks")
                        .setPassword("testpw"))
        //.setOpenSslEngineOptions(new OpenSSLEngineOptions().setSessionCacheEnabled(false))
        ;
    }

}
