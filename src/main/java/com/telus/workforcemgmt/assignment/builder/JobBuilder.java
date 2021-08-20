package com.telus.workforcemgmt.assignment.builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.telus.workforcemgmt.capacitymanagement.dto.DemandStreamSummary;
import com.telus.workforcemgmt.dto.workspecification.WorkSpecification;
import com.telus.workforcemgmt.wil3.dto.TeamWorkerSkill;

import lombok.Data;

@Data
public class JobBuilder {
	
	private LocalDateTime effectiveDateTime;
	private WorkSpecification jobSpecification;
	private DemandStreamSummary demandStreamSummary;
	private List<DemandStreamSummary> demandStreamCandidates;
		
	private static final String skillsDescription = "Specified in work order by host > Combined by components specification WORK_ORDER_RULE_SKILL > Derived from work order specification WORK_ORDER_RULE_SKILL.";
	private static final String skillLevelUsageCodeDescription = "If all component specifications WORK_ORDER_RULE_SKILL_LEVEL have same skillLevelUsage , use this skillLevelUsage; else use skillLevelUsage from work orderSpecification WORK_ORDER_RULE_SKILL_LEVEL";
	private static final String priorityCodeDescription = "Derived from work order 'ENGAGEMENT_LEVEL' (table PRIORITY_OVERRIDE_RULE) > Specified in work order by host  > derived from work order specification SEVERITY_PRIORITY_RULE  > derivd from special project  > derived from component specifications WORK_ORDER_RULE  > derived from work order specification WORK_ORDER_RULE ";
	private static final String technologyCodeDescription = " Derived from technologyCodes in components specified by host (SAT->OTT->GFAST->GPON->FIBRE->WIRELESS->COPPER_BONDED->ETTS->COPPER->MICRO_DPU)  > derived from special project > Specified in work order by host";
	
	private static final Integer SKILL_LEVEL_DEFAUL = 3;
	private List<TeamWorkerSkill> requiredSkills;
	private String skillLevelUsage;
	private Integer skillLevel = SKILL_LEVEL_DEFAUL;
	private String priorityCd;
	private String technologyCode;
	
	
	
	public void init(WorkOrderBuilder workOrderBuilder) {
		calculateTechnologyCode(workOrderBuilder);
		calculatePriority(workOrderBuilder);
		calculateSkillLevel(workOrderBuilder);
		calculateSkills(workOrderBuilder);
	}
	
	private void calculateTechnologyCode(WorkOrderBuilder workOrderBuilder) {
		if (this.jobSpecification.getComponentSpecificationList() != null && !this.jobSpecification.getComponentSpecificationList().isEmpty()) {
			this.technologyCode = this.jobSpecification.getComponentSpecificationList()
				.stream().map(comp -> comp.getWorkOrderRule().getTechnologyCd())
				.sorted((tech1, tech2) -> techmap.get(tech1) - techmap.get(tech2)).findFirst().get();
		} else if (workOrderBuilder.getProjectRequirements() != null && workOrderBuilder.getProjectRequirements().getTechnology() != null) {
			this.technologyCode = workOrderBuilder.getProjectRequirements().getTechnology();
		} else if (workOrderBuilder.getWorkorder().getProductTechnologyCd() != null) {
			this.technologyCode = workOrderBuilder.getWorkorder().getProductTechnologyCd();
		}
	}
	
	private void calculatePriority(WorkOrderBuilder workOrderBuilder) {
		if (workOrderBuilder.getWorkOrderSpecification().getPriorityOverrideRule() != null) {
			this.priorityCd = workOrderBuilder.getWorkOrderSpecification().getPriorityOverrideRule().getJobPriorityCd();
		} else if (workOrderBuilder.getWorkorder().getPriorityCd() != null) {
			this.priorityCd = workOrderBuilder.getWorkorder().getPriorityCd();
		} else if (workOrderBuilder.getWorkOrderSpecification().getSeverityPriorityRule() != null) {
			this.priorityCd = Integer.toString(workOrderBuilder.getWorkOrderSpecification().getSeverityPriorityRule().getJobPriorityNum().intValue());
		} else if (workOrderBuilder.getProjectRequirements() != null) {
			this.priorityCd = workOrderBuilder.getProjectRequirements().getPriority();
		} else if (this.jobSpecification.getComponentSpecificationList() != null && !this.jobSpecification.getComponentSpecificationList().isEmpty()) {
			BigDecimal priority = jobSpecification.getComponentSpecificationList().stream()
			.sorted((comSpec1, compSpec2) -> comSpec1.getWorkOrderRule().getJobPriorityNum().intValue() - compSpec2.getWorkOrderRule().getJobPriorityNum().intValue())
			.findFirst().get().getWorkOrderRule().getJobPriorityNum();
			this.priorityCd = Integer.toString(priority.intValue());
		} else {
			this.priorityCd = Integer.toString(workOrderBuilder.getWorkOrderSpecification().getWorkOrderRule().getJobPriorityNum().intValue());
		}
	}
	
