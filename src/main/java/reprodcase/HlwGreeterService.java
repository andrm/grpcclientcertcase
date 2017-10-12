package reprodcase;

import io.vertx.core.Future;
import reprodcase.proto.GreeterGrpc;
import reprodcase.proto.HelloWorldGreeter;

public class HlwGreeterService  extends GreeterGrpc.GreeterVertxImplBase {

    @Override
    public void sayHello(HelloWorldGreeter.HelloRequest request, Future<HelloWorldGreeter.HelloReply> response) {
        HelloWorldGreeter.HelloReply reply = HelloWorldGreeter.HelloReply.newBuilder()
                .setMessage("I'm here").build();
        response.complete(reply);
    }
}
