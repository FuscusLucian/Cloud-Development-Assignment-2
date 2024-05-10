# Welcome to your CDK Java project!

## Setup
The application is intended to be used with Java 21, but should work with pretty much any version. 
That can be downloaded here:
* https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html

### Manually configure SDK
If you are not using a more powerful editor which automatically configures the correct JDK, 
or if you want to manually configure it, it can be done like this:

Unzip the SDK in a location like C:\jdk\jdk11.0.13_8

You will then have to configure the java path in the file `gradle.properties`

## Linux/Mac OS specifics
The CDK is configured to run gradle with the gradle wrapper.
This might cause issues on linux/mac operating systems.
If such a problem does occur, try changing cdk.json to use `./gradlew` and also by running `sudo chmod +x gradlew` first.
It is also a possibility with problems due to different line endings on linux/mac and windows.
That should be fixed with `sed -i.bak 's/\r$//' gradlew`

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

 * `gradle build`    compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation
 * `cdk destroy`     Destroy all created stacks
 
Enjoy!
