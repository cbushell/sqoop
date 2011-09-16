/**
 * Copyright 2011 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.sqoop.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Runs a process via Runtime.exec() and allows handling of stdout/stderr to be
 * deferred to other threads.
 *
 */
public final class Executor {

  public static final Log LOG = LogFactory.getLog(Executor.class.getName());

  private Executor() {
  }

  /**
   * Execute a program defined by the args array with default stream sinks
   * that consume the program's output (to prevent it from blocking on buffers)
   * and then ignore said output.
   */
  public static int exec(String [] args) throws IOException {
    NullAsyncSink s = new NullAsyncSink();
    return exec(args, s, s);
  }

  /**
   * Run a command via Runtime.exec(), with its stdout and stderr streams
   * directed to be handled by threads generated by AsyncSinks.
   * Block until the child process terminates.
   *
   * @return the exit status of the ran program
   */
  public static int exec(String [] args, AsyncSink outSink,
      AsyncSink errSink) throws IOException {
    return exec(args, null, outSink, errSink);
  }


  /**
   * Run a command via Runtime.exec(), with its stdout and stderr streams
   * directed to be handled by threads generated by AsyncSinks.
   * Block until the child process terminates. Allows the programmer to
   * specify an environment for the child program.
   *
   * @return the exit status of the ran program
   */
  public static int exec(String [] args, String [] envp, AsyncSink outSink,
      AsyncSink errSink) throws IOException {

    // launch the process.
    Process p = Runtime.getRuntime().exec(args, envp);

    // dispatch its stdout and stderr to stream sinks if available.
    if (null != outSink) {
      outSink.processStream(p.getInputStream());
    }

    if (null != errSink) {
      errSink.processStream(p.getErrorStream());
    }

    // wait for the return value.
    while (true) {
      try {
        int ret = p.waitFor();
        return ret;
      } catch (InterruptedException ie) {
        continue;
      }
    }
  }


  /**
   * @return An array formatted correctly for use as an envp based on the
   * current environment for this program.
   */
  public static List<String> getCurEnvpStrings() {
    Map<String, String> curEnv = System.getenv();
    ArrayList<String> array = new ArrayList<String>();

    if (null == curEnv) {
      return null;
    }

    for (Map.Entry<String, String> entry : curEnv.entrySet()) {
      array.add(entry.getKey() + "=" + entry.getValue());
    }

    return array;
  }
}
