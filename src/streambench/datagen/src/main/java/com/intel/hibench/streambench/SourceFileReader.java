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

package com.intel.hibench.streambench;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.util.Vector;

public class SourceFileReader {

  // get file reader according to given path and offset.
  static public BufferedReader getReader (Configuration dfsConf, String path, long offset) {
    BufferedReader reader = null;
    try {
      Path pt = new Path(path);
      FileSystem fs = FileSystem.get(dfsConf);
      InputStreamReader isr;
      if (fs.isDirectory(pt)) {
        //give path is an directory
        isr = new InputStreamReader(openMultipleParts(fs, pt, offset));
      } else {
        //give path is an file
        FSDataInputStream inputStream = fs.open(pt);
        if (offset > 0) {
          inputStream.seek(offset);
        }
        isr = new InputStreamReader(inputStream);
      }

      reader = new BufferedReader(isr);
    } catch (IOException e) {
      System.err.println("Fail to get reader from path: " + path);
      e.printStackTrace();
    }
    return reader;
  }

  // Open all files start with "part-", those files are generated by genSeedDataset.sh
  static private InputStream openMultipleParts (
      FileSystem fs, Path pt, long offset) throws IOException {

    System.out.println("opening all parts in path: " + pt + ", from offset: " + offset );
    // list all files in given path
    RemoteIterator<LocatedFileStatus> rit = fs.listFiles(pt, false);
    Vector<FSDataInputStream> fileHandleList = new Vector<FSDataInputStream>();
    while (rit.hasNext()) {
      Path path = rit.next().getPath();

      // Only read those files start with "part-"
      if (path.getName().startsWith("part-")) {
        long fileSize = fs.getFileStatus(path).getLen();
        if (offset < fileSize) {
          FSDataInputStream inputStream = fs.open(path);
          if (offset > 0) {
            inputStream.seek(offset);
          }
          fileHandleList.add(inputStream);
        }
        offset -= fileSize;
      }
    }

    if (!fileHandleList.isEmpty()) {
      return new SequenceInputStream(fileHandleList.elements());
    } else {
      System.err.println("Error, no source file loaded. run genSeedDataset.sh first!");
      return null;
    }

  }
}