	private void calculateSkillLevel(WorkOrderBuilder workOrderBuilder) {
		if (this.jobSpecification.getComponentSpecificationList() != null && !this.jobSpecification.getComponentSpecificationList().isEmpty()) {
			Set<String> skillLevelUsages = this.jobSpecification.getComponentSpecificationList()
					.stream().map(data -> data.getWorkOrderRuleSkillLevel().getSkillLevelUsageCd())
					.collect(Collectors.toSet());
			if (skillLevelUsages.size() == 1)  skillLevelUsage = skillLevelUsages.iterator().next();
			
			BigDecimal level = this.jobSpecification.getComponentSpecificationList()
					.stream().map(data -> data.getWorkOrderRuleSkillLevel().getSkillLevelNum())
					.sorted().findFirst().orElse(null);
			if (level != null) skillLevel = level.intValue();
			
		}
		if (skillLevelUsage == null && workOrderBuilder.getWorkOrderSpecification().getWorkOrderRuleSkillLevel() != null) {
			skillLevelUsage = workOrderBuilder.getWorkOrderSpecification().getWorkOrderRuleSkillLevel().getSkillLevelUsageCd();
		}
		if (skillLevel == null && workOrderBuilder.getWorkOrderSpecification().getWorkOrderRuleSkillLevel() != null) {
			BigDecimal level = workOrderBuilder.getWorkOrderSpecification().getWorkOrderRuleSkillLevel().getSkillLevelNum();
			if (level != null) skillLevel = level.intValue();
		} 
	}
	
	private void calculateSkills(WorkOrderBuilder workOrderBuilder) {
		if (workOrderBuilder.getWorkorder().getRequiredSkillList() != null && !workOrderBuilder.getWorkorder().getRequiredSkillList().getRequiredSkill().isEmpty()) {
			this.requiredSkills = workOrderBuilder.getWorkorder().getRequiredSkillList().getRequiredSkill();
		} 
		else if (this.jobSpecification.getComponentSpecificationList() != null && !this.jobSpecification.getComponentSpecificationList().isEmpty()) {
			requiredSkills = this.jobSpecification.getComponentSpecificationList()
			.stream().flatMap(data -> data.getWorkOrderRuleSkillList().stream())
			.map(data -> data.getSkillTypeCode())
			.distinct().map(data -> {
				TeamWorkerSkill skill = new TeamWorkerSkill();
				skill.setName(data);
				return skill;
			}).collect(Collectors.toList());
			requiredSkills.stream().forEach(data -> data.setLevelNum(Integer.toString(this.skillLevel)));
		} else {
			requiredSkills = workOrderBuilder.getWorkOrderSpecification().getWorkOrderRuleSkillList().stream()
					.map(data -> {
						TeamWorkerSkill skill = new TeamWorkerSkill();
						skill.setName(data.getSkillTypeCode());
						return skill;
					})
					.collect(Collectors.toList());
			requiredSkills.stream().forEach(data -> data.setLevelNum(Integer.toString(this.skillLevel)));
		}
	}

	
	static final HashMap<String, Integer> techmap = new HashMap<String, Integer> () {
		{ 
			put("SAT", 1);
			put("OTT", 2);
			put("GFAST", 3);
			put("GPON", 4);
			put("FIBRE", 5);
			put("WIRELESS", 6);
			put("COPPER_BONDED", 7);
			put("ETTS", 8);
			put("COPPER", 9);
			put("MICRO_DPU", 10);
		}
	};
	
	
}
