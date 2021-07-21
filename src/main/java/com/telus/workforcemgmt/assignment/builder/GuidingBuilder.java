package com.telus.workforcemgmt.assignment.builder;

import com.telus.workforcemgmt.dto.locationmanagement.Location;
import com.telus.workforcemgmt.dto.workspecification.WorkSpecification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuidingBuilder {

	private Location location;
	private WorkSpecification workorderSpec;
}
