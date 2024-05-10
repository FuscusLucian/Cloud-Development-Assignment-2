package ax.ha.clouddevelopment;


import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public final class S3BucketApp {

    private static final String GROUP_NAME = "lucian-svelic";

    private static final String AWS_ACCOUNT_ID = "292370674225";

    public static void main(final String[] args) {
        final App app = new App();

        if (GROUP_NAME.equals("UNSET")) {
            throw new IllegalArgumentException("You must set the GROUP_NAME variable in S3BucketApp");
        }

        new WebsiteBucketStack(app, GROUP_NAME + "-s3-assignment", new StackProps.Builder()
                .env(Environment.builder()
                        .account(AWS_ACCOUNT_ID)
                        .region("eu-north-1")
                        .build())
                .build(), GROUP_NAME);
        app.synth();
    }
}
