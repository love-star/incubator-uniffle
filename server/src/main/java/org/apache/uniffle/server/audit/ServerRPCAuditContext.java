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

package org.apache.uniffle.server.audit;

import org.slf4j.Logger;

import org.apache.uniffle.common.audit.AuditContext;
import org.apache.uniffle.common.rpc.StatusCode;

/** An audit context for shuffle server rpc. */
public class ServerRPCAuditContext implements AuditContext {
  private final Logger log;
  private String command;
  private String statusCode;
  private long creationTimeNs;
  private long executionTimeNs;
  private String appId = "N/A";
  private int shuffleId = -1;
  private String args;
  private String returnValue;

  /**
   * Constructor of {@link ServerRPCAuditContext}.
   *
   * @param log the logger to log the audit information
   */
  public ServerRPCAuditContext(Logger log) {
    this.log = log;
  }

  /**
   * Sets mCommand field.
   *
   * @param command the command associated with shuffle server rpc
   * @return this {@link AuditContext} instance
   */
  public ServerRPCAuditContext withCommand(String command) {
    this.command = command;
    return this;
  }

  /**
   * Sets creationTimeNs field.
   *
   * @param creationTimeNs the System.nanoTime() when this operation create, it only can be used to
   *     compute operation mExecutionTime
   * @return this {@link AuditContext} instance
   */
  public ServerRPCAuditContext withCreationTimeNs(long creationTimeNs) {
    this.creationTimeNs = creationTimeNs;
    return this;
  }

  /**
   * Sets status code field.
   *
   * @param statusCode the status code
   * @return this {@link AuditContext} instance
   */
  public ServerRPCAuditContext withStatusCode(StatusCode statusCode) {
    this.statusCode = statusCode.name();
    return this;
  }

  /**
   * Sets status code field.
   *
   * @param statusCode the status code
   * @return this {@link AuditContext} instance
   */
  public ServerRPCAuditContext withStatusCode(String statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  @Override
  public void close() {
    if (log == null) {
      return;
    }
    executionTimeNs = System.nanoTime() - creationTimeNs;
    log.info(toString());
  }

  @Override
  public String toString() {
    String line =
        String.format(
            "cmd=%s\tstatusCode=%s\tappId=%s\tshuffleId=%s\texecutionTimeUs=%d",
            command, statusCode, appId, shuffleId, executionTimeNs / 1000);
    if (args != null) {
      line += String.format("\targs{%s}", args);
    }
    if (returnValue != null) {
      line += String.format("\treturn{%s}", returnValue);
    }
    return line;
  }

  public ServerRPCAuditContext withAppId(String appId) {
    this.appId = appId;
    return this;
  }

  public ServerRPCAuditContext withShuffleId(int shuffleId) {
    this.shuffleId = shuffleId;
    return this;
  }

  public ServerRPCAuditContext withArgs(String args) {
    this.args = args;
    return this;
  }

  public ServerRPCAuditContext withReturnValue(String returnValue) {
    this.returnValue = returnValue;
    return this;
  }
}
