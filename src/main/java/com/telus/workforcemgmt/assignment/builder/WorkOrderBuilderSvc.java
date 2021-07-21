package com.telus.workforcemgmt.assignment.builder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.telus.workforcemgmt.capacitymanagement.dto.DemandStreamRequest;
import com.telus.workforcemgmt.capacitymanagement.dto.DemandStreamSummary;
import com.telus.workforcemgmt.capacitymanagement.dto.GeoPoint;
import com.telus.workforcemgmt.dto.InputHeader;
import com.telus.workforcemgmt.dto.assignmentmanagement.SearchAvailabilityPeriod;
import com.telus.workforcemgmt.dto.assignmentmanagement.WorkOrderAttributeCode;
import com.telus.workforcemgmt.dto.locationmanagement.Location;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.CalendarProfile;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.ProjectRequirements;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.TeamWorkerRequirements;
import com.telus.workforcemgmt.dto.resourcemanagement.dto.TeamWorkerSpokenLanguage;
import com.telus.workforcemgmt.dto.workspecification.WorkSpecification;
import com.telus.workforcemgmt.dto.workspecification.WorkSpecificationRequest;
import com.telus.workforcemgmt.wil3.dto.ComponentList;
import com.telus.workforcemgmt.wil3.dto.TypeCode;
import com.telus.workforcemgmt.wil3.dto.TypeCodeList;
import com.telus.workforcemgmt.wil3.dto.TypedLocationAddress;
import com.telus.workforcemgmt.wil3.dto.WorkOrder;

@Service
public class WorkOrderBuilderSvc {

	private static Logger log = LoggerFactory.getLogger(WorkOrderBuilderSvc.class);
	
	@Value("${spring.url.workspecification}")
	private String workSpecificationUri;
	@Value("${spring.url.capaciymanagement}")
	private String capacitymanagementUri;
	@Value("${spring.url.systemadapter}")
	private String systemadapterUri;
							
	public GuidingBuilder guiding(WorkOrder workorder, InputHeader header) {
		Location location = getLocation(workorder.getLocation(), header);
		WorkSpecification spec = getWorkSpecification(location, workorder, header);
		return GuidingBuilder.builder().location(location).workorderSpec(spec).build();
	}
	
	public WorkOrderBuilder build(GuidingBuilder guiding, WorkOrder workorder, InputHeader header) {
		LocalDateTime effDatetime = getEffectiveDate(workorder, guiding.getLocation());
		return build(guiding.getWorkorderSpec(), workorder, guiding.getLocation(), effDatetime, header);
	}
	
	public List<WorkOrderBuilder> build(GuidingBuilder guiding, WorkOrder workorder, SearchAvailabilityPeriod searchPeriod, InputHeader header) {
		Location location = guiding.getLocation();
		ZoneId  timezone = ZoneId.of(location.getTimezone());
		LocalDateTime start = searchPeriod.getStartDateTime().withZoneSameInstant(timezone).toLocalDateTime();
		LocalDateTime end = searchPeriod.getEndDateTime().withZoneSameInstant(timezone).toLocalDateTime();
		return Stream.iterate(start, d -> start.plusDays(1L)).limit(ChronoUnit.DAYS.between(start, end) + 1)
			.map(effDatetime -> build(guiding.getWorkorderSpec(), workorder, location, effDatetime, header))
			.collect(Collectors.toList());
	}
	
	public WorkOrderBuilder build(WorkOrder workorder, InputHeader header) {
		Location location = getLocation(workorder.getLocation(), header);
		LocalDateTime effDatetime = getEffectiveDate(workorder, location);
		return build(workorder, location, effDatetime, header);
	}
	
	public List<WorkOrderBuilder> build(WorkOrder workorder, SearchAvailabilityPeriod searchPeriod,InputHeader header) {
		Location location = getLocation(workorder.getLocation(), header);
		ZoneId  timezone = ZoneId.of(location.getTimezone());
		LocalDateTime start = searchPeriod.getStartDateTime().withZoneSameInstant(timezone).toLocalDateTime();
		LocalDateTime end = searchPeriod.getEndDateTime().withZoneSameInstant(timezone).toLocalDateTime();
		return Stream.iterate(start, d -> start.plusDays(1L)).limit(ChronoUnit.DAYS.between(start, end) + 1)
			.map(effDatetime -> build(workorder, location, effDatetime, header))
			.collect(Collectors.toList());
	}
	
