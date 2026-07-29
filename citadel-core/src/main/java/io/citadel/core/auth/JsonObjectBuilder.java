package io.citadel.core.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

final class JsonObjectBuilder {

  private final JsonObject obj;

  private JsonObjectBuilder() {
    this.obj = new JsonObject();
  }

  static JsonObjectBuilder create() {
    return new JsonObjectBuilder();
  }

  JsonObjectBuilder put(String key, String value) {
    obj.addProperty(key, value);
    return this;
  }

  JsonObjectBuilder put(String key, int value) {
    obj.addProperty(key, value);
    return this;
  }

  JsonObjectBuilder putObject(String key, JsonObjectBuilder builder) {
    obj.add(key, builder.build());
    return this;
  }

  JsonObjectBuilder putArray(String key, String[] values) {
    JsonArray arr = new JsonArray();
    for (String v : values) {
      arr.add(v);
    }
    obj.add(key, arr);
    return this;
  }

  JsonObject build() {
    return obj;
  }
}
