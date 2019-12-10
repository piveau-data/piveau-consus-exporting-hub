package io.piveau.exporting;

import io.piveau.exporting.hub.ExportingHubVerticle;
import io.piveau.pipe.connector.PipeConnector;
import io.vertx.core.*;

import java.util.Arrays;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.deployVerticle(ExportingHubVerticle.class, new DeploymentOptions().setWorker(true), ar -> {
            if (ar.succeeded()) {
                PipeConnector.create(vertx, cr -> {
                    if (cr.succeeded()) {
                        cr.result().consumerAddress(ExportingHubVerticle.ADDRESS);
                        startPromise.complete();
                    } else {
                        startPromise.fail(cr.cause());
                    }
                });
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }

}
