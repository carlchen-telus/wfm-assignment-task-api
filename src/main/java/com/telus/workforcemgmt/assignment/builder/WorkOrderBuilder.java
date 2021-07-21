package com.telus.workforcemgmt.assignment.builder;

import java.time.LocalDateTime;
import java.util.List;

import com.telus.workforcemgmt.dto.locationmanagement.Location;
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
	private List<JobBuilder> jobBuilders;
	

	
}