	private WorkOrderBuilder build(WorkOrder workorder, Location location, LocalDateTime effDatetime, InputHeader header) {
		WorkSpecification workorderSpec = getWorkSpecification(location, workorder, header);
		return build(workorderSpec, workorder, location, effDatetime, header);
	}

	private WorkOrderBuilder build(WorkSpecification workorderSpec, WorkOrder workorder, Location location, LocalDateTime effDatetime, InputHeader header) {
		boolean isFsa = withinFsaBauPeriod(workorder, location) && workorder.getProjectCd() == null;
		String projectCd = isFsa ? ConstantCodes.PROJEC_FALCON : workorder.getProjectCd();
		ProjectRequirements projectRequirements = getProjectRequirements(projectCd, workorderSpec.getWfmScopeRule().getWorkOrderCategoryCd(), header);
		
		CalendarProfile calendarProfile =  getCalendarProfile(location.getLocationId(), workorderSpec.getWfmScopeRule().getWorkOrderCategoryCd(), header);
		
		TeamWorkerSpokenLanguage teamWorkerSpokenLanguage = getTeamWorkerSpokenLanguage(location.getLocationId(), workorderSpec.getWfmScopeRule().getWorkOrderCategoryCd(), header);
		
		TeamWorkerRequirements teamWorkerRequirements = getTeamWorkerRequirements(
				location.getLocationId(), projectCd, workorder.getSiteAccessCd(), 
				workorderSpec.getWfmScopeRule().getWorkOrderCategoryCd(), 
				isFsa ? location.getFsa().getFsaName() : null, effDatetime, header);
		
		WorkSpecification[] jobSpecs = getJobWorkSpecification(workorderSpec, header);
		
		List<JobBuilder> builders = Arrays.stream(jobSpecs).map(spec -> {
			JobBuilder builder = new JobBuilder();
			builder.setCalendarProfile(calendarProfile);
			builder.setProjectRequirements(projectRequirements);
			builder.setTeamWorkerRequirements(teamWorkerRequirements);
			builder.setTeamWorkerSpokenLanguage(teamWorkerSpokenLanguage);
			builder.setDemandStreamSummary(getDemandStreamSummary(workorder, spec, location, effDatetime, projectCd,header));
			builder.setJobSpecification(spec);
			builder.setEffectiveDateTime(effDatetime);
			builder.setWorkOrderSpecification(workorderSpec);
			return builder;
		}).collect(Collectors.toList());
		
		return WorkOrderBuilder.builder()
				.jobBuilders(builders)
				.location(location)
				.workorder(workorder)
				.workOrderSpecification(workorderSpec)
				.effectiveDateTime(effDatetime)
				.build();
	}
	
	
	private DemandStreamSummary getDemandStreamSummary(WorkOrder workorder, WorkSpecification spec, Location location, LocalDateTime effDt, 
			String projectCd, InputHeader header){
		DemandStreamRequest request = DemandStreamRequest.builder()
				.customerId(workorder.getCustomerId())
				.districtName(spec.getVirtualNavHierarchy().getDistrictNm())
				.effectiveDate(effDt.toLocalDate())
				.geoPoint(new GeoPoint(Double.parseDouble(location.getGeoPoint().getLatitudeTxt())
						, Double.parseDouble(location.getGeoPoint().getLongitudeTxt())))
				.jobTypeCd(spec.getWfmScopeRule().getJobTypeCd())
				.locationId(Long.toString(location.getLocationId()))
				.originalSystemId(workorder.getOriginatingSystemId())
				.outOfServiceInd(workorder.isOutofServiceInd())
				.productCd(spec.getWfmScopeRule().getProductCd())
				.serviceAreaName(spec.getVirtualNavHierarchy().getServiceAreaNm())
				.serviceClassCd(spec.getWfmScopeRule().getServiceClassCd())
				.severityCd(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.SEVERITY.name()))
				.skillLevelUsageCd(spec.getWorkOrderRuleSkillLevel().getSkillLevelUsageCd())
				.slaInd(ConstantCodes.FWDS_BOOLREAN_TRUE.equals(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.SLA.name())))
				.specialProjectName(projectCd)
				.technologyCd(spec.getWorkOrderRule().getTechnologyCd())
				.workOrderActionCatgryCd(spec.getWorkOrderRule().getWorkOrderActionCatgryCd())
				.workOrderCategoryCd(spec.getWfmScopeRule().getWorkOrderCategoryCd())
				.workOrderClassificationCd(workorder.getClassificationCd())
				.build();
		try {
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.capacitymanagementUri + "/demandstream")
					.queryParamIfPresent("customerId", Optional.of(request.getCustomerId()))
					.queryParamIfPresent("districtName", Optional.of(request.getDistrictName()))
					.queryParamIfPresent("effectiveDate", Optional.of(DateTimeFormatter.ISO_DATE.format(request.getEffectiveDate())))
					.queryParamIfPresent("latitude", Optional.of(request.getGeoPoint().getLatitude()))
					.queryParamIfPresent("longitude", Optional.of(request.getGeoPoint().getLongitude()))
					.queryParamIfPresent("installTypeCd", Optional.of(request.getInstallTypeCd()))
					.queryParamIfPresent("jobTypeCd", Optional.of(request.getJobTypeCd()))
					.queryParamIfPresent("locationId", Optional.of(request.getLocationId()))
					.queryParamIfPresent("originalSystemId", Optional.of(request.getOriginalSystemId()))
					.queryParamIfPresent("outOfServiceInd", Optional.of(request.getOutOfServiceInd()))
					.queryParamIfPresent("productCd", Optional.of(request.getProductCd()))
					.queryParamIfPresent("serviceAreaName", Optional.of(request.getServiceAreaName()))
					.queryParamIfPresent("serviceClassCd", Optional.of(request.getServiceClassCd()))
					.queryParamIfPresent("severityCd", Optional.of(request.getSeverityCd()))
					.queryParamIfPresent("skillLevelUsageCd", Optional.of(request.getSkillLevelUsageCd()))
					.queryParamIfPresent("slaInd", Optional.of(request.getSlaInd()))
					.queryParamIfPresent("specialProjectName", Optional.of(request.getSpecialProjectName()))
					.queryParamIfPresent("technologyCd", Optional.of(request.getTechnologyCd()))
					.queryParamIfPresent("workOrderActionCatgryCd", Optional.of(request.getWorkOrderActionCatgryCd()))
					.queryParamIfPresent("workOrderCategoryCd", Optional.of(request.getWorkOrderCategoryCd()))
					.queryParamIfPresent("workOrderClassificationCd", Optional.of(request.getWorkOrderClassificationCd()));
			
			ResponseEntity<DemandStreamSummary> response = restTemplate.exchange(
					builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), DemandStreamSummary.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private WorkSpecification[] getJobWorkSpecification(WorkSpecification workOrderSpec, InputHeader header) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(workOrderSpec.getLink("jobspecifications").get().getHref());
		try {
			ResponseEntity<WorkSpecification[]> response = restTemplate.exchange(
				builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), 
				WorkSpecification[].class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private TeamWorkerSpokenLanguage getTeamWorkerSpokenLanguage(Long locationId, String workOrderCategoryCd, InputHeader header) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.systemadapterUri + "/teamWorkerSpokenLanguage")
				.queryParam("locationId", locationId)
				.queryParam("workOrderCategoryCd", workOrderCategoryCd);
		try {
			ResponseEntity<TeamWorkerSpokenLanguage> response = restTemplate.exchange(
				builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), TeamWorkerSpokenLanguage.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private TeamWorkerRequirements getTeamWorkerRequirements(Long locationId, String specialProjectCd, String siteAccessCd, String workOrderCategoryCd, String fsaId, 
			LocalDateTime effectiveDate, InputHeader header) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.systemadapterUri + "/teamWorkerRequirements")
				.queryParam("locationId", locationId)
				.queryParam("specialProjectCd", specialProjectCd)
				.queryParam("effectiveDate", DateTimeFormatter.ISO_DATE.format(effectiveDate.toLocalDate()))
				.queryParam("siteAccessCd", siteAccessCd)
				.queryParam("workOrderCategoryCd", workOrderCategoryCd)
				.queryParam("fsaId", fsaId);
		try {
			ResponseEntity<TeamWorkerRequirements> response = restTemplate.exchange(
				builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), TeamWorkerRequirements.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	private ProjectRequirements getProjectRequirements(String projectCd, String workOrderCategoryCd, InputHeader header) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.systemadapterUri + "/projectRequirements")
				.queryParam("projectCd", projectCd)
				.queryParam("workOrderCategoryCd", workOrderCategoryCd);
		try {
			ResponseEntity<ProjectRequirements> response = restTemplate.exchange(
				builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), ProjectRequirements.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	private CalendarProfile getCalendarProfile(Long locationId, String workOrderCategoryCd, InputHeader header) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.systemadapterUri + "/calendarProfile")
				.queryParam("locationId", locationId)
				.queryParam("workOrderCategoryCd", workOrderCategoryCd);
		try {
			ResponseEntity<CalendarProfile> response = restTemplate.exchange(
				builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), CalendarProfile.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private WorkSpecification getWorkSpecification(Location loc, WorkOrder workorder, InputHeader header) {
		WorkSpecificationRequest req = new WorkSpecificationRequest();
		req.setCauseLevel1Txt(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.LEVEL_1.name()));
		req.setCauseLevel2Txt(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.LEVEL_2.name()));
		req.setCauseLevel3Txt(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.LEVEL_3.name()));
		req.setEngagementLevel(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.ENGAGEMENT_LEVEL.name()));
		req.setTroubleTypeTxt(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.TROUBLE_TYPE.name()));
		req.setSeverityCd(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.SEVERITY.name()));
		req.setSlaInd(ConstantCodes.FWDS_BOOLREAN_TRUE.equals(getWorkOrderAttribute(workorder.getWorkOrderAttributeList(), WorkOrderAttributeCode.SLA.name())));
		req.setDuration(workorder.getEstimatedDurationNum() != null ? workorder.getEstimatedDurationNum().doubleValue() : null);
		req.setEffectDT(getEffectiveDate(workorder, loc));
		req.setJobTypeCd(workorder.getJobTypeCd());
		req.setMultiUnitInd(loc.isMultiUnitAddressInd());
		req.setNumberOfTechRequired(workorder.getRequiredTeamWorkerNum());
		req.setOutOfServiceInd(workorder.isOutofServiceInd());
		req.setProductCd(workorder.getProductCategoryCd());
		req.setServiceAreaClliCd(loc.getServiceAreaCLLI());
		req.setServiceSubclassCd(workorder.getServiceSubClassCd());
		req.setServiceClassCd(workorder.getServiceClassCd());
		req.setTechnologyCd(workorder.getProductTechnologyCd());
		req.setWorkgroupCd(workorder.getWorkGroupCd());
		req.setWorkOrderActionCd(workorder.getWorkOrderActionCd());
		req.setWorkOrderClassificationCd(workorder.getClassificationCd());
		req.setComponents(getComponentsSpecReq(workorder.getComponentList()));
		
		try {
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.workSpecificationUri + "/workorderspecification")
					.queryParamIfPresent("causeLevel1Txt", Optional.of(req.getCauseLevel1Txt()))
					.queryParamIfPresent("causeLevel2Txt", Optional.of(req.getCauseLevel2Txt()))
					.queryParamIfPresent("causeLevel3Txt", Optional.of(req.getCauseLevel3Txt()))
					.queryParamIfPresent("components", Optional.of(req.getComponents()))
					.queryParamIfPresent("duration", Optional.of(req.getDuration()))
					.queryParamIfPresent("effectDT", Optional.of(req.getEffectDT()))
					.queryParamIfPresent("engagementLevel", Optional.of(req.getEngagementLevel()))
					.queryParamIfPresent("jobTypeCd", Optional.of(req.getJobTypeCd()))
					.queryParamIfPresent("numberOfTechRequired", Optional.of(req.getNumberOfTechRequired()))
					.queryParamIfPresent("productCd", Optional.of(req.getProductCd()))
					.queryParamIfPresent("serviceAreaClliCd", Optional.of(req.getServiceAreaClliCd()))
					.queryParamIfPresent("serviceClassCd", Optional.of(req.getServiceClassCd()))
					.queryParamIfPresent("serviceSubclassCd", Optional.of(req.getServiceSubclassCd()))
					.queryParamIfPresent("severityCd", Optional.of(req.getSeverityCd()))
					.queryParamIfPresent("technologyCd", Optional.of(req.getTechnologyCd()))
					.queryParamIfPresent("troubleTypeTxt", Optional.of(req.getTroubleTypeTxt()))
					.queryParamIfPresent("workgroupCd", Optional.of(req.getWorkgroupCd()))
					.queryParamIfPresent("workOrderActionCd", Optional.of(req.getWorkOrderActionCd()))
					.queryParamIfPresent("workOrderClassificationCd", Optional.of(req.getWorkOrderClassificationCd()))
					.queryParamIfPresent("outOfServiceInd", Optional.of(req.isOutOfServiceInd()))
					.queryParamIfPresent("slaInd", Optional.of(req.isSlaInd()))
					.queryParamIfPresent("multiUnitInd", Optional.of(req.isMultiUnitInd()));
			
			ResponseEntity<WorkSpecification> response = restTemplate.exchange(
					builder.build().encode().toUri(), HttpMethod.GET, new HttpEntity(headers), WorkSpecification.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private boolean withinFsaBauPeriod(WorkOrder workorder, Location loc) {
		LocalDateTime effectiveDate = getEffectiveDate(workorder, loc);
		return isFSACandidate(workorder) &&	loc.getFsa() != null && loc.getFsa().getFsaName() != null 
				&& loc.getFsa().getBauDate() != null 
				&& loc.getFsa().getBauDate().atStartOfDay().isAfter(effectiveDate) 
				&& loc.getFsa().getReadyDate().atStartOfDay().isBefore(effectiveDate);
	}
	
	private boolean isFSACandidate(WorkOrder order) {
		boolean ret = ConstantCodes.SERVICECLASS_RESIDENTIAL.equals(order.getServiceClassCd())
				&& ArrayUtils.contains(ConstantCodes.FSA_CANDIDATE_ID, order.getOriginatingSystemId())
				&& ((WorkOrderComponentUtils.hasVoiceDSLTTV(order) && WorkOrderComponentUtils.hasGPONTechnology(order))
					|| WorkOrderComponentUtils.hasGPONComponent(order.getComponentList()));
		return ret;
	}
	
	private LocalDateTime getEffectiveDate(WorkOrder workorder, Location loc) {
		ZonedDateTime effectiveDate = null;
		if (workorder.getAppointmentStartDate() != null) {
				effectiveDate = workorder.getAppointmentStartDate().toGregorianCalendar().toZonedDateTime();
		} else if (workorder.getDueDate() != null){
				effectiveDate = workorder.getDueDate().toGregorianCalendar().toZonedDateTime();
		} else {
			throw new IllegalArgumentException("No effective date found in workorder");
		}
		ZoneId workOrderZone = ZoneId.of(loc.getTimezone());
		ZonedDateTime effZoned = effectiveDate.withZoneSameInstant(workOrderZone);
		return effZoned.toLocalDateTime();
	}
	
	private String[] getComponentsSpecReq(ComponentList list) {
		if (list == null) return null;
		return list.getComponent().stream().map(comp -> {
			//JOB_TYPE_CD-PRODUCT_CATEGORY_CD-TECHNOLOGY_CD-ESTIMATED_DURATION_AMT-WORK_ORDER_ACTION_CD-INSTALL_TYPE_CD
			return comp.getJobTypeCd()+"-"+comp.getProductCategoryCd() +"-"
					+ comp.getProductTechnologyCd()+"-"+comp.getEstimatedDurationNum()+"-"
					+comp.getWorkOrderActionCd()+"-"+comp.getInstallationTypeCd();
		}).collect(Collectors.toList()).toArray(new String[0]);
	}
	
	private String getWorkOrderAttribute(TypeCodeList list, String code) {
		if (list == null) return null;
		TypeCode attribute = list.getTypeCode().stream().filter(attr -> attr.getTypeCd().equals(code)).findFirst().orElse(null);
		if (attribute != null) {
			return attribute.getDescriptionTxt();
		}
		return null;
	}
	
	public Location getLocation(com.telus.workforcemgmt.wil3.dto.Location loc, InputHeader header){
		try {
			TypedLocationAddress typedLoc = loc.getLocationList().stream().filter(data -> data.isDispatchLocationInd()).findFirst().get();
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.set(ConstantCodes.TRANSACTION_ID, header.getTransactionId());
			ResponseEntity<Location> response = restTemplate.exchange(
					systemadapterUri + "/location/" + typedLoc.getLocationId(), HttpMethod.GET, new HttpEntity(headers), Location.class);
			return response.getBody();
		} catch (HttpStatusCodeException ex) {
			throw new RuntimeException(ex);
		}
	}
}
