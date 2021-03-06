package org.oliot.epcis.service.capture.secured;

import javax.servlet.ServletContext;

import org.json.JSONObject;
import org.oliot.epcis.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ServletContextAware;

/**
 * Copyright (C) 2014-2016 Jaewook Byun
 *
 * This project is part of Oliot open source (http://oliot.org). Oliot EPCIS
 * v1.2.x is Java Web Service complying with Electronic Product Code Information
 * Service (EPCIS) v1.2.
 *
 * @author Jaewook Byun, Ph.D student
 * 
 *         Korea Advanced Institute of Science and Technology (KAIST)
 * 
 *         Real-time Embedded System Laboratory(RESL)
 * 
 *         bjw0829@kaist.ac.kr, bjw0829@gmail.com
 */

@Controller
@RequestMapping("/DeleteEPCIS")
public class DeleteEPCIS implements ServletContextAware {

	@Autowired
	ServletContext servletContext;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ResponseEntity<?> asyncDelete(String inputString) {
		ResponseEntity<?> result = del(inputString, null, null, null);
		return result;
	}

	/**
	 * del
	 * 
	 * @creator Jaehee Ha lovesm135@kaist.ac.kr created 2017/02/07
	 * @param inputString
	 * @param gcpLength
	 * @return ResponseEntity<?>
	 */
	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<?> del(@RequestBody String inputString, @RequestParam(required = false) Integer gcpLength,
			@RequestParam(required = false) String userID, @RequestParam(required = false) String accessToken) {
		JSONObject retMsg = new JSONObject();

		// =============================================================================================
		/* jaeheeHa3 AC_delete repository */
		// This method must be conducted by Access Control Web GUI.
		// This method must not be conducted by URL based Get request.

		// url of ac_api server
		String quri = "http://" + Configuration.ac_api_address + "/user/" + userID + "/epcis/" + "bar1_epcis"
				+ "/possess";

		// query to ac_api server
		String qurlParameters = "";
		String query_result = Configuration.query_access_relation(quri, accessToken, qurlParameters);

		// for debug, erase after implementing.
		Configuration.logger.info(query_result);
		query_result = query_result.replaceAll("[\"{} ]", "").split(":")[1];

		boolean pass = (query_result.equals("yes")) ? true : false;

		if (!pass) {
			return new ResponseEntity<>("No!", HttpStatus.BAD_REQUEST);
		}

		Configuration.dropMongoDB();

		Configuration.logger.info(" EPCIS Repository : Deleted ");

		if (retMsg.isNull("error") == true)
			return new ResponseEntity<>(retMsg.toString(), HttpStatus.OK);
		else
			return new ResponseEntity<>(retMsg.toString(), HttpStatus.BAD_REQUEST);
	}

}
