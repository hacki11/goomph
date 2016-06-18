/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.Maps;
import com.diffplug.common.io.Files;

/** Miscellaneous utilties for copying files around. */
public class FileMisc {
	/** Enforces unix newlines on the given string. */
	public static String toUnixNewline(String input) {
		return input.replace("\r\n", "\n");
	}

	/**
	 * Copies from src to dst and performs a simple
	 * copy-replace templating operation along the way.
	 * 
	 * ```java
	 * copyFile(src, dst,
	 *     "%username%", "lskywalker"
	 *     "%firstname%", "Luke",
	 *     "%lastname%", "Skywalker");
	 * ```
	 */
	public static void copyFile(File srcFile, File dstFile, String... toReplace) throws IOException {
		// make a map of the keys that we're replacing
		Preconditions.checkArgument(toReplace.length % 2 == 0);
		Map<String, String> replaceMap = Maps.newHashMap();
		for (int i = 0; i < toReplace.length / 2; ++i) {
			replaceMap.put(toReplace[2 * i], toReplace[2 * i + 1]);
		}
		// replace them
		String content = Joiner.on("\n").join(Files.readLines(srcFile, StandardCharsets.UTF_8));
		for (Entry<String, String> entry : replaceMap.entrySet()) {
			content = content.replace(entry.getKey(), entry.getValue());
		}
		// write it out
		dstFile.getParentFile().mkdirs();
		Files.write(content.getBytes(StandardCharsets.UTF_8), dstFile);
	}

	/** Deletes the given file or directory if it exists, then creates a fresh directory in its place. */
	public static void cleanDir(File dirToRemove) throws IOException {
		if (dirToRemove.isFile()) {
			dirToRemove.delete();
		} else if (dirToRemove.isDirectory()) {
			try {
				FileUtils.deleteDirectory(dirToRemove);
			} catch (IOException e) {
				// we couldn't delete the directory,
				// but deleting everything inside is just as good
				for (File file : dirToRemove.listFiles()) {
					if (file.isFile()) {
						file.delete();
					} else {
						FileUtils.deleteDirectory(file);
					}
				}
			}
		}
		dirToRemove.mkdirs();
	}

	/**
	 * Flattens a single directory (moves its children to be its peers, then deletes the given directory.
	 * 
	 * ```
	 * before:
	 *     root/
	 *        toFlatten/
	 *           child1
	 *           child2
	 * 
	 * flatten("root/toFlatten")
	 * 
	 * after:
	 *     root/
	 *        child1
	 *        child2
	 * ```
	 */
	public static void flatten(File dirToRemove) throws IOException {
		final File parent = dirToRemove.getParentFile();
		// move each child directory to the parent
		for (File child : dirToRemove.listFiles()) {
			boolean createDestDir = false;
			if (child.isFile()) {
				FileUtils.moveFileToDirectory(child, parent, createDestDir);
			} else if (child.isDirectory()) {
				FileUtils.moveDirectoryToDirectory(child, parent, createDestDir);
			} else {
				throw new IllegalArgumentException("Unknown filetype: " + child);
			}
		}
		// remove the directory which we're flattening away
		dirToRemove.delete();
	}

	/** Concats the first files and writes them to the last file. */
	public static void concat(Iterable<File> toMerge, File dst) throws IOException {
		try (FileChannel dstChannel = FileChannel.open(dst.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			for (File file : toMerge) {
				try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
					FileChannel channel = raf.getChannel();
					dstChannel.write(channel.map(FileChannel.MapMode.READ_ONLY, 0, raf.length()));
				}
			}
		}
	}

	/** Permission bits. */
	private static final int OWNER_READ_FILEMODE = 0400;
	private static final int OWNER_WRITE_FILEMODE = 0200;
	private static final int OWNER_EXEC_FILEMODE = 0100;
	private static final int GROUP_READ_FILEMODE = 0040;
	private static final int GROUP_WRITE_FILEMODE = 0020;
	private static final int GROUP_EXEC_FILEMODE = 0010;
	private static final int OTHERS_READ_FILEMODE = 0004;
	private static final int OTHERS_WRITE_FILEMODE = 0002;
	private static final int OTHERS_EXEC_FILEMODE = 0001;

	/** Converts a set of {@link PosixFilePermission} to chmod-style octal file mode. */
	public static int toOctalFileModeInt(Set<PosixFilePermission> permissions) {
		int result = 0;
		for (PosixFilePermission permissionBit : permissions) {
			switch (permissionBit) {
			case OWNER_READ:
				result |= OWNER_READ_FILEMODE;
				break;
			case OWNER_WRITE:
				result |= OWNER_WRITE_FILEMODE;
				break;
			case OWNER_EXECUTE:
				result |= OWNER_EXEC_FILEMODE;
				break;
			case GROUP_READ:
				result |= GROUP_READ_FILEMODE;
				break;
			case GROUP_WRITE:
				result |= GROUP_WRITE_FILEMODE;
				break;
			case GROUP_EXECUTE:
				result |= GROUP_EXEC_FILEMODE;
				break;
			case OTHERS_READ:
				result |= OTHERS_READ_FILEMODE;
				break;
			case OTHERS_WRITE:
				result |= OTHERS_WRITE_FILEMODE;
				break;
			case OTHERS_EXECUTE:
				result |= OTHERS_EXEC_FILEMODE;
				break;
			}
		}
		return result;
	}

	/** Converts a set of {@link PosixFilePermission} to chmod-style octal file mode. */
	public static String toOctalFileMode(Set<PosixFilePermission> permissions) {
		int value = toOctalFileModeInt(permissions);
		return Integer.toOctalString(value);
	}

	/** Returns true if any of the bits contain the executable permission. */
	public static boolean containsExecutablePermission(Set<PosixFilePermission> permissions) {
		return permissions.contains(PosixFilePermission.OWNER_EXECUTE) &&
				permissions.contains(PosixFilePermission.GROUP_EXECUTE) &&
				permissions.contains(PosixFilePermission.OTHERS_EXECUTE);
	}

	/** Writes a file with the given name, to the given directory, containing the given value. */
	public static void writeToken(File dir, String name, String value) throws IOException {
		Preconditions.checkArgument(dir.isDirectory());
		File token = new File(dir, name);
		FileUtils.write(token, value, StandardCharsets.UTF_8);
	}

	/** Returns the contents of a file with the given name, if it exists. */
	public static Optional<String> readToken(File dir, String name) throws IOException {
		File token = new File(dir, name);
		if (!token.isFile()) {
			return Optional.empty();
		} else {
			return Optional.of(FileUtils.readFileToString(token, StandardCharsets.UTF_8));
		}
	}

	/** Writes an empty file with the given name in the given directory. */
	public static void writeToken(File dir, String name) throws IOException {
		writeToken(dir, name, "");
	}

	/** Returns true iff the given directory has a file with the given name. */
	public static boolean hasToken(File dir, String name) throws IOException {
		return readToken(dir, name).isPresent();
	}

	/** Quotes the given input string iff it contains whitespace. */
	public static String quote(String input) {
		if (input.contains(" ")) {
			return "\"" + input + "\"";
		} else {
			return input;
		}
	}

	/** Quotes the absolute path of the given file iff it contains whitespace. */
	public static String quote(File input) {
		return quote(input.getAbsolutePath());
	}
}
