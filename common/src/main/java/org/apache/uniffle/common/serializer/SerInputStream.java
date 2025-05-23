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

package org.apache.uniffle.common.serializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import io.netty.buffer.ByteBuf;

public abstract class SerInputStream extends InputStream {

  public void init() {}

  @Override
  public abstract int available();

  public abstract long getStart();

  public abstract long getEnd();

  public abstract void transferTo(ByteBuf to, int len) throws IOException;

  public static SerInputStream newInputStream(File file) throws IOException {
    return SerInputStream.newInputStream(file, 0, file.length());
  }

  public static SerInputStream newInputStream(File file, long start) throws IOException {
    return SerInputStream.newInputStream(file, start, file.length());
  }

  public static SerInputStream newInputStream(File file, long start, long end) throws IOException {
    return new FileSerInputStream(file, start, Math.min(end, file.length()));
  }

  public static SerInputStream newInputStream(ByteBuf byteBuf) {
    return SerInputStream.newInputStream(byteBuf, 0, byteBuf.writerIndex());
  }

  public static SerInputStream newInputStream(ByteBuf byteBuf, int start) {
    return SerInputStream.newInputStream(byteBuf, start, byteBuf.writerIndex());
  }

  public static SerInputStream newInputStream(ByteBuf byteBuf, int start, int end) {
    return new BufferSerInputStream(byteBuf, start, Math.min(byteBuf.writerIndex(), end));
  }
}
