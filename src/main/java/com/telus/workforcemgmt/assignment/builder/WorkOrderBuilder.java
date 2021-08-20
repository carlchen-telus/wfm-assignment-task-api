package com.telus.workforcemgmt.assignment.builder;

import java.time.LocalDateTime;
import java.util.List;

import com.telus.workforcemgmt.dto.locationmanagement.Location;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.CalendarProfile;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.ProjectRequirements;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.TeamWorkerRequirements;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.TeamWorkerSpokenLanguage;
import com.telus.workforcemgmt.dto.workspecification.WorkSpecification;
import com.telus.workforcemgmt.wil3.dto.WorkOrder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderBuilder {

	private LocalDateTime effectiveDateTime;
	private WorkOrder workorder;
	private Location location;
	private WorkSpecification workOrderSpecification;
	private ProjectRequirements projectRequirements;
	private CalendarProfile calendarProfile;
	private TeamWorkerRequirements teamWorkerRequirements;
	private TeamWorkerSpokenLanguage teamWorkerSpokenLanguage;
	private List<JobBuilder> jobBuilders;
	private String projectName;
	private String workOrderCategoryCode;
	
	private static final String projectCodeDescription = "Specified in work order by host > Derived from location FSA: 1) FSA ID not empty and work order is between FSA Ready date and FSA BAU date 2) work order is initated from FIFA() or Remedy () 3) work order service class R 4) work order has GPON component or has Voice/DSL/TTV with GPON technolgy";
}

