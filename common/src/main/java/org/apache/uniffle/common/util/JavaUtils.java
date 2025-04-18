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

package org.apache.uniffle.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.common.base.Enums;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaUtils {
  private static final Logger logger = LoggerFactory.getLogger(JavaUtils.class);
  private static final String JAVA_9 = "JAVA_9";

  public static boolean isJavaVersionAtLeastJava9() {
    return Enums.getIfPresent(JavaVersion.class, JAVA_9).isPresent()
        && SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9);
  }

  /** Closes the given object, ignoring IOExceptions. */
  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException e) {
      logger.error("IOException should not have been thrown.", e);
    }
  }

  public static <K, V> ConcurrentHashMap<K, V> newConcurrentMap() {
    if (isJavaVersionAtLeastJava9()) {
      return new ConcurrentHashMap<>();
    } else {
      return new ConcurrentHashMapForJDK8<>();
    }
  }

  public static <K, V> ConcurrentHashMap<K, V> newConcurrentMap(Map<? extends K, ? extends V> m) {
    if (isJavaVersionAtLeastJava9()) {
      return new ConcurrentHashMap<>(m);
    } else {
      return new ConcurrentHashMapForJDK8<>(m);
    }
  }

  /**
   * For JDK8, there is bug for ConcurrentHashMap#computeIfAbsent, checking the key existence to
   * speed up. See details in issue #519
   */
  private static class ConcurrentHashMapForJDK8<K, V> extends ConcurrentHashMap<K, V> {
    ConcurrentHashMapForJDK8() {
      super();
    }

    ConcurrentHashMapForJDK8(Map<? extends K, ? extends V> m) {
      super(m);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
      V result;
      if (null == (result = get(key))) {
        result = super.computeIfAbsent(key, mappingFunction);
      }
      return result;
    }
  }
}
