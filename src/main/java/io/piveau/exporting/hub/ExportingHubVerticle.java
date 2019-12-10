package io.piveau.exporting.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.piveau.pipe.Pipe;
import io.piveau.pipe.connector.PipeContext;
import io.piveau.utils.JenaUtils;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportingHubVerticle extends AbstractVerticle {

    public static final String ADDRESS = "io.piveau.pipe.exporting.hub.queue";

    private Logger log = LoggerFactory.getLogger(getClass());

    private WebClient client;

    private String hubAddress;
    private String hubApiKey;
    private boolean hubAddHash;

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer(ADDRESS, this::handlePipe);
        client = WebClient.create(vertx);

        ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray()
                        .add("PIVEAU_HUB_ADDRESS")
                        .add("PIVEAU_HUB_APIKEY")
                        .add("PIVEAU_HUB_ADD_HASH")));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(envStoreOptions));
        retriever.getConfig(ar -> {
            if (ar.succeeded()) {
                hubAddress = ar.result().getString("PIVEAU_HUB_ADDRESS", "http://piveau-hub:8080");
                hubApiKey = ar.result().getString("PIVEAU_HUB_APIKEY", "");
                hubAddHash = ar.result().getBoolean("PIVEAU_HUB_ADD_HASH", false);
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    private boolean fieldEquals(ObjectNode dataInfo, String field, String value) {
        return dataInfo.hasNonNull(field) && dataInfo.get(field).asText().equals(value);
    }

    private void handlePipe(Message<PipeContext> message) {
        PipeContext pipeContext = message.body();
        Pipe pipe = pipeContext.getPipe();
        pipeContext.log().debug(Json.encodePrettily(pipe));

        ObjectNode dataInfo = pipeContext.getDataInfo();

        JsonNode config = pipeContext.getConfig();

        ObjectNode hub = ((ObjectNode)config).with("hub");
        String address = hub.path("endpoint").path("address").asText(hubAddress);
        String apiKey = hub.path("endpoint").path("apiKey").asText(hubApiKey);

        if(fieldEquals(dataInfo, "content", "identifierList")) {
            deleteIdentifiers(address, apiKey, pipeContext);
        } else if(fieldEquals(dataInfo, "content", "metrics")) {
            exportMetrics(address, apiKey, pipeContext);
        } else {
            exportMetadata(address, apiKey, pipeContext);
        }
    }

    private void exportMetadata(String address, String apiKey, PipeContext pipeContext) {

        JsonNode dataInfo = pipeContext.getDataInfo();

        String identifier = URLEncoder.encode(dataInfo.path("identifier").asText(), StandardCharsets.UTF_8);

        log.debug("Content type: {}", pipeContext.getMimeType());

        HttpRequest<Buffer> request = client.putAbs(address + "/datasets/" + identifier)
                .putHeader("Authorization", apiKey)
                .putHeader("Content-Type", pipeContext.getMimeType())
                .addQueryParam("catalogue", dataInfo.path("catalogue").textValue());

        if (dataInfo.hasNonNull("hash")) {
            request.addQueryParam("hash", dataInfo.path("hash").textValue());
        } else if (hubAddHash) {
            Model model = JenaUtils.read(pipeContext.getStringData().getBytes(), pipeContext.getMimeType());
            request.addQueryParam("hash", JenaUtils.canonicalHash(model));
        }

        request.sendBuffer(Buffer.buffer(pipeContext.getStringData()), ar -> {
            if (ar.succeeded()) {
                if (ar.result().statusCode() == 201) {
                    pipeContext.log().info("Dataset created: {}", dataInfo);
                } else if (ar.result().statusCode() == 200) {
                    pipeContext.log().info("Dataset updated: {}", dataInfo);
                } else if (ar.result().statusCode() == 304) {
                    pipeContext.log().info("Dataset skipped: {}", dataInfo);
                } else {
                    pipeContext.setFailure(new Throwable(identifier, new Throwable("" + ar.result().statusCode() + " - " + ar.result().statusMessage() + " - " + ar.result().bodyAsString())));
                }
            } else {
                pipeContext.setFailure(ar.cause());
            }
        });
    }

    private void exportMetrics(String address, String apiKey, PipeContext pipeContext) {

        JsonNode dataInfo = pipeContext.getDataInfo();

        String identifier = URLEncoder.encode(dataInfo.path("identifier").asText(), StandardCharsets.UTF_8);

        Dataset ds = JenaUtils.readDataset(pipeContext.getStringData().getBytes(),null);

        HttpRequest<Buffer> request = client.putAbs(address + "/metrics/" + identifier)
                .putHeader("Authorization", apiKey)
                .putHeader("Content-Type", pipeContext.getMimeType())
                .addQueryParam("catalogue", dataInfo.path("catalogue").textValue());

        request.sendBuffer(Buffer.buffer(pipeContext.getStringData()), ar -> {
            if(ar.succeeded()) {
                if(ar.result().statusCode() == 201) {
                    pipeContext.log().info("Metrics graph created: {}", dataInfo);
                } else if(ar.result().statusCode() == 200) {
                    pipeContext.log().info("Metrics graph updated: {}", dataInfo);
                }  else {
                    pipeContext.setFailure(new Throwable(identifier, new Throwable("" + ar.result().statusCode() + " - " + ar.result().statusMessage() + " - " + ar.result().bodyAsString())));
                }
            } else {
                pipeContext.setFailure(ar.cause());
            }
        });
    }


    private void deleteIdentifiers(String address, String apiKey, PipeContext pipeContext) {

        String catalogueId = pipeContext.getDataInfo().path("catalogue").asText();
        HttpRequest<Buffer> request = client.getAbs(address + "/datasets")
                .putHeader("Authorization", apiKey)
                .addQueryParam("catalogue", catalogueId)
                .addQueryParam("sourceIds", "true");

        request.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                if (response.statusCode() == 200) {
                    Set<String> sourceIds = new JsonArray(pipeContext.getStringData()).stream().map(Object::toString).collect(Collectors.toSet());
                    Set<String> targetIds = ar.result().bodyAsJsonArray().stream().map(Object::toString).collect(Collectors.toSet());
                    int targetSize = targetIds.size();
                    targetIds.removeAll(sourceIds);
                    pipeContext.log().info("Source {}, target {}, deleting {} datasets", sourceIds.size(), targetSize, targetIds.size());
                    targetIds.forEach(datasetId -> deleteDataset(address, apiKey, pipeContext, datasetId, catalogueId));
                } else {
                    pipeContext.log().error(response.statusMessage());
                }
            } else {
                pipeContext.setFailure(ar.cause());
            }
        });
    }

    private void deleteDataset(String address, String apiKey, PipeContext pipeContext, String datasetId, String catalogueId) {
        String encoded = URLEncoder.encode(datasetId, StandardCharsets.UTF_8);

        HttpRequest<Buffer> request = client.deleteAbs(address + "/datasets/" + encoded)
                .putHeader("Authorization", apiKey)
                .addQueryParam("catalogue", catalogueId);
        request.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                switch (response.statusCode()) {
                    case 200:
                        pipeContext.log().info("Dataset '{}' deleted", datasetId);
                        break;
                    case 404:
                        pipeContext.log().warn("Dataset '{}' not found", datasetId);
                        break;
                    default:
                        pipeContext.log().error("{} - {} ({})", response.statusCode(), response.statusMessage(), datasetId);
                }
            } else {
                pipeContext.log().error("Delete dataset", ar.cause());
            }
        });
    }

}
