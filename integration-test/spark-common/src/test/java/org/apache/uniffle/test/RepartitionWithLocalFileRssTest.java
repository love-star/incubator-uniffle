/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.spark.SparkConf;
import org.apache.spark.shuffle.RssSparkConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import org.apache.uniffle.common.compression.Codec;
import org.apache.uniffle.common.rpc.ServerType;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

import static org.apache.uniffle.common.config.RssClientConf.COMPRESSION_TYPE;

public class RepartitionWithLocalFileRssTest extends RepartitionTest {

  @BeforeAll
  public static void setupServers(@TempDir File tmpDir) throws Exception {
    CoordinatorConf coordinatorConf = coordinatorConfWithoutPort();
    Map<String, String> dynamicConf = Maps.newHashMap();
    dynamicConf.put(RssSparkConfig.RSS_STORAGE_TYPE.key(), StorageType.LOCALFILE.name());
    addDynamicConf(coordinatorConf, dynamicConf);
    storeCoordinatorConf(coordinatorConf);

    ShuffleServerConf grpcShuffleServerConf =
        shuffleServerConfWithoutPort(0, null, ServerType.GRPC);
    File dataDir1 = new File(tmpDir, "data1");
    File dataDir2 = new File(tmpDir, "data2");
    String grpcBasePath = dataDir1.getAbsolutePath() + "," + dataDir2.getAbsolutePath();
    grpcShuffleServerConf.setString("rss.storage.type", StorageType.LOCALFILE.name());
    grpcShuffleServerConf.setBoolean(ShuffleServerConf.RSS_TEST_MODE_ENABLE, true);
    grpcShuffleServerConf.setString("rss.storage.basePath", grpcBasePath);
    storeShuffleServerConf(grpcShuffleServerConf);

    ShuffleServerConf nettyShuffleServerConf =
        shuffleServerConfWithoutPort(1, null, ServerType.GRPC_NETTY);
    File dataDir3 = new File(tmpDir, "data3");
    File dataDir4 = new File(tmpDir, "data4");
    String nettyBasePath = dataDir3.getAbsolutePath() + "," + dataDir4.getAbsolutePath();
    nettyShuffleServerConf.setString("rss.storage.type", StorageType.LOCALFILE.name());
    nettyShuffleServerConf.setBoolean(ShuffleServerConf.RSS_TEST_MODE_ENABLE, true);
    nettyShuffleServerConf.setString("rss.storage.basePath", nettyBasePath);
    storeShuffleServerConf(nettyShuffleServerConf);

    startServersWithRandomPorts();
  }

  @Override
  public void updateRssStorage(SparkConf sparkConf) {}

  /**
   * Test different compression types with localfile rss mode.
   *
   * @throws Exception
   */
  @Override
  public void run() throws Exception {
    String fileName = generateTestFile();
    SparkConf sparkConf = createSparkConf();
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

    List<Map> results = new ArrayList<>();
    Map resultWithoutRss = runSparkApp(sparkConf, fileName);
    results.add(resultWithoutRss);

    updateSparkConfWithRssGrpc(sparkConf);
    updateSparkConfCustomer(sparkConf);
    for (Codec.Type type : new Codec.Type[] {Codec.Type.NOOP, Codec.Type.ZSTD, Codec.Type.LZ4}) {
      sparkConf.set("spark." + COMPRESSION_TYPE.key().toLowerCase(), type.name());
      Map resultWithRss = runSparkApp(sparkConf, fileName);
      results.add(resultWithRss);
    }
    updateSparkConfWithRssNetty(sparkConf);
    for (Codec.Type type : new Codec.Type[] {Codec.Type.NOOP, Codec.Type.ZSTD, Codec.Type.LZ4}) {
      sparkConf.set("spark." + COMPRESSION_TYPE.key().toLowerCase(), type.name());
      Map resultWithRss = runSparkApp(sparkConf, fileName);
      results.add(resultWithRss);
    }

    for (int i = 1; i < results.size(); i++) {
      verifyTestResult(results.get(0), results.get(i));
    }
  }
}
