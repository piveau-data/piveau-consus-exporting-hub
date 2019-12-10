package io.piveau.exporting;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testing the exporter")
@ExtendWith(VertxExtension.class)
class ExportingTest {

    @BeforeEach
    void startImporter(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(), testContext.completing());
    }

    @Test
    @DisplayName("Receiving dataset and exporting it to hub")
    @Timeout(value = 1, timeUnit = TimeUnit.MINUTES)
    void sendDataset(Vertx vertx, VertxTestContext testContext) {
        Checkpoint checkpoint = testContext.checkpoint(2);

        // Mockup hub
        vertx.createHttpServer().requestHandler(request -> {
            testContext.verify(() -> {
                assertEquals("any-kind", request.getParam("catalogue"));
                assertEquals("text/plain", request.getHeader("Content-Type"));
                assertEquals("not-necessary", request.getHeader("Authorization"));
                assertTrue(request.path().endsWith("/datasets/whatever"));
                request.bodyHandler(buffer -> {
                    assertEquals("Test message", buffer.toString());
                });
            });
            request.response().setStatusCode(200).end(ar -> {
                if(ar.succeeded()) {
                    checkpoint.flag();
                } else {
                    testContext.failNow(ar.cause());
                }
            });
        }).listen(8098);

        // Injecting pipe
        sendPipe("test1-pipe.json", vertx, testContext, checkpoint);
    }

    @Test
    @DisplayName("Receiving identifier list and deleting one dataset")
    @Timeout(value = 1, timeUnit = TimeUnit.MINUTES)
    void sendIdentifierList(Vertx vertx, VertxTestContext testContext) {
        Checkpoint checkpoint = testContext.checkpoint(3);

        // Mockup hub
        vertx.createHttpServer().requestHandler(request -> {
            if (request.method() == HttpMethod.GET && request.path().equals("/hub/datasets")) {
                // assert fetch identifiers
                testContext.verify(() -> {
                    assertEquals("any-kind", request.getParam("catalogue"));
                    assertEquals("not-necessary", request.getHeader("Authorization"));
                });
                request.response().setStatusCode(200).end(new JsonArray().add("first-identifier").add("second-identifier").toBuffer(), ar -> {
                    if(ar.succeeded()) {
                        checkpoint.flag();
                    } else {
                        testContext.failNow(ar.cause());
                    }
                });
            } else if (request.method() == HttpMethod.DELETE && request.path().equals("/hub/datasets/second-identifier")) {
                // assert delete
                testContext.verify(() -> {
                    assertEquals("any-kind", request.getParam("catalogue"));
                    assertEquals("not-necessary", request.getHeader("Authorization"));
                });
                request.response().setStatusCode(200).end(ar -> {
                    if(ar.succeeded()) {
                        checkpoint.flag();
                    } else {
                        testContext.failNow(ar.cause());
                    }
                });
            } else {
                testContext.failNow(new Throwable("Unexpected request to hub."));
            }
        }).listen(8098);

        // Injecting pipe
        sendPipe("test2-pipe.json", vertx, testContext, checkpoint);
    }


    @Test
    @DisplayName("Receiving dataset with metrics and exporting the metrics to hub")
    @Timeout(value = 1, timeUnit = TimeUnit.MINUTES)
    void sendMetrics(Vertx vertx, VertxTestContext testContext) {
        Checkpoint checkpoint = testContext.checkpoint(2);

        // Mockup hub
        vertx.createHttpServer().requestHandler(request -> {
            testContext.verify(() -> {
                assertEquals("any-kind", request.getParam("catalogue"));
                assertEquals("application/trig", request.getHeader("Content-Type"));
                assertEquals("not-necessary", request.getHeader("Authorization"));
                assertTrue(request.path().endsWith("/metrics/sampleDataset"));
            });
            request.response().setStatusCode(200).end(ar -> {
                if (ar.succeeded()) {
                    checkpoint.flag();
                } else {
                    testContext.failNow(ar.cause());
                }
            });
        }).listen(8098);

        // Injecting pipe
        sendPipe("test3-pipe.json", vertx, testContext, checkpoint);
    }

    private void sendPipe(String pipeFile, Vertx vertx, VertxTestContext testContext, Checkpoint checkpoint) {
        vertx.fileSystem().readFile("src/test/resources/" + pipeFile, result -> {
            if (result.succeeded()) {
                JsonObject pipe = new JsonObject(result.result());
                WebClient client = WebClient.create(vertx);
                client.post(8080, "localhost", "/pipe")
                        .putHeader("Content-Type", "application/json")
                        .sendJsonObject(pipe, testContext.succeeding(response -> testContext.verify(() -> {
                            if (response.statusCode() == 202) {
                                checkpoint.flag();
                            } else {
                                testContext.failNow(new Throwable(response.statusMessage()));
                            }
                        })));
            } else {
                testContext.failNow(result.cause());
            }
        });
    }

}
