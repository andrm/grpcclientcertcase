package reprodcase;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import reprodcase.proto.GreeterGrpc;
import reprodcase.proto.HelloWorldGreeter;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class TestClient {
    private ManagedChannel channel;

    private GreeterGrpc.GreeterBlockingStub stub;

    public static void main(String[] args) {
        try {
            TestClient client = new TestClient("localhost", 8980);
            System.out.println("Started");
            String s = client.get();
            System.out.println("Done:" + s);
        } catch (Exception e) {
            System.err.println("Problem with TestClient:" + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public TestClient(String host, int port) throws Exception {
        ManagedChannelBuilder builder = NettyChannelBuilder
                .forAddress(host, port)
                .sslContext(GrpcSslContexts.forClient()
                        .startTls(true)
                        //.clientAuth(ClientAuth.REQUIRE)

                        .trustManager(new File("TestCA.crt"))
                        .keyManager(new File("TestClient.crt"),
                                new File("TestClient.p8"))
                        .build());
        init(builder);

    }

    private void init(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        stub = GreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String get() {
        HelloWorldGreeter.HelloRequest request = HelloWorldGreeter.HelloRequest.newBuilder()
                .setName("Sth").build();
        HelloWorldGreeter.HelloReply reply = stub.sayHello(request);
        System.out.println("Reply:" + reply.getMessage());
        return reply.getMessage();
    }
}