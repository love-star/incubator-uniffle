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

package org.apache.uniffle.common.segment;

import java.util.List;

import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;

public class FixedSizeSegmentSplitter extends AbstractSegmentSplitter {
  public FixedSizeSegmentSplitter(int readBufferSize) {
    super(readBufferSize);
  }

  @Override
  public List<ShuffleDataSegment> split(ShuffleIndexResult shuffleIndexResult) {
    // For FixedSizeSegmentSplitter, we do not filter by taskAttemptId, so pass null for the filter.
    return splitCommon(shuffleIndexResult, null);
  }
}
