package tr.com.srdc.cda2fhir.jolt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.ContextualTransform;

public class Substitute implements ContextualTransform {
	static Map<String, Chainr> templates = new HashMap<String, Chainr>();
	static {
		Map<String, List<Object>> rawTemplates = Utility.readTemplates();
		rawTemplates.entrySet().forEach(rowTemplate -> {
			String name = rowTemplate.getKey();
			List<Object> value = rowTemplate.getValue();
			Chainr chainr = Chainr.fromSpec(value);
			templates.put("->" + name, chainr);
		});
	}

	private Object findTemplateValue(Map<String, Object> map, Map<String, Object> context) {
		Set<String> keys = map.keySet();
		if (keys.size() != 1) {
			return null;
		}
		String key = keys.stream().findFirst().get();
		Chainr chainr = templates.get(key);
		if (chainr == null) {
			return null;
		}
		Object input = map.get(key);
		Object replacement = chainr.transform(input, context);
		return replacement;
	}

	@SuppressWarnings("unchecked")
	private Object substitute(Map<String, Object> object, Map<String, Object> context) {
		if (object == null) {
			return null;
		}

		Iterator<Map.Entry<String, Object>> itr = object.entrySet().iterator();

		List<String> topSubstitutes = new ArrayList<String>();
		while (itr.hasNext()) {
			Map.Entry<String, Object> entry = itr.next();
			String key = entry.getKey();
			Object value = entry.getValue();
			Chainr chainr = templates.get(key);
			if (chainr != null) {
				topSubstitutes.add(key);
				continue;
			}
			if (value instanceof List) {
				List<Object> elements = (List<Object>) value;
				Object newValue = substitute(elements, context);
				entry.setValue(newValue);
				continue;
			}
			if (value instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) value;
				Object replacement = findTemplateValue(map, context);
				if (replacement == null) {
					replacement = substitute(map, context);
				}
				if (replacement == null) {
					itr.remove();
				} else {
					entry.setValue(replacement);
				}
				continue;
			}
		}

		for (String key : topSubstitutes) {
			Object value = object.get(key);
			Chainr chainr = templates.get(key);
			Map<String, Object> additionalKeys = (Map<String, Object>) chainr.transform(value, context);
			if (additionalKeys != null) {
				object.putAll(additionalKeys);
			}
			object.remove(key);
		}

		if (object.isEmpty()) {
			return null;
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	private Object substitute(List<Object> list, Map<String, Object> context) {
		ListIterator<Object> itr = list.listIterator();
		while (itr.hasNext()) {
			Object element = itr.next();
			if (element instanceof List) {
				Object replacement = substitute((List<Object>) element, context);
				if (replacement == null) {
					itr.remove();
				} else {
					itr.set(replacement);
				}
				continue;
			}
			if (element instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) element;
				Object replacement = findTemplateValue(map, context);
				if (replacement == null) {
					replacement = substitute(map, context);
				}
				if (replacement == null) {
					itr.remove();
				} else {
					itr.set(replacement);
				}
				continue;
			}
		}
		if (list.size() == 0) {
			return null;
		}
		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object transform(Object input, Map<String, Object> context) {
		Map<String, Object> map = (Map<String, Object>) input;
		return substitute(map, context);
	}
}
