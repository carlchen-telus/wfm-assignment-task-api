package com.telus.workforcemgmt.assignment.builder;

import org.apache.commons.lang3.ArrayUtils;

import com.telus.workforcemgmt.wil3.dto.Component;
import com.telus.workforcemgmt.wil3.dto.ComponentList;
import com.telus.workforcemgmt.wil3.dto.WorkOrder;

public class WorkOrderComponentUtils {
		
	public static boolean hasOnlyRackWork(ComponentList complist) {
		if (complist == null || complist.getComponent().size() ==0) return false;
		for (Component e : complist.getComponent()) {
			if (!ConstantCodes.JOBTYPE_RACKWORK.equals(e.getJobTypeCd())){
				return false;
			}
		}
		return true;
	}

	public static boolean hasGPONComponent(ComponentList complist) {
		if (complist == null || complist.getComponent().size() ==0) return false;
		for (Component e : complist.getComponent()) {
			if (ConstantCodes.TECHNOLOGY_GPON.equals(e.getProductTechnologyCd())){
				return true;
			}
		}
		return false;
	}

	public static boolean hasGPONTechnology(WorkOrder order) {
		return order.getProductTechnologyCd() != null &&
		ConstantCodes.TECHNOLOGY_GPON.equals(order.getProductTechnologyCd());
	}


	//CSD-10882
	public final static String[] FSA_JOBTYPES = {"VOICE","HS","IPTV"};


	public static boolean hasVoiceDSLTTV(WorkOrder order) {
		ComponentList complist = order.getComponentList();
		if ((complist != null) && (complist.getComponent().size() > 0)) {
			for (Component e : complist.getComponent()) {
				if (ArrayUtils.contains(FSA_JOBTYPES, e.getJobTypeCd())){
					return true;
				}
			}			
		}
		
		if (order.getJobTypeCd() != null) {
			if(ArrayUtils.contains(FSA_JOBTYPES, order.getJobTypeCd() )) {
				return true;	
			}			
		}
		
		return false;
	}
}