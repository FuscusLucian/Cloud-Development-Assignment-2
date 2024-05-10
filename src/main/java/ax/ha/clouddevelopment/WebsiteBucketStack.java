package ax.ha.clouddevelopment;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.AnyPrincipal;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.BucketWebsiteTarget;
import software.amazon.awscdk.services.s3.BucketPolicy;
import software.amazon.awscdk.services.s3.BucketPolicyProps;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.BucketDeploymentProps;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.s3.IBucket;


import java.util.Arrays;
import java.util.Map;

/**
 * The WebsiteBucketStack class represents a collection of AWS resources that are grouped together
 * to form the infrastructure needed for hosting a static website. This class handles the creation
 * and configuration of resources such as an S3 bucket for website storage, security policies for
 * restricted access, DNS settings for easy URL access, and a CDN distribution to improve global
 * access speeds. It extends the AWS CDK Stack, allowing all these components to be managed together
 * as a single unit in AWS CloudFormation, which makes deploying and updating the resources simpler.
 * <a href="https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_cloudfront-readme.html">...</a>
 */
public class WebsiteBucketStack extends Stack {

    //Defines the S3 bucket with website configuration
    public WebsiteBucketStack(final Construct scope,
                              final String id,
                              final StackProps props,
                              final String groupName) {
        super(scope, id, props);

        Bucket websiteBucket = new Bucket(this, groupName + "Bucket", BucketProps.builder()
                .bucketName(groupName + ".cloud-ha.com")
                .websiteIndexDocument("index.html")
                .publicReadAccess(false)
                .build());

        // Basically we run our bucket through all of these various AWS features/functions that get added,
        // there's a hella of a lot more of em' but the assignment just needed these (for now?)
        // IP Whitelisting
        setupBucketPolicy(websiteBucket);
        // Deployment of Bucket content
        setupBucketDeployment(websiteBucket);
        // Output the website URL
        setupCfnOutput(groupName, websiteBucket);
        // Setup Hosted Zone and DNS
        setupRoute53(groupName, websiteBucket);
        // Setup CloudFront
        setupCloudFront(websiteBucket);
    }


    /**
     * Sets up the IP whitelisting bucket policy.
     * @param websiteBucket The S3 bucket to which the policy will be attached.
     * Configures a bucket policy to restrict access to the S3 bucket based on IP whitelisting.
     * This policy allows only specific IP addresses to retrieve objects from the S3 bucket,
     * enhancing the security by preventing unauthorized access.
     */
    private void setupBucketPolicy(Bucket websiteBucket) {

        // IP Whitelisting
        BucketPolicy bucketPolicy = new BucketPolicy(this, "BucketPolicy", BucketPolicyProps.builder()
                .bucket(websiteBucket) // sets the bucket to which this policy will be attached to
                .build());

        PolicyStatement statement = PolicyStatement.Builder.create()
                // Specifies who is allowed to do the actions specified , it's a list cause it expects one
                .principals(Arrays.asList(new AnyPrincipal()))
                // specifies which actions are allowed or denied
                .actions(Arrays.asList("s3:GetObject")) // lets specific users/service retrieve objects out of our bucket
                // indetifies the specific resources to which the policy applies to
                .resources(Arrays.asList(websiteBucket.getBucketArn() + "/*")) // /* just means to apply it to all objects in the bucket
                // specifies the circumstance under which the policy statmenet applies, in this case the IP
                .conditions(Map.of("IpAddress", Map.of("aws:SourceIp", "79.133.25.93/32")))
                // Deterimnes whether the aciton is allowed or denied when the conditions we set are met
                .effect(Effect.ALLOW)
                .build();

        // adds the statement that got made to the policy document of BucketPolicy
        bucketPolicy.getDocument().addStatements(statement);
    }

    /**
     * Deploys website content to the specified S3 bucket.
     * @param websiteBucket The S3 bucket where the website content will be deployed.
     * Deploys the static content from a specified local directory to the S3 bucket.
     * This setup automates the process of uploading website content, ensuring that the
     * S3 bucket always has the latest version of the website ready to be served to users.
     */
    private void setupBucketDeployment(Bucket websiteBucket) {
        new BucketDeployment(this, "BucketDeployment", BucketDeploymentProps.builder()
                .sources(Arrays.asList(Source.asset("./src/main/resources/website")))
                .destinationBucket(websiteBucket)
                .build());
    }

