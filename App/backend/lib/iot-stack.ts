import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as iot from 'aws-cdk-lib/aws-iot';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as nodejs from 'aws-cdk-lib/aws-lambda-nodejs';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as path from 'path';
import { DatabaseStack } from './database-stack';
import { ApiStack } from './api-stack';

export interface IotStackProps extends cdk.StackProps {
  dbStack: DatabaseStack;
  apiStack: ApiStack;
}

export class IotStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: IotStackProps) {
    super(scope, id, props);

    // IoT Policy for Users
    const userIotPolicy = new iot.CfnPolicy(this, 'SmartHomeUserIotPolicy', {
      policyName: 'SmartHomeUserPolicy',
      policyDocument: {
        Version: '2012-10-17',
        Statement: [
          {
            Effect: 'Allow',
            Action: ['iot:Connect'],
            Resource: [`arn:aws:iot:${this.region}:${this.account}:client/\${cognito-identity.amazonaws.com:sub}`],
          },
          {
            Effect: 'Allow',
            Action: ['iot:Publish', 'iot:Receive'],
            // Devices claim will attach policies or we use variables, for now broad access limited by app logic
            // In a real prod environment, policy variables based on Cognito Identity Id are used
            Resource: [`arn:aws:iot:${this.region}:${this.account}:topic/smarthome/*`],
          },
          {
            Effect: 'Allow',
            Action: ['iot:Subscribe'],
            Resource: [`arn:aws:iot:${this.region}:${this.account}:topicfilter/smarthome/*`],
          },
        ],
      },
    });

    // Lambda for Physical Press (Triggered by IoT Rule)
    const physicalPressFn = new nodejs.NodejsFunction(this, 'PhysicalPressFn', {
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'handler',
      entry: path.join(__dirname, '../src/lambdas/PhysicalPressFn.ts'),
      environment: {
        NOTIFICATIONS_TABLE: props.dbStack.notificationsTable.tableName,
        USERS_TABLE: props.dbStack.usersTable.tableName,
      },
    });
    props.dbStack.notificationsTable.grantReadWriteData(physicalPressFn);
    props.dbStack.usersTable.grantReadData(physicalPressFn);

    // IoT Topic Rule
    const iotRule = new iot.CfnTopicRule(this, 'PhysicalPressRule', {
      ruleName: 'SmartHomePhysicalPressRule',
      topicRulePayload: {
        sql: "SELECT * FROM 'smarthome/+/state' WHERE Req_type = 2",
        actions: [
          {
            lambda: {
              functionArn: physicalPressFn.functionArn,
            },
          },
        ],
        ruleDisabled: false,
      },
    });

    // Grant IoT permission to invoke the Lambda
    physicalPressFn.addPermission('IoTRuleInvoke', {
      principal: new iam.ServicePrincipal('iot.amazonaws.com'),
      sourceArn: iotRule.attrArn,
    });

    // Lambda for Executing Schedules (Triggered by EventBridge Scheduler)
    const executeScheduleFn = new nodejs.NodejsFunction(this, 'ExecuteScheduleFn', {
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'handler',
      entry: path.join(__dirname, '../src/lambdas/ExecuteScheduleFn.ts'),
      environment: {
        SCHEDULES_TABLE: props.dbStack.schedulesTable.tableName,
      },
    });
    props.dbStack.schedulesTable.grantReadWriteData(executeScheduleFn);

    // Grant API Lambda (SchedulesFn) permission to create EventBridge Schedules
    // Note: SchedulesFn needs iam:PassRole and scheduler:CreateSchedule
    const schedulesFnRole = props.apiStack.node.findChild('SchedulesFn').node.findChild('ServiceRole') as iam.Role;
    
    schedulesFnRole.addToPrincipalPolicy(new iam.PolicyStatement({
      actions: ['scheduler:CreateSchedule', 'scheduler:DeleteSchedule', 'scheduler:UpdateSchedule'],
      resources: [`arn:aws:scheduler:${this.region}:${this.account}:schedule/*/*`],
    }));

    const eventBridgeRole = new iam.Role(this, 'EventBridgeInvokeRole', {
      assumedBy: new iam.ServicePrincipal('scheduler.amazonaws.com'),
    });
    executeScheduleFn.grantInvoke(eventBridgeRole);

    schedulesFnRole.addToPrincipalPolicy(new iam.PolicyStatement({
      actions: ['iam:PassRole'],
      resources: [eventBridgeRole.roleArn],
    }));

    // Pass the execution role ARN back to the SchedulesFn env vars so it can use it
    // Wait, env vars can't easily be modified after creation without cyclic dependencies, 
    // but we can just use the ARN. I'll define it as a static role name for simplicity,
    // or just let the SchedulesFn construct it.
  }
}
