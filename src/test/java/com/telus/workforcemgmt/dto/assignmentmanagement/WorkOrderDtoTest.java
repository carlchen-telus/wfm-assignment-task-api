package com.telus.workforcemgmt.dto.assignmentmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.telus.workforcemgmt.assignment.utils.WorkOrderDifference;
import com.telus.workforcemgmt.dto.Difference;
import com.telus.workforcemgmt.dto.*;

public class WorkOrderDtoTest {
	
	private Gson createGson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateGsonSerializer());
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeGsonSerializer());
		gsonBuilder.registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeGsonSerializer());
		Gson gson = gsonBuilder.setPrettyPrinting().create();
		return gson;
	}
	
	@Test
	public void testEqualWorkOrderDtos() throws JsonMappingException, JsonProcessingException {
		WorkOrderDto workOrder = createWorkOrderDto();
		
		Gson gson = createGson();    
	    String json = gson.toJson(workOrder);
	    System.out.println(json);   
	    
	    
	    Map<String,Object> result = gson.fromJson(json, HashMap.class);
	    Map<String,Object> result2 = gson.fromJson(json, HashMap.class);

	    List<Difference> diff = WorkOrderDifference.diff(result, result2);
	    json = gson.toJson(diff);
	    System.out.println("difference:" + json);  
	}

	
	@Test
	public void testSkillDifference() throws JsonMappingException, JsonProcessingException {
		WorkOrderDto workOrder = createWorkOrderDto();
		WorkOrderDto workOrder2 = workOrder.toBuilder()
				.requiredSkillList(new TeamWorkerSkill[] {new TeamWorkerSkill("DST", "2.0"), new TeamWorkerSkill("POTS", "1.0")})
				.build();
		
		Gson gson = createGson();    
	    String json = gson.toJson(workOrder);
	    String json2= gson.toJson(workOrder2);
	    System.out.println(json);   
	   
	    Map<String,Object> result =
	            new ObjectMapper().readValue(json, HashMap.class);
	    Map<String,Object> result2 =
	            new ObjectMapper().readValue(json2, HashMap.class);
	    
	    System.out.println(json);
	    List<Difference> diff = WorkOrderDifference.diff(result2, result);
	    json = gson.toJson(diff);
	    System.out.println("difference:" + json);  
	}
	
	@Test
	public void testWorkOrderAttributeDifference() throws JsonMappingException, JsonProcessingException {
		WorkOrderDto workOrder = createWorkOrderDto();
		WorkOrderDto workOrder2 = workOrder.toBuilder()
				.workOrderAttributeList(Map.of("TYPE0", "TYPE0ValueUpdate", "TYPE3", "TYPE3Value"))
				.build();
		
		Gson gson = createGson();    
	    String json = gson.toJson(workOrder);
	    String json2= gson.toJson(workOrder2);
	    System.out.println(json);   
	   
	    Map<String,Object> result =
	            new ObjectMapper().readValue(json, HashMap.class);
	    Map<String,Object> result2 =
	            new ObjectMapper().readValue(json2, HashMap.class);
	    
	    System.out.println(json);
	    List<Difference> diff = WorkOrderDifference.diff(result2, result);
	    json = gson.toJson(diff);
	    System.out.println("difference:" + json);  
	}
	
	@Test
	public void testCompnentDifference() throws JsonMappingException, JsonProcessingException {
		WorkOrderDto workOrder = createWorkOrderDto();
		
		Component comp1 = Component.builder()
				.originatingSystemId("13574")
				.originatingSystemWorkOrderId("AR123")
				.jobTypeCd("MM")
				.estimatedDurationNum(2.0d)
				.statusCd("CANCELLED")
				.componentNumber(1)
				.componentRemarkList(Map.of("RMK1", "TYPE0Value", "RMK2", "TYPE1Value"))
				.componentRequiredSkillList(new TeamWorkerSkill[] {new TeamWorkerSkill("COMPDST", "1.0"), new TeamWorkerSkill("COMPPOTS", "2.0")})
				.build();
		
		Component comp2 = comp1.toBuilder().originatingSystemWorkOrderId("AR2346")
				.build();
		
		WorkOrderDto workOrder2 = workOrder.toBuilder()
				.componentList(Map.of(comp1.getOriginatingSystemWorkOrderId(), comp1))
				.build();
		
		Gson gson = createGson();    
	    String json = gson.toJson(workOrder);
	    String json2= gson.toJson(workOrder2);
	    System.out.println(json);   
	   
	    Map<String,Object> result =
	            new ObjectMapper().readValue(json, HashMap.class);
	    Map<String,Object> result2 =
	            new ObjectMapper().readValue(json2, HashMap.class);
	    
	    System.out.println(json);
	    List<Difference> diff = WorkOrderDifference.diff(result2, result);
	    json = gson.toJson(diff);
	    System.out.println("difference:" + json);  
	}
	
	@Test
	public void testCompnentSkillDifference() throws JsonMappingException, JsonProcessingException {
		WorkOrderDto workOrder = createWorkOrderDto();
		
		Component comp1 = Component.builder()
				.originatingSystemId("13574")
				.originatingSystemWorkOrderId("AR123")
				.jobTypeCd("MM")
				.estimatedDurationNum(1.0d)
				.statusCd("OPEN")
				.componentNumber(1)
				.componentRequiredSkillList(new TeamWorkerSkill[] {new TeamWorkerSkill("COMPDST", "2.0"), new TeamWorkerSkill("COMPPOTS2", "2.0")})
				.build();

		WorkOrderDto workOrder2 = workOrder.toBuilder()
				.componentList(Map.of(comp1.getOriginatingSystemWorkOrderId(), comp1))
				.build();
		
		Gson gson =  createGson();  
	    String json = gson.toJson(workOrder);
	    String json2= gson.toJson(workOrder2);
	    System.out.println(json);   
	   
	    Map<String,Object> result =
	            new ObjectMapper().readValue(json, HashMap.class);
	    Map<String,Object> result2 =
	            new ObjectMapper().readValue(json2, HashMap.class);
	    
	    System.out.println(json);
	    List<Difference> diff = WorkOrderDifference.diff(result2, result);
	    json = gson.toJson(diff);
	    System.out.println("difference:" + json);  
	}
	
	
	@Test
	public void testCompnentRemarkDifference() throws JsonMappingException, JsonProcessingException {
		WorkOrderDto workOrder = createWorkOrderDto();
		
		Component comp1 = Component.builder()
				.originatingSystemId("13574")
				.originatingSystemWorkOrderId("AR123")
				.componentRemarkList(Map.of("RMK1", "TYPE0ValueUpdate", "RMK3", "TYPE3Value"))
				.build();
		
		WorkOrderDto workOrder2 = workOrder.toBuilder()
				.componentList(Map.of(comp1.getOriginatingSystemWorkOrderId(), comp1))
				.build();
		
		Gson gson =  createGson();   
	    String json = gson.toJson(workOrder);
	    String json2= gson.toJson(workOrder2);
	    System.out.println(json);   
	   
	    Map<String,Object> result =
	            new ObjectMapper().readValue(json, HashMap.class);
	    Map<String,Object> result2 =
	            new ObjectMapper().readValue(json2, HashMap.class);
	    
	    System.out.println(json);
	    List<Difference> diff = WorkOrderDifference.diff(result2, result);
	    json = gson.toJson(diff);
	    System.out.println("difference:" + json);  
	}
	
	private WorkOrderDto createWorkOrderDto() {
		Component comp1 = Component.builder()
				.originatingSystemId("13574")
				.originatingSystemWorkOrderId("AR123")
				.jobTypeCd("MM")
				.estimatedDurationNum(1.0d)
				.statusCd("OPEN")
				.componentNumber(1)
				.componentRemarkList(Map.of("RMK1", "TYPE0Value", "RMK2", "TYPE1Value"))
				.componentRequiredSkillList(new TeamWorkerSkill[] {new TeamWorkerSkill("COMPDST", "1.0"), new TeamWorkerSkill("COMPPOTS", "2.0")})
				.build();
		
		Component comp2 = comp1.toBuilder().originatingSystemWorkOrderId("AR2345")
				.componentRequiredSkillList(new TeamWorkerSkill[] {new TeamWorkerSkill("COMP2DST", "1.0"), new TeamWorkerSkill("COMP2POTS", "2.0")})
				.build();
				
		WorkOrderDto workOrder = WorkOrderDto.builder()
		.appointmentStartDate(ZonedDateTime.now())
		.calendarName("CalendarName")
		.workOrderId(1234568L)
		.estimatedDurationNum(3.0d)
		.outOfServiceIndicator(true)
		.workOrderStatusCode("OPEN")
		.serviceIdentification(Map.of("ACCOUNT", "123456"))
		.requiredTechnicianList(new String[] {"X12345", "T12345"})
		.requiredSkillList(new TeamWorkerSkill[] {new TeamWorkerSkill("DST", "1.0"), new TeamWorkerSkill("POTS", "2.0")})
		.workOrderAttributeList(Map.of("TYPE0", "TYPE0Value", "TYPE1", "TYPE1Value"))
		.componentList(Map.of(comp1.getOriginatingSystemWorkOrderId(), comp1, comp2.getOriginatingSystemWorkOrderId(), comp2))
		.build();
		return workOrder;
	}

}
