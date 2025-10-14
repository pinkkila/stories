# stories - CI/CD project

The purpose of this project was to implement a CI/CD pipeline and document the process.

### Example Application

The project uses a simple Spring Boot MVC app with a PostgreSQL database.

## CI/CD Document 

[Document](https://github.com/pinkkila/stories/blob/dev/documents/cicd.md) walks through building a CI/CD pipeline for a containerized application using GitHub Actions and deploying it to AWS Elastic Container Service (ECS) with Fargate. The setup demonstrates how to automate building, containerizing, and deploying your application using AWS and GitHub workflows. 

In addition to the deployment pipeline, the document includes integration with Amazon RDS for database hosting and AWS Secrets Manager for credential management, including automatic secret rotation using AWS Lambda. The pipeline uses GitHub’s OpenID Connect (OIDC) provider to enable authentication between GitHub Actions and AWS.

## AWS Architecture Diagram

![img_1.png](documents/cicd-img/diagram.png)
Diagram references: [^1], [^2].

## What's Next

The next step is to implement the same deployment process using Terraform.


#### References:

[^1]: IAAS Academy. Deploy Applications on AWS Fargate (ECS Tutorial + Hands-On Project): https://www.youtube.com/watch?v=C6v1GVHfOow

[^2]: AWS documentation. Best practices for connecting Amazon ECS to AWS services from inside your VPC: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/networking-connecting-vpc.html