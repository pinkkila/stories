# How to implement... 

[//]: # (Todo: Write better info)
CI/CD pipeline 
AWS Elastic Container Service (ECS).
GitHub Actions
Spring Boot
IAAS Academy's YouTube video "Deploy Applications on AWS Fargate (ECS Tutorial + Hands-On Project)" is major source. 

## Preparations

- First, verify that your application builds correctly in your local environment. Run:

```
./gradlew build
```


### Actuator

- We will use Spring Boot Actuator to expose a health check endpoint. This will later be used by AWS to verify that the service is running.
- Add the following dependency to your `build.gradle:

```
    implementation 'org.springframework.boot:spring-boot-starter-actuator' 
```

- Configure Actuator to expose the health endpoint by adding these lines to your `application.yaml:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
```

- Start your application and open the endpoint in a browser or with curl:

```
http://localhost:8080/actuator/health
```

You should see a JSON response like this:

```json
{
  "status": "UP"
}
```

### Spring Cloud AWS Secrets Manager

- Since we will later host our database on AWS and store the credentials in AWS Secrets Manager, we can use the Spring Cloud AWS Secrets Manager dependency to load them automatically.
- Add the dependency to `build.gradle` (For some reason the version number is needed in this particular dependency. Check out the latest from [Maven Central](https://mvnrepository.com/artifact/io.awspring.cloud/spring-cloud-aws-starter-secrets-manager)):

```
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-secrets-manager:3.4.0'
```

- Create a new configuration file named `application-aws.yaml`.
- You should now have two application files: application.yaml (the default profile) and application-aws.yaml (the aws profile).
- Add the following configuration to `application-aws.yaml`:

```yaml
spring:
  config:
    import: aws-secretsmanager:stories_db-secrets;

  datasource:
    username: ${username}
    password: ${password}
    url: jdbc:postgresql://${host}:${port}/${dbname}
```

- ⚠️ Important: Later, when creating the secret in AWS Secrets Manager, the secret name must exactly match what you configure here ('stories_db-secrets'). If you want to use a different name (for example, if your app isn’t called stories), update the name accordingly.



## AWS VPC

- Navigate to the **VPC** dashboard and select "Create VPC":

![img.png](cicd-guide-img/img3.png)

![img.png](cicd-guide-img/img4.png)

#### NAT Gateway

- [AWS docs](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-nat-gateway.html): *"You can use a NAT gateway so that instances in a private subnet can connect to services outside your VPC but external services can't initiate a connection with those instances."*
- Consider using a NAT Gateway if your application running in a private subnet needs to make outbound server-side connections to external services (for example, calling an external API).
- Client-side requests, such as those from a browser (e.g., Google Fonts or other external APIs called from the frontend), do not require a NAT Gateway because they use the client’s network connection.
- For stories-app, outbound server-side connections are not required, so a NAT Gateway is not selected here. Instead, VPC endpoints are later configured and used to provide private access to AWS services.


![img.png](cicd-guide-img/img5.png)

#### Subnet naming

- In this guide, subnets are named with the stories prefix, since the application is called stories. You can replace this prefix with your own application name to keep your resources organized.
- Inlude the AWS region and availability zone in the name so it's easier to identify where each subnet is located.

- Example naming convention:
  - **eu-north-1a** 
    - stories-public-subnet1-eu-north-1a
    - stories-app-subnet1-eu-north-1a
    - stories-data-subnet1-eu-north-1a

  - **eu-north-1b**
    - stories-public-subnet2-eu-north-1b
    - stories-app-subnet2-eu-north-1b
    - stories-data-subnet2-eu-north-1b

![img.png](cicd-guide-img/img6.png)




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


## OIDC provider

- Navigate to the **IAM** service and then **Identity providers** and select "Add provider" 

![img.png](cicd-guide-img/img82.png)

- Go to the created Identity provider and select "Assign role"

![img_1.png](cicd-guide-img/img83.png)

![img_2.png](cicd-guide-img/img84.png)

![img_3.png](cicd-guide-img/img85.png)

![img_4.png](cicd-guide-img/img86.png)

![img_5.png](cicd-guide-img/img87.png)

- Navigate in **IAM** service to the **Policies** section and select "Create policy"
- Select "JSON" and copy and paste the following json:


```json
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Sid": "ManageTaskDefinitions",
			"Effect": "Allow",
			"Action": [
				"ecs:DescribeTaskDefinition",
				"ecs:RegisterTaskDefinition"
			],
			"Resource": "*"
		},
		{
			"Sid": "DeployService",
			"Effect": "Allow",
			"Action": [
				"ecs:DescribeServices",
				"ecs:UpdateService"
			],
			"Resource": [
                "Place your service ARN here! (remove this line❗️)",
				"serviceArn........"
			]
		},
		{
			"Sid": "PassRolesInTaskDefinition",
			"Effect": "Allow",
			"Action": [
				"iam:PassRole"
			],
			"Resource": [
                "Place your executionRoleArn and taskRoleArn here! (remove this line❗️)",
				"executionRoleArn......",
                "taskRoleArn......"
			]
		}
	]
}
```

- Open another tab and navigate to **Elastic Container Serivice** dasboard and then go to the cluster we made earlier and from the "Service" section copy your service's ARN and place that to the policy json:

![img_6.png](cicd-guide-img/img88.png)

- Navigate to the Task definition we created earlier and view its JSON. Copy the value of the "executionRoleArn" and place it the policy json.
- ‼️ According the [amazon-ecs-deploy-task-definition README](https://github.com/aws-actions/amazon-ecs-deploy-task-definition?tab=readme-ov-file#permissions) you should add both *executionRoleArn* and *taskRoleArn* but in my case those are identical, so I have only placed one ARN.

![img_7.png](cicd-guide-img/img89.png)

![img_8.png](cicd-guide-img/img90.png)

- Go back to the **Roles** and go to the role that we creted earlier and select "Add permission" and "Attach policies"

![img_9.png](cicd-guide-img/img91.png)


## GitHub Secrets

- In your GitHub reposiroty navigate to the "Settings" and select "Secrets and variables". Then click "New repository secret"

![img.png](cicd-guide-img/img92.png)


## GitHub Actions

- Next in the project root create directories `.github/workflows` and then add `deploy.yml` to the `workflows` directory.
- Add following lines to the `deploy.yml` 
  - Set your *env* values and make sure that *database* and *java-version* values also correspont to your project values.
  - In *step* "Build, tag, and push image to Amazon ECR" check the comment. 

```yaml
name: CI/CD Build & Deploy to ECS

