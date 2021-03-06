package org.oliot.epcis.serde.mongodb;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.oliot.epcis.configuration.Configuration;
import org.oliot.epcis.service.registry.DiscoveryServiceAgent;
import org.oliot.model.epcis.AggregationEventExtensionType;
import org.oliot.model.epcis.AggregationEventType;
import org.oliot.model.epcis.BusinessLocationType;
import org.oliot.model.epcis.BusinessTransactionListType;
import org.oliot.model.epcis.BusinessTransactionType;
import org.oliot.model.epcis.EPC;
import org.oliot.model.epcis.EPCISEventExtensionType;
import org.oliot.model.epcis.EPCListType;
import org.oliot.model.epcis.QuantityElementType;
import org.oliot.model.epcis.QuantityListType;
import org.oliot.model.epcis.ReadPointType;

import static org.oliot.epcis.serde.mongodb.MongoWriterUtil.*;

/**
 * Copyright (C) 2014-2016 Jaewook Byun
 *
 * This project is part of Oliot (oliot.org), pursuing the implementation of
 * Electronic Product Code Information Service(EPCIS) v1.1 specification in
 * EPCglobal.
 * [http://www.gs1.org/gsmp/kc/epcglobal/epcis/epcis_1_1-standard-20140520.pdf]
 * 
 *
 * @author Jaewook Jack Byun, Ph.D student
 * 
 *         Korea Advanced Institute of Science and Technology (KAIST)
 * 
 *         Real-time Embedded System Laboratory(RESL)
 * 
 *         bjw0829@kaist.ac.kr, bjw0829@gmail.com
 */

public class AggregationEventWriteConverter {

	public BsonDocument convert(AggregationEventType aggregationEventType, Integer gcpLength) {
		BsonDocument dbo = new BsonDocument();
		// Base Extension
		if (aggregationEventType.getBaseExtension() != null) {
			EPCISEventExtensionType baseExtensionType = aggregationEventType.getBaseExtension();
			BsonDocument baseExtension = getBaseExtensionObject(baseExtensionType);
			dbo.put("baseExtension", baseExtension);
		}
		// Event Time
		if (aggregationEventType.getEventTime() != null)
			dbo.put("eventTime",
					new BsonInt64(aggregationEventType.getEventTime().toGregorianCalendar().getTimeInMillis()));
		// Event Time Zone
		if (aggregationEventType.getEventTimeZoneOffset() != null)
			dbo.put("eventTimeZoneOffset", new BsonString(aggregationEventType.getEventTimeZoneOffset()));
		// Record Time : according to M5
		GregorianCalendar recordTime = new GregorianCalendar();
		long recordTimeMilis = recordTime.getTimeInMillis();
		dbo.put("recordTime", new BsonInt64(recordTimeMilis));

		// Parent ID
		if (aggregationEventType.getParentID() != null) {
			dbo.put("parentID",
					new BsonString(MongoWriterUtil.getInstanceEPC(aggregationEventType.getParentID(), gcpLength)));
		}
		// Child EPCs
		if (aggregationEventType.getChildEPCs() != null) {
			EPCListType epcs = aggregationEventType.getChildEPCs();
			List<EPC> epcList = epcs.getEpc();
			BsonArray epcDBList = new BsonArray();

			for (int i = 0; i < epcList.size(); i++) {
				BsonDocument epcDB = new BsonDocument();
				epcDB.put("epc", new BsonString(MongoWriterUtil.getInstanceEPC(epcList.get(i).getValue(), gcpLength)));
				epcDBList.add(epcDB);
			}
			dbo.put("childEPCs", epcDBList);
		}
		// Action
		if (aggregationEventType.getAction() != null)
			dbo.put("action", new BsonString(aggregationEventType.getAction().name()));
		// Biz Step
		if (aggregationEventType.getBizStep() != null)
			dbo.put("bizStep", new BsonString(aggregationEventType.getBizStep()));
		// Disposition
		if (aggregationEventType.getDisposition() != null)
			dbo.put("disposition", new BsonString(aggregationEventType.getDisposition()));
		// ReadPoint
		if (aggregationEventType.getReadPoint() != null) {
			ReadPointType readPointType = aggregationEventType.getReadPoint();
			BsonDocument readPoint = getReadPointObject(readPointType, gcpLength);
			dbo.put("readPoint", readPoint);
		}
		// BizLocation
		if (aggregationEventType.getBizLocation() != null) {
			BusinessLocationType bizLocationType = aggregationEventType.getBizLocation();
			BsonDocument bizLocation = getBizLocationObject(bizLocationType, gcpLength);
			dbo.put("bizLocation", bizLocation);
		}

		if (aggregationEventType.getBizTransactionList() != null) {
			BusinessTransactionListType bizListType = aggregationEventType.getBizTransactionList();
			List<BusinessTransactionType> bizList = bizListType.getBizTransaction();

			BsonArray bizTranList = getBizTransactionObjectList(bizList);
			dbo.put("bizTransactionList", bizTranList);
		}

		// Vendor Extension
		if (aggregationEventType.getAny() != null) {
			List<Object> objList = aggregationEventType.getAny();
			BsonDocument map2Save = getAnyMap(objList);
			if (map2Save != null && map2Save.isEmpty() == false)
				dbo.put("any", map2Save);

		}

		// Extension
		if (aggregationEventType.getExtension() != null) {
			AggregationEventExtensionType aee = aggregationEventType.getExtension();
			BsonDocument extension = getAggregationEventExtensionObject(aee, gcpLength);
			dbo.put("extension", extension);

		}

		if (Configuration.isServiceRegistryReportOn == true) {
			HashSet<String> candidateSet = getCandidateEPCSet(aggregationEventType);
			DiscoveryServiceAgent dsa = new DiscoveryServiceAgent();
			int updatedEPCCount = dsa.registerEPC(candidateSet);
			Configuration.logger.info(updatedEPCCount + " EPC(s) are registered to Discovery Service");
		}
		return dbo;
	}

	private HashSet<String> getCandidateEPCSet(AggregationEventType aggregationEventType) {
		HashSet<String> candidateSet = new HashSet<String>();

		// Parent ID
		if (aggregationEventType.getParentID() != null) {
			candidateSet.add(aggregationEventType.getParentID());
		}
		// Child EPCs
		if (aggregationEventType.getChildEPCs() != null) {
			EPCListType epcs = aggregationEventType.getChildEPCs();
			List<EPC> epcList = epcs.getEpc();
			for (int i = 0; i < epcList.size(); i++) {
				candidateSet.add(epcList.get(i).getValue());
			}
		}
		// Extension
		if (aggregationEventType.getExtension() != null) {
			AggregationEventExtensionType aee = aggregationEventType.getExtension();
			if (aee.getChildQuantityList() != null) {
				QuantityListType qetl = aee.getChildQuantityList();
				List<QuantityElementType> qetList = qetl.getQuantityElement();
				for (int i = 0; i < qetList.size(); i++) {
					QuantityElementType qet = qetList.get(i);
					if (qet.getEpcClass() != null)
						candidateSet.add(qet.getEpcClass().toString());
				}
			}
		}
		return candidateSet;
	}
}
