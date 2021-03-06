package tr.com.srdc.cda2fhir.jolt;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;

public class IDTest {
	@Test
	public void testContained() throws Exception {
		File file = new File("src/test/resources/jolt-verify/data-type/ID-identifier.json");
		String content = FileUtils.readFileToString(file, Charset.defaultCharset());
		JSONArray testCases = new JSONArray(content);
		for (int index = 0; index < testCases.length(); ++index) {
			JSONObject testCase = testCases.getJSONObject(index);
			JSONObject inputJSON = testCase.getJSONObject("input");
			JSONArray templateJSON = testCase.getJSONArray("template");
			JSONObject expectedJSON = testCase.getJSONObject("expected");

			String input = inputJSON.toString();
			Object inputObject = JsonUtils.jsonToObject(input);

			String template = templateJSON.toString();
			List<Object> chainrSpecJSON = JsonUtils.jsonToList(template);
			Chainr chainr = Chainr.fromSpec(chainrSpecJSON);

			Object actualObject = chainr.transform(inputObject);
			String actual = JsonUtils.toPrettyJsonString(actualObject);

			String expected = expectedJSON.toString();
			JSONAssert.assertEquals("Test Case " + index, expected, actual, false);
		}
	}
}
