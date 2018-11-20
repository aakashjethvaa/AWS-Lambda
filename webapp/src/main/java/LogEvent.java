import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {
    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "csye6225";
    private Regions REGION = Regions.US_EAST_1;

    AmazonDynamoDBClient clientdb = new AmazonDynamoDBClient();
    AmazonSimpleEmailService client1 = new AmazonSimpleEmailServiceClient();
    ListIdentitiesRequest request1 = new ListIdentitiesRequest();
    ListIdentitiesResult response = client1.listIdentities(request1);
    String FROM = response.toString().substring(14, response.toString().length() - 3) + "";

    static final String SUBJECT = "Reset Your Password";
    static final String TEXTBODY = "This email was sent through Amazon SES using the AWS SDK for Java.";

    public Object handleRequest(SNSEvent request, Context context) {

        this.initDynamoDbClient();
        Table table = this.dynamoDb.getTable(DYNAMODB_TABLE_NAME);

        // Update predefined TileToLiveSpecification for the table if not defined already
        final UpdateTimeToLiveRequest ttlReq = new UpdateTimeToLiveRequest();
        ttlReq.setTableName(DYNAMODB_TABLE_NAME);

        // Set TimeToLiveSpecification attribute and boolean status set to true
        final TimeToLiveSpecification ttlSpec = new TimeToLiveSpecification();
        ttlSpec.setAttributeName("TTL");
        ttlSpec.setEnabled(true);
        ttlReq.withTimeToLiveSpecification(ttlSpec);

        String uuid = String.valueOf(UUID.randomUUID());
        String username = request.getRecords().get(0).getSNS().getMessage();
        Long ttl = Instant.now().getEpochSecond() + 20 * 60;

        String[] parts = FROM.split(",");
        String part1 = "no-reply@"+parts[0];
        Item i = table.getItem("ID", username);
        if (i == null) {
            Item item = new Item().withPrimaryKey("ID", username).withString("token", uuid).withLong("TTL", ttl);
            PutItemOutcome outcome = table.putItem(item);
            final String HTMLBODY = "<h2>Password Reset Request</h2><p>Click on the link below to reset your password." +
                    "</p><p>If you did not apply for the reset then we cannot do anything!</p>" +
                    "<p>http://" + part1 + "/reset?email=" + username + "&token=" + uuid;
            try {
                AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
                SendEmailRequest req = new SendEmailRequest().withDestination(new Destination().withToAddresses(username)).withMessage(new Message().withBody(new Body()
                        .withHtml(new Content().withCharset("UTF-8").withData(HTMLBODY)).withText(new Content().withCharset("UTF-8").withData(TEXTBODY)))
                        .withSubject(new Content().withCharset("UTF-8").withData(SUBJECT))).withSource(part1);
                client.sendEmail(req);
                System.out.println("Email sent!");
            } catch (Exception ex) {
                System.out.println("The email was not sent. Error message: "+ ex.getMessage());
            }
        } else {
            return null;
        }

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation started: " + timeStamp);
        context.getLogger().log("Dynamo db name: " + DYNAMODB_TABLE_NAME);
        context.getLogger().log("Number of Records: " + (request.getRecords().size()));
        context.getLogger().log("Record message: " + request.getRecords().get(0).getSNS().getMessage());
        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);
        return null;
    }

    public void initDynamoDbClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(REGION));
        this.dynamoDb = new DynamoDB(client);
    }
}

