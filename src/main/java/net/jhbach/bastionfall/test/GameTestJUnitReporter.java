package net.jhbach.bastionfall.test;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GameTestJUnitReporter {

	private static final Path OUTPUT_PATH = Paths.get("build", "test-results", "gametest", "TEST-ForgeGameTests.xml");
	private static final List<TestResult> results = new ArrayList<>();

	static {
		// Reset the file on first load
		try {
			Files.createDirectories(OUTPUT_PATH.getParent());
			Files.deleteIfExists(OUTPUT_PATH);
		} catch (IOException e) {
			System.err.println("[GameTest] Could not prepare JUnit report file: " + e.getMessage());
		}
	}

	public static void init() {
		results.clear();

		try {
			Files.createDirectories(OUTPUT_PATH.getParent());
			Files.deleteIfExists(OUTPUT_PATH);
		} catch (IOException e) {
			System.err.println("[GameTestJUnitReporter] Failed to initialize report file: " + e.getMessage());
		}
	}

	public static void recordPass(String testName) {
		results.add(new TestResult(testName, true, null));
	}

	public static void recordFail(String testName, String message) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		String location = "";

		for (StackTraceElement elem : stack) {
			String className = elem.getClassName();
			if (!className.contains("GameTestJUnitReporter")
					&& !className.contains("GameTestHelper")
					&& className.endsWith("Test")) { // customizable condition
				location = elem.getFileName() + ":" + elem.getLineNumber();
				break;
			}
		}

		if (location.isEmpty() && stack.length > 2) {
			StackTraceElement fallback = stack[2];
			location = fallback.getFileName() + ":" + fallback.getLineNumber();
		}

		results.add(new TestResult(testName, false, message + " (" + location + ")"));
	}

	public static void writeReport() {
		try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_PATH)) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<testsuite name=\"ForgeGameTests\" tests=\"" + results.size() + "\" failures=\"" +
					results.stream().filter(r -> !r.passed).count() + "\">\n");

			for (TestResult result : results) {
				System.out.println(result);
				writer.write("  <testcase classname=\"ForgeGameTests\" name=\"" + result.name + "\">\n");
				if (!result.passed) {
					writer.write("    <failure message=\"" + escape(result.message) + "\"/>\n");
				}
				writer.write("  </testcase>\n");
			}

			writer.write("</testsuite>\n");
		} catch (IOException e) {
			System.err.println("[GameTest] Failed to write JUnit report: " + e.getMessage());
		}
	}

	private static String escape(String input) {
		return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static class TestResult {
		final String name;
		final boolean passed;
		final String message;

		TestResult(String name, boolean passed, String message) {
			this.name = name;
			this.passed = passed;
			this.message = message;
		}
	}
}

