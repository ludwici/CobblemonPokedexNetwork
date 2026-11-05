package com.ludwici.cobblemonpokedexnetwork.storage;

import com.ludwici.cobblemonpokedexnetwork.NetworkEntry;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOptions;
import net.minecraft.resources.ResourceLocation;
import org.bson.Document;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class MongoPokedexStorageBackend implements PokedexStorageBackend {
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoPokedexStorageBackend(PokedexStorageConfig config) {
        MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString(config.mongoDBConnectionString)).applyToClusterSettings(builder -> builder.serverSelectionTimeout(5, TimeUnit.SECONDS)).applyToSocketSettings(builder -> builder.readTimeout(10, TimeUnit.SECONDS)).build();
        MongoClient created = MongoClients.create(settings);
        try {
            created.getDatabase(config.mongoDBDatabaseName).runCommand(new Document("ping", 1));
            this.collection = created.getDatabase(config.mongoDBDatabaseName).getCollection(config.mongoDBCollectionName);
            this.client = created;
        } catch (RuntimeException e) {
            created.close();
            throw e;
        }
    }

    @Override
    public Map<ResourceLocation, Map<String, NetworkEntry>> load() throws IOException {
        Map<ResourceLocation, Map<String, NetworkEntry>> result = new LinkedHashMap<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                ResourceLocation speciesId;
                try {
                    speciesId = ResourceLocation.parse(document.getString("_id"));
                } catch (RuntimeException e) {
                    throw new IOException("Invalid CPN MongoDB species ID", e);
                }
                Document forms = document.get("forms", Document.class);
                if (forms == null) {
                    throw new IOException("CPN MongoDB document is missing forms for " + speciesId);
                }
                if (result.putIfAbsent(speciesId, JsonPokedexStorageBackend.deserialize(forms.toJson())) != null) {
                    throw new IOException("Duplicate CPN MongoDB species: " + speciesId);
                }
            }
        }
        return result;
    }

    @Override
    public void save(ResourceLocation speciesId, Map<String, NetworkEntry> forms) {
        Document document = new Document("_id", speciesId.toString()).append("forms", Document.parse(JsonPokedexStorageBackend.serialize(forms)));
        collection.replaceOne(new Document("_id", speciesId.toString()), document, new ReplaceOptions().upsert(true));
    }

    @Override
    public void close() {
        client.close();
    }
}
