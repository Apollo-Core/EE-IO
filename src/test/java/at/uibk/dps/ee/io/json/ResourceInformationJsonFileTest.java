package at.uibk.dps.ee.io.json;

import static org.junit.Assert.*;

import org.junit.Test;

import at.uibk.dps.ee.io.testconstants.ConstantsTestCoreEEiO;

public class ResourceInformationJsonFileTest {

	@Test
	public void testReadFromFile() {
		String filePath = ConstantsTestCoreEEiO.resourceTestInputPath;
		ResourceInformationJsonFile result = ResourceInformationJsonFile.readFromFile(filePath);
		assertEquals(2, result.size());

		FunctionTypeEntry entry = result.get(0);
		assertEquals("addition", entry.getFunctionType());
		assertEquals(4, entry.getResources().size());

		ResourceEntry resEntry = entry.getResources().get(0);
		assertEquals("Serverless", resEntry.getType());
		assertEquals(5, resEntry.getProperties().size());
		assertEquals("US", resEntry.getProperties().get("region").getAsString());
	}

}
