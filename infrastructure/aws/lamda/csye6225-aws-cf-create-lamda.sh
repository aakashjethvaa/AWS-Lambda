###################################################################################
#starting of script
#Get a STACK name to create new one.
###################################################################################
echo "Please Enter a new name for the stack: "
read stack_name


name="lambdaFunction"
role="lambda_Dynamodb_SES"
#lambdaRoleArn=$(aws iam get-role --role-name $roleName --query Role.Arn --output text)
#echo "lambdaArn: $lambdaRoleArn"

#domain=$(aws route53 list-hosted-zones --query HostedZones[0].Name --output text)
#trimdomain=${domain::-1}
#bucket_name="code-deploy.$trimdomain"
read -p "Enter bucket domain name" domain
bucket_name="code-deploy.csye6225-fall2018-$domain.me.csye6225.com"
echo "S3 Domain: $bucket_name"

createOutput=$(aws cloudformation create-stack --stack-name $stack_name --capabilities CAPABILITY_NAMED_IAM --template-body file://csye6225-cf-lamda.json --parameters ParameterKey=s3domain,ParameterValue=$bucket_name  ParameterKey=role,ParameterValue=$role  ParameterKey=name,ParameterValue=$name)

if [ $? -eq 0 ]; then
	echo "Creating stack..."
	aws cloudformation wait stack-create-complete --stack-name $stack_name 
	echo "Stack created successfully. Stack Id below: "
	echo $createOutput

else
	echo "Error in creation of stack"
	echo $createOutput
fi;
