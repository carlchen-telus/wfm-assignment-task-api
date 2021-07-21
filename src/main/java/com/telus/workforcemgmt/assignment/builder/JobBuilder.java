package com.telus.workforcemgmt.assignment.builder;

import java.time.LocalDateTime;
import java.util.List;

import com.telus.workforcemgmt.capacitymanagement.dto.DemandStreamSummary;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.CalendarProfile;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.ProjectRequirements;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.TeamWorkerRequirements;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.TeamWorkerSpokenLanguage;
import com.telus.workforcemgmt.dto.workspecification.WorkOrderRuleSkill;
import com.telus.workforcemgmt.dto.workspecification.WorkSpecification;

import lombok.Data;

@Data
public class JobBuilder {
	
	private LocalDateTime effectiveDateTime;
	private WorkSpecification jobSpecification;
	private WorkSpecification workOrderSpecification;
	private ProjectRequirements projectRequirements;
	private CalendarProfile calendarProfile;
	private TeamWorkerRequirements teamWorkerRequirements;
	private TeamWorkerSpokenLanguage teamWorkerSpokenLanguage;
	private DemandStreamSummary demandStreamSummary; 

	
	private String skillsDescription;
	private String priorityCodeDescription;
	private String projectCodeDescription;
	
}