on:
  push:
    branches: [ "main" ]

env:
  AWS_REGION: eu-north-1                                      # set this to AWS region that you are using, e.g. eu-north-1
  ECR_REPOSITORY: stories                                     # set this to your Amazon ECR repository name
  ECS_SERVICE: stories-task-definition-service-kcj0t5mk       # set this to your Amazon ECS service name
  ECS_CLUSTER: stories-ecs-cluster                            # set this to your Amazon ECS cluster name
  CONTAINER_NAME: stories-container                           # set this to the name of the container in the
                                                              # containerDefinitions section of your task definition
  AWS_IAM_ROLE: github-oidc-provider-aws-stories              # set this to your AWS OICD provider role
  ECS_TASK_DEFINITION_FAMILY: stories-task-definition         # set this to your Amazon ECS task definition family
                                                              # (name that we gave to the task definition)

permissions:
  contents: read
  id-token: write

jobs:
  build-and-deploy:
    name: Build, test and deploy
    runs-on: ubuntu-latest
    environment: production

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
    - name: Checkout
      uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@af1da67850ed9a4cedd57bfd976089dd991e2582

    - name: Run tests and build
      env:
        AWS_REGION: eu-north-1
      run: ./gradlew build

    - name: Configure AWS credentials (OIDC)
      uses: aws-actions/configure-aws-credentials@v4
      with:
        audience: sts.amazonaws.com
        aws-region: ${{ env.AWS_REGION }}
        role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/${{ env.AWS_IAM_ROLE }}

    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v2

    - name: Build, tag, and push image to Amazon ECR
      id: build-image
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        ECR_REPOSITORY: ${{ env.ECR_REPOSITORY }}
        IMAGE_TAG: ${{ github.sha }}
      run: |
        # If your ECS service runs on x86_64 instead of ARM, replace '--platform linux/arm64' with the default:
        # docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
        docker buildx build --platform linux/arm64 -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
        echo "image=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT

    - name: Download task definition
      run: |
        aws ecs describe-task-definition \
          --task-definition "${{ env.ECS_TASK_DEFINITION_FAMILY }}" \
          --query 'taskDefinition' > /tmp/app-task-definition.json

    - name: Sanitize task definitioin
      run: |
        jq 'del(.taskDefinitionArn, .revision, .status, .registeredAt, .registeredBy)' \
          /tmp/app-task-definition.json > /tmp/app-task-definition.sanitized.json

    - name: Fill in the new image ID in the Amazon ECS task definition
      id: task-def-app
      uses: aws-actions/amazon-ecs-render-task-definition@v1
      with:
        task-definition: /tmp/app-task-definition.sanitized.json
        container-name: ${{ env.CONTAINER_NAME }}
        image: ${{ steps.build-image.outputs.image }}

    - name: Deploy Amazon ECS task definition
      uses: aws-actions/amazon-ecs-deploy-task-definition@v2
      with:
        task-definition: ${{ steps.task-def-app.outputs.task-definition }}
        service: ${{ env.ECS_SERVICE }}
        cluster: ${{ env.ECS_CLUSTER }}
        wait-for-service-stability: true
```






















