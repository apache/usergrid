package org.apache.usergrid.apm.service.service;

import junit.framework.TestCase;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.AWSUtil;
import org.apache.usergrid.apm.service.ApplicationServiceImpl;
import org.apache.usergrid.apm.service.DeploymentConfig;
import org.apache.usergrid.apm.service.ServiceFactory;

/**
 * This is more like a utility class to do some AWS cleanup/creation such as 
 * create and delete a SQS queue. Not really a jUnit Test. All of these will be moved to AWSUtil
 * @author prabhat
 *
 */
public class AwsUtilTest  extends TestCase {

   protected void setup() throws Exception {
      super.setUp();
   }

   protected void tearDown() throws Exception {
      super.tearDown();
   }
   
   public void testConfigLoad() {
      String env = DeploymentConfig.geDeploymentConfig().getEnvironment();
      assertTrue("Was not able to read environment properly", !env.equals("${aws.environment}"));
   }
   
 
   
   public void testDeleteCreateSQSQueueForProfile () throws InterruptedException {
      ApplicationServiceImpl appService = (ApplicationServiceImpl) ServiceFactory.getApplicationService();
      AmazonSQSClient sqsClient = appService.getSqsClient();
      App model = new App();
      model.setInstaOpsApplicationId(new Long (10));
      appService.createSQSQueue(model.getInstaOpsApplicationId().toString());   
      
      //Thread.sleep(5000);
      DeleteQueueRequest dqr = new DeleteQueueRequest(AWSUtil.formFullQueueUrl(model.getInstaOpsApplicationId().toString()));
      //Following deletes the q even if it's not empty which is what we want during test
      sqsClient.deleteQueue(dqr);
   }

}
