package tr.com.srdc.cda2fhir.jolt.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoltFormat {
	private Map<String, String> map = new HashMap<String, String>();
	
	public String get(String target) {
		return map.get(target);
	}
	
	@Override
	public JoltFormat clone() {
		JoltFormat formatClone = new JoltFormat();
		formatClone.map.putAll(map);
		return formatClone;
	}
	
	public void putAllAsPromoted(JoltFormat source, String target) {
		source.map.forEach((key, format) -> {
			if (target.isEmpty()) {
				map.put(key, format);
			} else {
				String promotedKey = String.format("%s.%s", target, key);
				map.put(promotedKey, format);
			}
		});
	}
	
	private static String formatValue(String value) {
		String result = value.replace(",@0", "").replace("(@0)", "");
		return result.substring(1);
	}
	
	@SuppressWarnings("unchecked")
	private static void fillResult(JoltFormat result, Map<String, Object> map, String parentPath) {	
		for (Map.Entry<String, Object> entry: map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				throw new ReportException("There must be a function for each entry in format specification");
			}			
			if (value instanceof Map) {
				Map<String, Object> valueAsMap = (Map<String, Object>) value;
				Map.Entry<String, Object> valueEntry = valueAsMap.entrySet().stream().findFirst().get();
				String newPath = key;
				String valueKey = valueEntry.getKey();
				if (valueKey.equals("*")) {
					newPath += "[]";
				} else {
					newPath += "." + valueKey;
				}
				if (parentPath.length() > 0) {
					newPath = parentPath + "." + newPath;
				}
				Object valueEntryValue = valueEntry.getValue();
				if (valueEntryValue instanceof String) {
					result.map.put(newPath, formatValue((String) valueEntryValue));
					continue;
				}
				fillResult(result, (Map<String, Object>) valueEntryValue, newPath);
				continue;
			}
			if (value instanceof String) {
				String path = parentPath.length() > 0 ? String.format("%s.%s", parentPath, key) : key;
				result.map.put(path, formatValue((String) value));
				continue;
			}
			if (value instanceof List) {
				throw new ReportException("Lists are not supported in format specification");
			}
		}
	}

	public static JoltFormat getInstance(Map<String, Object> map) {
		JoltFormat result = new JoltFormat();
		fillResult(result, map, "");
		return result;
	}
}
