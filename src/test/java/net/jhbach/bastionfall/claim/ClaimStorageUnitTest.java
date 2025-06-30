package net.jhbach.bastionfall.claim;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class ClaimStorageUnitTest {

	@TestFactory
	Collection<DynamicTest> testForgeGameTestResults() throws Exception {
		// Step 1: Run the Forge GameTests
		String gradlew = System.getProperty("os.name").toLowerCase().contains("win")
				? "gradlew.bat"
				: "./gradlew";

		Process p = new ProcessBuilder(gradlew, "runGameTestServer")
				.directory(new File("."))
				.redirectErrorStream(true)
				.start();

		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line;
			while ((line = r.readLine()) != null) {
				System.out.println("[ForgeGameTest] " + line);
			}
		}
		int exitCode = p.waitFor();

		// Step 2: Parse XML output
		Path xmlPath = Paths.get("run/build/test-results/gametest/TEST-ForgeGameTests.xml");
		assertTrue(Files.exists(xmlPath), "GameTest result file not found");

		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder()
				.parse(Files.newInputStream(xmlPath));

		NodeList testcases = doc.getElementsByTagName("testcase");

		Collection<DynamicTest> dynamicTests = new ArrayList<>();
		for (int i = 0; i < testcases.getLength(); i++) {
			Element testcase = (Element) testcases.item(i);
			String testName = testcase.getAttribute("name");
			NodeList failures = testcase.getElementsByTagName("failure");

			dynamicTests.add(DynamicTest.dynamicTest(testName, () -> {
				if (failures.getLength() > 0) {
					String message = failures.item(0).getAttributes().getNamedItem("message").getNodeValue();
					fail("Forge GameTest '" + testName + "' failed: " + message);
				}
			}));
		}

		return dynamicTests;
	}

}
