package com.telus.workforcemgmt.assignment.builder;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;

import org.springframework.format.annotation.DateTimeFormat;

public class JobBuilderRequest {

	@NotBlank
	private String jobTypeCd;
	@NotBlank
	private String productCd;
	@NotBlank
	private String serviceClassCd;
	private String technologyCd;
	@NotBlank
	private String workOrderClassificationCd;
	@NotBlank
	private String serviceAreaClliCd;
	private boolean outOfServiceInd = false;
	private String serviceSubclassCd = null;
	private boolean slaInd = false;
	private String workOrderActionCd;
	private String workgroupCd;
	private String troubleTypeTxt;
	private String causeLevel1Txt;
	private String causeLevel2Txt;
	private String causeLevel3Txt;
	private String severityCd;
	private String engagementLevel;
	@NotBlank
	private Double duration = 0.0D;
	private String specialProjectName;
	private String customerId;
	private String originalSystemId;
	private String locationId;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate effectiveDate; 
	private String[] components; //JOB_TYPE_CD-PRODUCT_CATEGORY_CD-TECHNOLOGY_CD-ESTIMATED_DURATION_AMT-WORK_ORDER_ACTION_CD-INSTALL_TYPE_CD
}
