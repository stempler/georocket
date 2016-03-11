package io.georocket.storage.hdfs;

import io.georocket.constants.ConfigConstants;
import io.georocket.storage.StorageTest;
import io.georocket.storage.Store;
import io.georocket.util.PathUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test {@link HDFSStore}
 * @author Andrej Sajenko
 */
public class HDFSStoreTest extends StorageTest {

  /**
   * Create a temporary tempFolder
   */
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String hdfsLocalRoot;
  private String hdfsAdress;

  /**
   * Set up test dependencies.
   */
  @Before
  public void setUp() {
    hdfsLocalRoot = tempFolder.getRoot().getAbsolutePath();
    hdfsAdress = "file://" + hdfsLocalRoot;
  }

  private void configureVertx(Vertx vertx) {
    JsonObject config = vertx.getOrCreateContext().config();

    System.setProperty("hadoop.home.dir", "/"); // prevent exception -> http://stackoverflow.com/questions/19840056/failed-to-detect-a-valid-hadoop-home-directory

    config.put(ConfigConstants.STORAGE_HDFS_PATH, hdfsLocalRoot);
    config.put(ConfigConstants.STORAGE_HDFS_DEFAULT_FS, hdfsAdress);
  }

  @Override
  protected Store createStore(Vertx vertx) {
    this.configureVertx(vertx);
    return new HDFSStore(vertx);
  }

  @Override
  protected Handler<Future<String>> prepare_Data(TestContext context, Vertx vertx, String path) {
    return h -> {
      String destinationFolder = path == null || path.isEmpty() ? hdfsLocalRoot : PathUtils.join(hdfsLocalRoot, path);

      Path filePath = Paths.get(destinationFolder, id);

      try {
        Files.createDirectories(Paths.get(destinationFolder));
        Files.write(filePath, chunkContent.getBytes());
      } catch (IOException ex) {
        context.fail("Test file creation failed.: " + ex.getMessage());
      }

      h.complete(filePath.toString().replace(hdfsLocalRoot + "/", ""));
    };
  }

  @Override
  protected Handler<Future<Object>> validate_after_Store_add(TestContext context, Vertx vertx, String path) {
    return h -> {
      String fileDestination = (path == null || path.isEmpty()) ? hdfsLocalRoot : PathUtils.join(hdfsLocalRoot, path);

      File folder = new File(fileDestination);

      if (!folder.exists()) {
        context.fail("Test expected to find a tempFolder after calling HDFSStore::add. FolderPath('" + folder.getAbsolutePath() + "')");
      }

      File[] files = folder.listFiles();

      if (files == null || files.length != 2) {
        context.fail("Test expected to find two file after calling HDFSStore::add FolderPath('" + folder.getAbsolutePath() + "')");
      }

      // Hadoop client create two files, one starts with a point '.' and ends with the extension ".crc". The other file contains the needed content.
      File file = files[0].getPath().endsWith(".crc") ? files[1] : files[0];

      try {
        List<String> lines = Files.readAllLines(file.toPath());

        if (lines.isEmpty()) {
          context.fail("Test expected to find any content after calling HDFSStore::add FilePath('" + file.getAbsolutePath() + "')");
        }

        String firstLine = lines.get(0);

        context.assertEquals(chunkContent, firstLine);

      } catch (IOException ex) {
        context.fail("Could not read a file where the test expected to find one, after calling HDFSStore::add.  ExMsg: " + ex.getMessage());
      }

      h.complete();
    };
  }

  @Override
  protected Handler<Future<Object>> validate_after_Store_delete(TestContext context, Vertx vertx, String path) {
    return h -> {
      if (Files.exists(Paths.get(path))) {
        context.fail("Test expected to find zero files after calling HDFSStore::delete. FilePath('" + path + "')");
      }

      h.complete();
    };
  }

}
