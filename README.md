# AWS SQS Message Processor

A Spring Boot application that listens to AWS SQS messages and processes them with robust error handling, retry mechanisms, and security features.

## Features

### Message Processing
- Listens to AWS SQS queue for incoming messages
- Processes messages containing product information
- Validates and sanitizes message content
- Converts JSON messages to Product objects
- Makes API calls to process the products
- Rate limiting to prevent system overload

### Retry Mechanism
- Configurable retry attempts (default: 3)
- Exponential backoff using visibility timeout
- Dead Letter Queue (DLQ) integration for failed messages
- Retry count tracking per message
- Automatic cleanup of successful message retry counts

### Error Handling
- Comprehensive exception handling
- Detailed logging of processing steps
- Secure error messages without sensitive information
- Custom exception types for better error management
- Validation of message size and content

### Security Features
- XSS protection
- Content Security Policy implementation
- Frame options security
- HTTPS/TLS support
- CSRF protection (disabled for AWS SDK callbacks)
- Input sanitization
- Secure logging practices

### Product Validation
- Name validation (2-100 characters)
- Description length check (max 500 characters)
- Price validation (must be positive)
- Quantity validation (non-negative)
- Required field validation

### Monitoring & Logging
- Detailed logging of processing steps
- Message tracking through processing lifecycle
- Rate limit monitoring
- DLQ monitoring
- Processing success/failure logging
- Performance metrics logging

## Configuration

### AWS Configuration
```properties
aws.sqs.arn=arn:aws:sqs:region:account:queuename
aws.sqs.url=https://sqs.region.amazonaws.com/account/queuename
aws.sqs.dlq.url=https://sqs.region.amazonaws.com/account/queuename-dlq
aws.region=region
```

### Application Configuration
```properties
server.port=8082
server.ssl.enabled=true
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3

# Rate limiting
aws.sqs.rate-limit=10
aws.sqs.batch-size=10

# Timeouts
aws.sqs.connection-timeout=5000
aws.sqs.socket-timeout=5000
```

## Dependencies
- Spring Boot 3.2.3
- AWS Java SDK SQS
- Google Guava
- Project Lombok
- Spring Security
- Spring Validation
- SLF4J/Log4j2

## Getting Started

1. Configure AWS credentials:
   - Set up AWS credentials in `~/.aws/credentials` or
   - Use environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`

2. Update application.properties:
   - Set your AWS region
   - Configure SQS queue URLs
   - Set API endpoint URL

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
java -jar target/demo-sqslistener-0.0.1-SNAPSHOT.jar
```

## Message Format
The application expects messages in JSON format:
```json
{
    "name": "Product Name",
    "description": "Product Description",
    "price": 99.99,
    "quantity": 10
}
```

## Error Handling
- Invalid messages are rejected
- Messages exceeding size limit are rejected
- Failed processing attempts are retried
- Messages failing after max retries go to DLQ
- All errors are logged with appropriate context

## Monitoring
- Application logs available in `logs/app.log`
- Rolling file strategy with size and time-based rotation
- Masked sensitive data in logs
- Structured logging format for easy parsing

## Security Considerations
- TLS 1.2/1.3 only
- Input validation and sanitization
- Secure error handling
- Protected endpoints
- Rate limiting
- No sensitive data exposure

## Contributing
Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## AWS Configuration Requirements

### AWS Credentials
To connect to AWS SQS, you need one of the following authentication methods:

1. **Environment Variables** (Recommended for local development):
   ```bash
   export AWS_ACCESS_KEY_ID=your_access_key
   export AWS_SECRET_ACCESS_KEY=your_secret_key
   export AWS_REGION=your_region
   ```

2. **AWS Instance Role** (Recommended for EC2/ECS deployment):
   - Attach an IAM role to your EC2 instance or ECS task
   - Required SQS permissions in the role:
     ```json
     {
         "Version": "2012-10-17",
         "Statement": [
             {
                 "Effect": "Allow",
                 "Action": [
                     "sqs:ReceiveMessage",
                     "sqs:DeleteMessage",
                     "sqs:GetQueueAttributes",
                     "sqs:ChangeMessageVisibility",
                     "sqs:SendMessage"
                 ],
                 "Resource": [
                     "arn:aws:sqs:region:account:queuename",
                     "arn:aws:sqs:region:account:queuename-dlq"
                 ]
             }
         ]
     }
     ```

3. **AWS Credentials file** (Alternative for local development):
   - Location: `~/.aws/credentials`
   - Format:
     ```ini
     [default]
     aws_access_key_id = your_access_key
     aws_secret_access_key = your_secret_key
     ```

### Security Best Practices
- Never commit AWS credentials to version control
- Rotate access keys regularly
- Use IAM roles with minimum required permissions
- Enable CloudTrail logging for SQS operations
- Use VPC endpoints for SQS when possible 