# How to implement CI/CD with AWS 

[//]: # (Todo: Write better info)
This is guide for implementing CI/CD to AWS Fargate with Spring Boot and database using GitHub actions.

## Preparations

### Actuator in Spring Boot app

- We use Actuator to expose endpoint that later tells if the service is up or not.
- Add following dependency to `build.gradle`:
```
    implementation 'org.springframework.boot:spring-boot-starter-actuator' 
```
- Configure Actuator to expose health endpoint by adding following lines to the application.yaml:
```
management:
  endpoints:
    web:
      exposure:
        include: health
```
- Now when you run the app you can check enpoint `http://localhost:8080/actuator/health`, you should get:
```json
{
  "status": "UP"
}
```

### Build with GitHub Actions

- First make sure that your app builds successfully on your local environment, run `./gradlew build`.
- Next in the project root create directories `.github/workflows` and then add `deploy.yml` to the `workflows` directory. 
- Add following lines to the `deploy.yml` (make sure that database and java-version values correspont to your own values):
```yaml
name: Deploy to AWS

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:

    runs-on: ubuntu-latest
    permissions:
      contents: read

    services:
      postgres:
        image: postgres:latest
        env:
          POSTGRES_PASSWORD: secret
          POSTGRES_USER: myuser
          POSTGRES_DB: stories_db
        ports:
          - '5432:5432'

    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    # Configure Gradle for optimal use in GitHub Actions, including caching of downloaded dependencies.
    # See: https://github.com/gradle/actions/blob/main/setup-gradle/README.md
    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@af1da67850ed9a4cedd57bfd976089dd991e2582 # v4.0.0

    - name: Build with Gradle Wrapper
      run: ./gradlew build
```

- Now you can push the changes to the GitHub and the build should complete successfully. (Above we use `main` branch, but of course you can change it if you want).

![img.png](cicd-guide-img/img1.png)

- Build with Gradle Wrapper also runs tests (if test fails, the build fails):

![img.png](cicd-guide-img/img2.png)


### Spring Cloud AWS Secrets Manager in Spring Boot app

- Because we are later hosting database in AWS and use **AWS Secrets Manager** for storing and managing database credentials, we can use Spring Cloud AWS Secrets Manager dependency
- Add dependency to `build.gradle` (For some reason the version number is needed in secrets-manager. Check the latest from Maven Central):
```
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-secrets-manager:3.4.0'
```
- Create a `application-aws.yaml` file.
    - Now you should have two application.yaml files: `application.yaml` and `application-aws.yaml`.
    - application.yaml is automatically used as "default" profile and application-aws is used as "aws" profile.
- Next add following configuration to the `application-aws.yaml`:
```yaml
spring:
  config:
    import: aws-secretsmanager:stories_db-secrets;

  datasource:
    username: ${username}
    password: ${password}
    url: jdbc:postgresql://${host}:${port}/${dbname}
```

- ‼️Later we are going to create secret in the AWS Secrets Manager service and the secret name that we define there must be excatly same that is configured here ("stories_db-secrets"). If you want to give secret different name (e.g. your app name is not stories) write the secret name that you want to use. 

- Now when make a push to the GitHub the build fails:
```
        Caused by:
        org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'secretsManagerClient' defined in class path resource [io/awspring/cloud/autoconfigure/config/secretsmanager/SecretsManagerAutoConfiguration.class]: Failed to instantiate [software.amazon.awssdk.services.secretsmanager.SecretsManagerClient]: Factory method 'secretsManagerClient' threw exception with message: Unable to load region from any of the providers in the chain software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain@1e295f7f: [software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider@713497cd: Unable to load region from system settings. Region must be specified either via environment variable (AWS_REGION) or  system property (aws.region)., software.amazon.awssdk.regions.providers.AwsProfileRegionProvider@63318b56: No region provided in profile: default, software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider@19e5e110: Unable to retrieve region information from EC2 Metadata service. Please make sure the application is running on EC2.]
```

- Like the log suggest, modify `deploy.yml` so that there is **AWS_REGION** as an evn parameter:

```yaml
    - name: Build with Gradle Wrapper
      env:
        AWS_REGION: eu-north-1
      run: ./gradlew build
```


## AWS VPC

- Go to VPC dashboard and select Create VPC:

![img.png](cicd-guide-img/img3.png)

![img.png](cicd-guide-img/img4.png)

- NAT-gateway is needed if your app needs to fetch external resources (Google Fonts, Stripe API, etc.) at runtime.
  - stories-app is simple app that doesn't call external resources so there is no need for NAT-gateway.

[//]: # (- ‼️If you are following [Deploy Applications on AWS Fargate &#40;ECS Tutorial + Hands-On Project&#41;]&#40;https://www.youtube.com/watch?v=C6v1GVHfOow&t=3337s&#41;: NAT-gateway is used atleast with the Lambda that rotates database secrets. In this guide **VPC endpoint** is used instead of NAT-gateway.)

![img.png](cicd-guide-img/img5.png)

#### Subnet names

- I use "stories" prefix here because it is the name of the app.
- Put region that you are using.

- **eu-north-1a** 
  - stories-public-subnet1-eu-north-1a
  - stories-app-subnet1-eu-north-1a
  - stories-data-subnet1-eu-north-1a

- **eu-north-1b**
  - stories-public-subnet2-eu-north-1b
  - stories-app-subnet2-eu-north-1b
  - stories-data-subnet2-eu-north-1b

![img.png](cicd-guide-img/img6.png)

- Select "Create VPC"

- ❗️When you create VPC **Elastic IP** is created. When you remove the VPC remember to release Elastic IP address. 


## AWS Security Group 

- Create following Security Groups **VPC -> Security Groups -> Create security group**

![img.png](cicd-guide-img/img7.png)

![img_1.png](cicd-guide-img/img8.png)

![img.png](cicd-guide-img/img9.png)


## AWS ECR

- Go to **Elastic Container Registry** and select "Create"

![img.png](cicd-guide-img/img10.png)

- After repository is created go to repository and select "View push commands"
- Folow those commands and push image to the repository. (Remember build your Spring app with lates changes before push).



## AWS RDS

- Navigate to **Aurora and RDS** service
- First we need to create **DB Subnet group**. Select "Create DB subnet group"

![img.png](cicd-guide-img/img11.png)

- Next we create Database. Go to "Databases" in **RDS** and select "Create database"

![img_1.png](cicd-guide-img/img12.png)

- We will configure Secrets Manager later.

![img_2.png](cicd-guide-img/img13.png)

![img_3.png](cicd-guide-img/img14.png)

- You can check from the "Additional configuration" that the port is correct. 

![img_4.png](cicd-guide-img/img15.png)

![img_5.png](cicd-guide-img/img16.png)

- In "Additional configuration" add "Initial database name".

![img_6.png](cicd-guide-img/img17.png)

- Then select "Create".

- You get notification where you can copy to master password. If you forget to do that, go to your db instance and select "Modify" and then set a new master password. 

## AWS Secrets Manager, Secret rotation with Lambda, VPC endpoint

- Create follown Security Group

![img.png](cicd-guide-img/img18.png)

- And then edit **stories-data-sg** security group by adding following inpound rule:

![img_1.png](cicd-guide-img/img19.png)

- Navigate to the AWS Secrets Manager service and select "Store a new secret"

![img_2.png](cicd-guide-img/img20.png)

- ❗️Make sure that "Secret name" matches excatly the one which is configured in the `application-aws.yaml`

![img_3.png](cicd-guide-img/img21.png)

![img_4.png](cicd-guide-img/img22.png)

![img_5.png](cicd-guide-img/img23.png)

![img_6.png](cicd-guide-img/img24.png)

- Navigate to the **Lambda** service and select the created Lambda function. 
- Then select "Configuration" and from there select "VPC" and then "Edit" and change Security Group for "stories-lambda-db-access-sg"

![img_7.png](cicd-guide-img/img25.png)

- Next create following Security Group:

![img_13.png](cicd-guide-img/img26.png)


- Navigate to **VPC** service, select "Endpoints" and "Create endpoint"

![img_13.png](cicd-guide-img/img27.png)

![img_14.png](cicd-guide-img/img28.png)

![img_15.png](cicd-guide-img/img29.png)


- After the enpoint is created, you can test rotation on the Secrets Manger service (Rotation -> Rotate secrets immediately).


## Target Group

- Navigate to **EC2** service and go to **Target Groups** and then select "Create target group"

![img.png](cicd-guide-img/img31.png)

![img_1.png](cicd-guide-img/img32.png)

Remove manually entered IP address.

![img_2.png](cicd-guide-img/img33.png)


## Application Load Balancer

- In **EC2** service go to the **Load Balancers** and select "Create load balancer" and then select "Application Load Balancer"

![img_3.png](cicd-guide-img/img34.png)

![img_4.png](cicd-guide-img/img35.png)

- Check the Summary and select "Create load balancer"

![img_5.png](cicd-guide-img/img36.png)


## IAM Roles & Policies

- Navigate to **IAM** service and go to the **Policies** and select "Create policy"

- Select **Secret Manager** as a Service:

![img_6.png](cicd-guide-img/img37.png)

- From the list check "GetSecretValue"

![img_7.png](cicd-guide-img/img38.png)

- To other tab open **Secret Manager** service, select secret that we created earlier and copy **Secret ARN**

- Go back to the policy creation page and select "Add ARNs"

- Paste copiod ARN (when you paste ARN other fields will be filled automatically) and then select "Add ARNs". After that select "Next".

![img_8.png](cicd-guide-img/img39.png)

![img_9.png](cicd-guide-img/img40.png)

- In the **IAM** service go to **Roles** and select "Create role"

![img_10.png](cicd-guide-img/img41.png)

![img_11.png](cicd-guide-img/img42.png)

![img_12.png](cicd-guide-img/img43.png)

![img_13.png](cicd-guide-img/img44.png)


## ECS Cluster

- Navigate to **Elastic Container Service**, go to **Clusters** and select "Create cluster"

![img.png](cicd-guide-img/img45.png)


## ECS Task definition

- In **Elastic Container Service** go to **Task definition** and select "Create new task definition"

- ❗️ Select for "Operating system/Architecture" "Linux/ARM64" if you used ARM64 for building image (e.g. Mac with M-series chip)

![img_1.png](cicd-guide-img/img46.png)

![img_2.png](cicd-guide-img/img47.png)

![img_3.png](cicd-guide-img/img48.png)


## ECS Service

- Navigate to the cluster we created earlier and there in the "Services" section select "Create"

![img_4.png](cicd-guide-img/img49.png)

![img_5.png](cicd-guide-img/img50.png)

![img_6.png](cicd-guide-img/img51.png)

![img_7.png](cicd-guide-img/img52.png)

![img_8.png](cicd-guide-img/img53.png)

- When you press "Create" the deployment will start, but it will fail. We need to create missing VPC endpoints and Security Group. 

## VPC endpoints

https://docs.aws.amazon.com/AmazonECR/latest/userguide/vpc-endpoints.html

![img_2.png](cicd-guide-img/img54.png)

![img.png](cicd-guide-img/img55.png)

![img_6.png](cicd-guide-img/img56.png)

![img_3.png](cicd-guide-img/img57.png)

![img_4.png](cicd-guide-img/img58.png)

![img_5.png](cicd-guide-img/img59.png)

![img_6.png](cicd-guide-img/img60.png)

![img_7.png](cicd-guide-img/img61.png)

![img_8.png](cicd-guide-img/img62.png)

![img_9.png](cicd-guide-img/img63.png)

![img_10.png](cicd-guide-img/img64.png)

![img_11.png](cicd-guide-img/img65.png)

![img_12.png](cicd-guide-img/img66.png)

- We also need to add Security group "stories-vpc-endpoint-app-sg" to the "stories-vpc-endpoint-secrets-manager" so navigate there and add select "Manage security groups":

![img_13.png](cicd-guide-img/img67.png)

- Navigate back to the **Amazon Elastic Container** service and select **Clusters** -> cluster created earlier -> service created earlier -> from "Update service" select "Force new deployment"
- When deployment is finnished, navigate to the **EC2** service and **Load Balancers** and select load balancer created earlier and copy "DNS name" and paste the address to the browser and make sure that your app is running. 


## Route 53 and Certificate Manager 

- Next we need a domain and if you don't have one yeat, just navigate to **Route53** service dashboard and go to "Register domain".
- If you already have a domain or your registeration is finished, navigate to **Certificate Manager** service and then select "Request":

![img.png](cicd-guide-img/img68.png)

![img_1.png](cicd-guide-img/img69.png)

- Next you need to select "Create records in Route 53" 

![img_2.png](cicd-guide-img/img70.png)

![img_3.png](cicd-guide-img/img71.png)

- Wait and check that certificate is validated and issued.

- Navigate to **EC2** service, then **Load Balancers** and select load balancer that we created earlier. Select "Add listener"

![img_4.png](cicd-guide-img/img72.png)

![img_5.png](cicd-guide-img/img73.png)

![img_6.png](cicd-guide-img/img74.png)

- Navigate **Security groups** and modify alb-sg by adding the https 443:

![img_7.png](cicd-guide-img/img75.png)

- Back to load balancer and on "HTTP:80" select "Edit listener" 

![img_8.png](cicd-guide-img/img76.png)

![img_9.png](cicd-guide-img/img77.png)

- Back to **Route 53** and in your **Hosted zone details**  select "Create record"

![img_10.png](cicd-guide-img/img78.png)

![img_11.png](cicd-guide-img/img79.png)

- It might take few a minutes but now your app should be awailable in both your-domain.com and www&#46;your-domain.com

![img_12.png](cicd-guide-img/img80.png)

![img_14.png](cicd-guide-img/img81.png)












