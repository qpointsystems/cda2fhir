package tr.com.srdc.cda2fhir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import tr.com.srdc.cda2fhir.transform.CCDTransformerImpl;
import tr.com.srdc.cda2fhir.util.FHIRUtil;
import tr.com.srdc.cda2fhir.util.IdGeneratorEnum;
import tr.com.srdc.cda2fhir.validation.ValidatorImpl;

public class IntegrationTest {
	static String hapiURL = "http://localhost:1137";
	static String serverBase = hapiURL + "/fhir";
	static FhirContext ctx;
	static IGenericClient client;
	static CCDTransformerImpl ccdTransformer;
	static Logger logger;

	@BeforeClass
	public static void init() throws IOException {

		// Load MDHT CDA packages. Otherwise ContinuityOfCareDocument and similar
		// documents will not be recognised.
		// This has to be called before loading the document; otherwise will have no
		// effect.
		CDAUtil.loadPackages();
		ctx = FhirContext.forDstu3();
		client = ctx.newRestfulGenericClient(serverBase);
		ccdTransformer = new CCDTransformerImpl(IdGeneratorEnum.COUNTER);
		logger = LoggerFactory.getLogger(ValidatorImpl.class);
	}

	@ClassRule
	public static DockerComposeRule docker = DockerComposeRule.builder().file("src/test/resources/docker-compose.yaml")
			.waitingForService("hapi", HealthChecks.toRespondOverHttp(8080, (port) -> port.inFormat(hapiURL))).build();

	@Test
	public void rakiaIntegration() throws Exception {
		String sourceName = "Cerner/Person-RAKIA_TEST_DOC00001 (1).XML";
		// create transaction bundle from ccda bundle
		Bundle transactionBundle = ccdTransformer.transformDocument("src/test/resources/" + sourceName,
				BundleType.TRANSACTION, null);

		// print pre-post bundle
		FHIRUtil.printJSON(transactionBundle, "src/test/resources/output/rakia_bundle.json");

		// Send transaction bundle to server.
		Bundle resp = client.transaction().withBundle(transactionBundle).execute();

		for (BundleEntryComponent entry : resp.getEntry()) {
			BundleEntryResponseComponent entryResp = entry.getResponse();

			Assert.assertEquals("201 Created", entryResp.getStatus());
		}

		Bundle patientResults = (Bundle) client.search().forResource(Patient.class).prettyPrint().execute();

		Bundle practitionerResults = (Bundle) client.search().forResource(Practitioner.class).prettyPrint().execute();

		Bundle medicationResults = (Bundle) client.search().forResource(Medication.class).prettyPrint().execute();

		Assert.assertEquals(1, patientResults.getTotal());
		Assert.assertEquals(18, practitionerResults.getTotal());
		Assert.assertEquals(14, medicationResults.getTotal());

	}

}
