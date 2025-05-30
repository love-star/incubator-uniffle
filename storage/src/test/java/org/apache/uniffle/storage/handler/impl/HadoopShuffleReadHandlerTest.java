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

package org.apache.uniffle.storage.handler.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.util.BlockIdLayout;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.storage.HadoopShuffleHandlerTestBase;
import org.apache.uniffle.storage.HadoopTestBase;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HadoopShuffleReadHandlerTest extends HadoopTestBase {

  public static void createAndRunCases(String clusterPathPrefix, Configuration conf, String user)
      throws Exception {
    String basePath = clusterPathPrefix + "HdfsShuffleFileReadHandlerTest";
    HadoopShuffleWriteHandler writeHandler =
        new HadoopShuffleWriteHandler("appId", 0, 1, 1, basePath, "test", conf, user);

    Map<Long, byte[]> expectedData = Maps.newHashMap();

    int readBufferSize = 13;
    int totalBlockNum = 0;
    int expectTotalBlockNum = new Random().nextInt(37);
    int blockSize = new Random().nextInt(7) + 1;
    HadoopShuffleHandlerTestBase.writeTestData(
        writeHandler, expectTotalBlockNum, blockSize, 0, expectedData);
    int total =
        HadoopShuffleHandlerTestBase.calcExpectedSegmentNum(
            expectTotalBlockNum, blockSize, readBufferSize);
    Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
    Set<Long> processBlockIds = ConcurrentHashMap.newKeySet();
    expectedData.forEach((id, block) -> expectBlockIds.addLong(id));
    String fileNamePrefix =
        ShuffleStorageUtils.getFullShuffleDataFolder(
                basePath, ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 1, 1, 10))
            + "/test_0";
    HadoopShuffleReadHandler handler =
        new HadoopShuffleReadHandler(
            "appId", 0, 1, fileNamePrefix, readBufferSize, expectBlockIds, processBlockIds, conf);

    Set<Long> actualBlockIds = Sets.newHashSet();
    for (int i = 0; i < total; ++i) {
      ShuffleDataResult shuffleDataResult = handler.readShuffleData();
      totalBlockNum += shuffleDataResult.getBufferSegments().size();
      HadoopShuffleHandlerTestBase.checkData(shuffleDataResult, expectedData);
      for (BufferSegment bufferSegment : shuffleDataResult.getBufferSegments()) {
        actualBlockIds.add(bufferSegment.getBlockId());
      }
    }

    assertNull(handler.readShuffleData());
    assertEquals(total, handler.getShuffleDataSegments().size());
    assertEquals(expectTotalBlockNum, totalBlockNum);
    assertEquals(expectedData.keySet(), actualBlockIds);
  }

  @Test
  public void test() throws Exception {
    createAndRunCases(HDFS_URI, conf, StringUtils.EMPTY);
  }

  @Test
  public void testDataInconsistent() throws Exception {
    String basePath = HDFS_URI + "HdfsShuffleFileReadHandlerTest#testDataInconsistent";
    TestHadoopShuffleWriteHandler writeHandler =
        new TestHadoopShuffleWriteHandler(
            "appId", 0, 1, 1, basePath, "test", conf, StringUtils.EMPTY);

    Map<Long, byte[]> expectedData = Maps.newHashMap();
    int totalBlockNum = 0;
    int expectTotalBlockNum = 6;
    int blockSize = 7;
    long taskAttemptId = 0;

    // write expectTotalBlockNum - 1 complete block
    HadoopShuffleHandlerTestBase.writeTestData(
        writeHandler, expectTotalBlockNum - 1, blockSize, taskAttemptId, expectedData);

    // write 1 incomplete block , which only write index file
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    byte[] buf = new byte[blockSize];
    new Random().nextBytes(buf);
    BlockIdLayout layout = BlockIdLayout.DEFAULT;
    long blockId = layout.getBlockId(expectTotalBlockNum, 0, taskAttemptId);
    blocks.add(
        new ShufflePartitionedBlock(
            blockSize, blockSize, ChecksumUtils.getCrc32(buf), blockId, taskAttemptId, buf));
    writeHandler.writeIndex(blocks);

    int readBufferSize = 13;
    int total =
        HadoopShuffleHandlerTestBase.calcExpectedSegmentNum(
            expectTotalBlockNum, blockSize, readBufferSize);
    Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
    Set<Long> processBlockIds = ConcurrentHashMap.newKeySet();
    expectedData.forEach((id, block) -> expectBlockIds.addLong(id));
    String fileNamePrefix =
        ShuffleStorageUtils.getFullShuffleDataFolder(
                basePath, ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 1, 1, 10))
            + "/test_0";
    HadoopShuffleReadHandler handler =
        new HadoopShuffleReadHandler(
            "appId", 0, 1, fileNamePrefix, readBufferSize, expectBlockIds, processBlockIds, conf);

    Set<Long> actualBlockIds = Sets.newHashSet();
    for (int i = 0; i < total; ++i) {
      ShuffleDataResult shuffleDataResult = handler.readShuffleData();
      totalBlockNum += shuffleDataResult.getBufferSegments().size();
      HadoopShuffleHandlerTestBase.checkData(shuffleDataResult, expectedData);
      for (BufferSegment bufferSegment : shuffleDataResult.getBufferSegments()) {
        actualBlockIds.add(bufferSegment.getBlockId());
      }
    }

    assertNull(handler.readShuffleData());
    assertEquals(total, handler.getShuffleDataSegments().size());
    // The last block cannot be read, only the index is generated
    assertEquals(expectTotalBlockNum - 1, totalBlockNum);
    assertEquals(expectedData.keySet(), actualBlockIds);
  }

  static class TestHadoopShuffleWriteHandler extends HadoopShuffleWriteHandler {

    private Configuration hadoopConf;
    private Lock writeLock = new ReentrantLock();
    private String basePath;
    private String fileNamePrefix;
    private int failTimes = 0;

    TestHadoopShuffleWriteHandler(
        String appId,
        int shuffleId,
        int startPartition,
        int endPartition,
        String storageBasePath,
        String fileNamePrefix,
        Configuration hadoopConf,
        String user)
        throws Exception {
      super(
          appId,
          shuffleId,
          startPartition,
          endPartition,
          storageBasePath,
          fileNamePrefix,
          hadoopConf,
          user);
      this.hadoopConf = hadoopConf;
      this.fileNamePrefix = fileNamePrefix;
      this.basePath =
          ShuffleStorageUtils.getFullShuffleDataFolder(
              storageBasePath,
              ShuffleStorageUtils.getShuffleDataPath(
                  appId, shuffleId, startPartition, endPartition));
    }

    // only write index file
    public void writeIndex(List<ShufflePartitionedBlock> shuffleBlocks)
        throws IOException, IllegalStateException {
      HadoopFileWriter indexWriter = null;
      writeLock.lock();
      try {
        try {
          String indexFileName =
              ShuffleStorageUtils.generateIndexFileName(fileNamePrefix + "_" + failTimes);
          indexWriter = createWriter(indexFileName);
          for (ShufflePartitionedBlock block : shuffleBlocks) {
            long blockId = block.getBlockId();
            long crc = block.getCrc();
            long startOffset = indexWriter.nextOffset();

            FileBasedShuffleSegment segment =
                new FileBasedShuffleSegment(
                    blockId,
                    startOffset,
                    block.getDataLength(),
                    block.getUncompressLength(),
                    crc,
                    block.getTaskAttemptId());
            indexWriter.writeIndex(segment);
          }
        } catch (Exception e) {
          failTimes++;
          throw new RuntimeException(e);
        } finally {
          if (indexWriter != null) {
            indexWriter.close();
          }
        }
      } finally {
        writeLock.unlock();
      }
    }
  }
}
