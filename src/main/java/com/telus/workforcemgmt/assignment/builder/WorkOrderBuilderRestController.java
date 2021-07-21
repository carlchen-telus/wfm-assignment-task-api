package com.telus.workforcemgmt.assignment.builder;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import com.telus.workforcemgmt.dto.InputHeader;
import com.telus.workforcemgmt.dto.assignmentmanagement.SearchAvailabilityPeriod;
import com.telus.workforcemgmt.wil3.dto.WorkOrder;

@RestController
@Validated
public class WorkOrderBuilderRestController {

	@Autowired
	private WorkOrderBuilderSvc workOrderBuilderSvc;
	
	
	public WorkOrderBuilder createWorkOrderBuilder(WorkOrder workorder, InputHeader header) {
		return workOrderBuilderSvc.build(workorder, header);
	}
	
	public List<WorkOrderBuilder> searchAvailabilityWorkOrderBuilder(WorkOrder workorder, SearchAvailabilityPeriod period, InputHeader header) {
		return workOrderBuilderSvc.build(workorder, period, header);
	}
}
