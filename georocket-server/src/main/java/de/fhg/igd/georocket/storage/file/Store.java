package de.fhg.igd.georocket.storage.file;

import de.fhg.igd.georocket.util.ChunkMeta;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A store for chunks
 * @author Michel Kraemer
 */
public interface Store {
  /**
   * Add a chunk to the store
   * @param chunk the chunk to add
   * @param meta the chunk's metadata
   * @param handler will be called when the chunk has been added to the store
   */
  void add(String chunk, ChunkMeta meta, Handler<AsyncResult<Void>> handler);
  
  /**
   * Get a chunk from the store
   * @param name the chunk's name
   * @param handler will be called when the chunk has been retrieved from the store
   */
  void get(String name, Handler<AsyncResult<ChunkReadStream>> handler);
}
