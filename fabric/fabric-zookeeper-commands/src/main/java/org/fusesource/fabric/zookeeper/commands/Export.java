/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.zookeeper.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static org.fusesource.fabric.zookeeper.commands.RegexSupport.getPatterns;
import static org.fusesource.fabric.zookeeper.commands.RegexSupport.matches;
import static org.fusesource.fabric.zookeeper.commands.RegexSupport.merge;

@Command(name = "export", scope = "zk", description = "Export data from zookeeper")
public class Export extends ZooKeeperCommandSupport {

    @Argument(description="path of the directory to export to")
    String target = "." + File.separator + "export";

    @Option(name="-f", aliases={"--regex"}, description="regex to filter on what paths to export, can specify this option more than once for additional filters", multiValued=true)
    String regex[];

    @Option(name="-rf", aliases={"--reverse-regex"}, description="regex to filter what paths to exclude from the export, can specify this option more than once for additional filters", multiValued=true)
    String nregex[];

    @Option(name="-p", aliases={"--path"}, description="Top level context to export")
    String topLevel = "/";

    @Option(name="-d", aliases={"--delete"}, description="Clear target directory before exporting (CAUTION! Performs recursive delete!)")
    boolean delete;

    @Option(name="-t", aliases={"--trim"}, description="Trims the first timestamp comment line in properties files starting with the '#' character")
    boolean trimHeader;

    @Option(name="--dry-run", description="Runs the export but instead prints out what's going to happen rather than performing the action")
    boolean dryRun = false;

    File ignore = new File(".fabricignore");
    File include = new File(".fabricinclude");

    @Override
    protected Object doExecute() throws Exception {
        if (ignore.exists() && ignore.isFile()) {
            nregex = merge(ignore, nregex);
        }
        if (include.exists() && include.isFile()) {
            regex = merge(include, regex);
        }
        export(topLevel);
        System.out.printf("Export to %s completed successfully\n", target);
        return null;
    }

    private void delete(File parent) throws Exception {
        if (!parent.exists()) {
            return;
        }
        if (parent.isDirectory()) {
            for (File f : parent.listFiles()) {
                delete(f);
            }
        }
        parent.delete();
    }

    protected void export(String path) throws Exception {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        List<Pattern> include = getPatterns(regex);
        List<Pattern> exclude = getPatterns(nregex);
        List<String> paths = getZooKeeper().getAllChildren(path);
        SortedSet<File> directories = new TreeSet<File>();
        Map<File, String> settings = new HashMap<File, String>();

        for(String p : paths) {
            p = path + p;
            if (!matches(include, p, true) || matches(exclude, p, false)) {
                continue;
            }
            byte[] data = getZooKeeper().getData(p);
            if (data != null) {
                String name = p;
                if (!p.contains(".")) {
                    name += ".cfg";
                }
                String value = new String(data);
                if (trimHeader && value.startsWith("#")) {
                    // lets remove the first line
                    int idx = value.indexOf("\n");
                    if (idx > 0) {
                        value = value.substring(idx + 1);
                    }
                }
                settings.put(new File(target + File.separator + name), value);
            } else {
                directories.add(new File(target + File.separator + p));
            }
        }

        if (delete) {
            if (!dryRun) {
                delete(new File(target));
            } else {
                System.out.printf("Deleting %s and everything under it\n", new File(target));
            }
        }

        for (File d : directories) {
            if (d.exists() && !d.isDirectory()) {
                throw new IllegalArgumentException("Directory " + d + " exists but is not a directory");
            }
            if (!d.exists()) {
                if (!dryRun) {
                    if (!d.mkdirs()) {
                        throw new RuntimeException("Failed to create directory " + d);
                    }
                } else {
                    System.out.printf("Creating directory path : %s\n", d);
                }
            }
        }
        for (File f : settings.keySet()) {
            if (f.exists() && !f.isFile()) {
                throw new IllegalArgumentException("File " + f + " exists but is not a file");
            }
            if (!f.getParentFile().exists()) {
                if (!dryRun) {
                    if (!f.getParentFile().mkdirs()) {
                        throw new RuntimeException("Failed to create directory " + f.getParentFile());
                    }
                } else {
                    System.out.printf("Creating directory path : %s\n", f);
                }
            }
            if (!f.exists()) {
                try {
                    if (!dryRun) {
                        if (!f.createNewFile()) {
                            throw new RuntimeException("Failed to create file " + f);
                        }
                    } else {
                        System.out.printf("Creating file : %s\n", f);
                    }
                } catch (IOException io) {
                    throw new RuntimeException("Failed to create file " + f + " : " + io);
                }
            }
            if (!dryRun) {
                FileWriter writer = new FileWriter(f, false);
                writer.write(settings.get(f));
                writer.close();
            } else {
                System.out.printf("Writing value \"%s\" to file : %s\n", settings.get(f), f);
            }
        }
    }
}
