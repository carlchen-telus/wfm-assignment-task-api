package com.telus.workforcemgmt.assignment.utils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.telus.workforcemgmt.dto.Difference;
import com.telus.workforcemgmt.dto.Operation;

public class WorkOrderDifference {

	private static final Set<Class<?>> JSON_PRIMITIVES = Set.of(
			Integer.class,
			Long.class,
			Double.class,
			Boolean.class,
			String.class,
			ZonedDateTime.class, 
			LocalDate.class
			);

	private static final Set<String> DEEP_COMPARE_KEYS = Set.of(
			"workOrderRemarkList", "workOrderAttributeList", "workOrderDetailList", 
			"componentList", "componentAttributeList", "componentDetailList", "componentRemarkList"
	);

	private static final String COMPONENT = "componentList";
	private static final String COMPONENT_KEY = "originatingSystemWorkOrderId";
	
	private static final String TYPE_CODE_KEY = "typeCd";
	private static final String TYPE_CODE_VALUE= "descriptionTxt";
	//
	public static List<Difference> diff(Map<String, Object> newValue,
			Map<String, Object> oldValue) {
		return diff(newValue, oldValue, "/");
	}

	private static  List<Difference> diff(Map<String, Object> newValue,
			Map<String, Object> oldValue,
			String path) {
		if (newValue == oldValue) {
			return List.of();
		}
		
		List<Difference> differences = new ArrayList<>();
		
		if (newValue == null) return differences;
		if (oldValue == null) {
			differences.add(new Difference(newValue, null, path, Operation.ADDED));
			return differences;
		}
		
		Set<String> keys = new HashSet<>();
		keys.addAll(newValue.keySet());
		keys.addAll(oldValue.keySet());


		keys.forEach(key -> {
			//if host doesn't send the field, it means it 's not changed
			if (oldValue.containsKey(key) && !newValue.containsKey(key)) {

				// new key is added
			} else if (newValue.containsKey(key) && !oldValue.containsKey(key)) {
				differences.add(new Difference(newValue.get(key), null, path + key, Operation.ADDED));
				// existing key is modified
			} else {
				differences.addAll(compare(newValue.get(key), oldValue.get(key), path + key + "/", key));
			}
		});
		return differences;
	}

	private static List<Difference> compare(Object newValue, Object oldValue, String path, String key) {
		var differences = new ArrayList<Difference>();
		if (newValue == null) return differences;
		if (oldValue == null) {
			differences.add(new Difference(newValue, oldValue, path, Operation.ADDED));
			return differences;
		}

		var fromClass = newValue.getClass();
		var toClass = oldValue.getClass();
		if (oneIsPrimitive(fromClass, toClass)) {
			if (!newValue.equals(oldValue)) {
				differences.add(new Difference(newValue, oldValue, path, Operation.UPDATED));
			}
		} 
		else if (bothAreObjects(newValue, oldValue)) {
			differences.addAll(diff((Map<String, Object>) newValue, (Map<String, Object>) oldValue, path));
		} 

		else if (bothAreArrays(fromClass, toClass) && !requireDeepCompare(key)) {
			List fromArray = (ArrayList) newValue;
			List toArray = (ArrayList) oldValue;
						
			if (!toString(fromArray).equals(toString(toArray))) {
				differences.add(new Difference(fromArray, toArray, path, Operation.UPDATED));
			}
		} 

		else if (bothAreArrays(fromClass, toClass) && requireDeepCompare(key)) {
			List<Map> fromArray = (ArrayList<Map>) newValue;
			List<Map> toArray = (ArrayList<Map>) oldValue;

			if (COMPONENT.equals(key)) {
				differences.addAll(
						fromArray.stream().flatMap(data -> {
							List<Difference> res = diff(data, searchMap(toArray, COMPONENT_KEY, (String)data.get(COMPONENT_KEY)), 
									path + (String)data.get(COMPONENT_KEY) + "/");
							return res.stream();
						}).collect(Collectors.toList()));
			} else {// type code
				differences.addAll(
						fromArray.stream().flatMap(data -> {
							String typeCodeDescriptionTxt = (String)data.get(TYPE_CODE_VALUE);
							String typeCode = (String)data.get(TYPE_CODE_KEY);
							Map toValue = searchMap(toArray, TYPE_CODE_KEY, typeCode);
							List<Difference> typeCodeDiffs = compare(typeCodeDescriptionTxt, 
								toValue == null?  null : toValue.get(TYPE_CODE_VALUE), 
								path + typeCode + "/", 
								typeCode);
							return typeCodeDiffs.stream();
						}).collect(Collectors.toList()));

			}
		}
		return differences;
	}

	private static String toString(List listofMap) {
		StringBuilder b = new StringBuilder();
		listofMap.stream().map(WorkOrderDifference::toString) 
				.sorted().forEach(b::append);
		return b.toString();
	}
	
	private static String toString(Object obj) {
		if (JSON_PRIMITIVES.contains(obj.getClass())) 
			return obj.toString();
		else if (obj instanceof Map) {
			return mapToString((Map)obj);
		}
		else {
			throw new IllegalArgumentException("doesn't support type:" + obj.getClass().getName());
		}
	}
	
	private static String mapToString(Map<String, String> map) {
		Set<String> keyset = map.keySet();
		List<String> sorted = new ArrayList(keyset);
		StringBuilder b = new StringBuilder();
		sorted.stream().map(e -> e + map.get(e))
			.forEach(b::append);;
		return b.toString();
	}
	
	private  static boolean oneIsPrimitive(Class<?> from, Class<?> to) {
		return JSON_PRIMITIVES.contains(to) || JSON_PRIMITIVES.contains(from);
	}

	private  static boolean bothAreObjects(Object from, Object to) {
		return from instanceof Map && to instanceof Map;
	}

	private  static boolean bothAreArrays(Class<?> from, Class<?> to) {
		return from == ArrayList.class && to == ArrayList.class;
	}

	private  static boolean requireDeepCompare(String key) {
		return DEEP_COMPARE_KEYS.contains(key);  
	}

	private  static Map searchMap(List<Map> listOfMap, String key, String value) {
		for (Map m : listOfMap) {
			if (m.containsKey(key) && m.get(key).equals(value)) {
				return m;
			}
		}
		return null;
	}
}