    /**
     * Creates a CloudFormation output for the website URL.
     * @param groupName The group name used for naming the output.
     * @param websiteBucket The S3 bucket from which the website URL is derived.
     * Creates a visible output in the AWS CloudFormation console showing the website's URL.
     * This output helps anyone using the console to quickly find and access the website link
     * directly after the stack is deployed. It's like putting a label on a button that tells
     * you what it does, making it easy to share and verify the site's address.
     */
    private void setupCfnOutput(String groupName, Bucket websiteBucket) {
        // Outputs the website URL
        new CfnOutput(this, "websiteBucketOutput", CfnOutputProps.builder()
                .description(String.format("URL of the bucket assignment: %s", groupName)) // "%s" is where groupName will be inserted
                .value(websiteBucket.getBucketWebsiteUrl())
                .exportName(groupName + "-assignment2-url")
                .build());
    }

    /**
     * Sets up Route 53 DNS records for the website.
     * @param groupName The group name used for DNS record naming.
     * @param websiteBucket The S3 bucket to be linked via DNS.
     * Configures DNS settings for the website using AWS Route 53, linking a human-readable domain name
     * to the S3 bucket. This makes the website accessible via a custom domain (e.g., www.example.com),
     * rather than just an S3 endpoint, improving user experience and branding.
     */
    private void setupRoute53(String groupName, Bucket websiteBucket) {

        IHostedZone hostedZone = HostedZone.fromHostedZoneAttributes(this, "hostedZone", HostedZoneAttributes.builder()
                .hostedZoneId("Z0413857YT73A0A8FRFF") //  aws route53 list-hosted-zones , Id: /hostedzone/Z0413857YT73A0A8FRFF
                .zoneName("cloud-ha.com")
                .build());

        new ARecord(this, "AliasRecord", ARecordProps.builder()
                .zone(hostedZone)
                .recordName(groupName + ".cloud-ha.com")
                .target(RecordTarget.fromAlias(new BucketWebsiteTarget(websiteBucket)))
                .build());
    }
        /**
         * Configures and deploys a CloudFront distribution for the specified S3 bucket.
         * @param websiteBucket The S3 bucket that will serve as the origin for the CloudFront distribution.
        What does it do ? Distributes our Bucket content from appropriate nearby server sources
         In Sweden and want to get on the site ? Uses Stockholm servers, in France ? Paris server.
         Making the distribution much faster, instead of it all coming from just one possible source that could be far away
         oh and lets us geo-block places
         */
    public void setupCloudFront(IBucket websiteBucket) {
        CloudFrontWebDistribution distribution = new CloudFrontWebDistribution(this, "LucianDistribution", CloudFrontWebDistributionProps.builder()
                //Configures from where CloudFront will fetch the content, so our S3 bucket we made
                .originConfigs(Arrays.asList(
                        SourceConfiguration.builder()
                                .s3OriginSource(S3OriginConfig.builder()
                                        .s3BucketSource(websiteBucket) // sets the S3 Bucket as our source
                                        .build())
                                // sets how it'll behave when a user tries to accesses it, set to default
                                .behaviors(Arrays.asList(Behavior.builder()
                                        .isDefaultBehavior(true)
                                        .build()))
                                .build()))
                .priceClass(PriceClass.PRICE_CLASS_100) // Restrict to Europe,USA,Canada,Israel
                .geoRestriction(GeoRestriction.denylist("CN")) // China blocked
                .build());

        new CfnOutput(this, "DistributionId", CfnOutputProps.builder()
                .value(distribution.getDistributionId())
                .build());
    }

}
/*
 Why so many asLists ? AWS CDK code and other Java applications are  driven by its utility in creating immutable fixed-size lists
 Basically AWS CDK just really likes lists ans asLists create these simple immutable lists we store various things into. */