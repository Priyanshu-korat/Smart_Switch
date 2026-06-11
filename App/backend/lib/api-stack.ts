import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as nodejs from 'aws-cdk-lib/aws-lambda-nodejs';
import * as path from 'path';
import { DatabaseStack } from './database-stack';
import { AuthStack } from './auth-stack';

export interface ApiStackProps extends cdk.StackProps {
  dbStack: DatabaseStack;
  authStack: AuthStack;
}

export class ApiStack extends cdk.Stack {
  public readonly api: apigateway.RestApi;

  constructor(scope: Construct, id: string, props: ApiStackProps) {
    super(scope, id, props);

    // API Gateway
    this.api = new apigateway.RestApi(this, 'SmartHomeApi', {
      restApiName: 'SmartHome Service',
      description: 'API for SmartHome App',
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
      },
    });

    const authorizer = new apigateway.CognitoUserPoolsAuthorizer(this, 'SmartHomeAuthorizer', {
      cognitoUserPools: [props.authStack.userPool],
    });

    const defaultMethodOptions: apigateway.MethodOptions = {
      authorizer,
      authorizationType: apigateway.AuthorizationType.COGNITO,
    };

    // Shared NodejsFunction settings
    const lambdaProps: nodejs.NodejsFunctionProps = {
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'handler',
      environment: {
        USERS_TABLE: props.dbStack.usersTable.tableName,
        DEVICES_TABLE: props.dbStack.devicesTable.tableName,
        SWITCHES_TABLE: props.dbStack.switchesTable.tableName,
        ROOMS_TABLE: props.dbStack.roomsTable.tableName,
        SCENES_TABLE: props.dbStack.scenesTable.tableName,
        SCHEDULES_TABLE: props.dbStack.schedulesTable.tableName,
        NOTIFICATIONS_TABLE: props.dbStack.notificationsTable.tableName,
        DEVICE_ACCESS_TABLE: props.dbStack.deviceAccessTable.tableName,
      },
    };

    // Lambdas
    const claimDeviceFn = new nodejs.NodejsFunction(this, 'ClaimDeviceFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/ClaimDeviceFn.ts'),
    });
    props.dbStack.devicesTable.grantReadWriteData(claimDeviceFn);
    props.dbStack.deviceAccessTable.grantReadWriteData(claimDeviceFn);

    const listDevicesFn = new nodejs.NodejsFunction(this, 'ListDevicesFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/ListDevicesFn.ts'),
    });
    props.dbStack.devicesTable.grantReadData(listDevicesFn);

    const removeDeviceFn = new nodejs.NodejsFunction(this, 'RemoveDeviceFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/RemoveDeviceFn.ts'),
    });
    props.dbStack.devicesTable.grantReadWriteData(removeDeviceFn);
    props.dbStack.deviceAccessTable.grantReadWriteData(removeDeviceFn);

    const updateSwitchesFn = new nodejs.NodejsFunction(this, 'UpdateSwitchesFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/UpdateSwitchesFn.ts'),
    });
    props.dbStack.switchesTable.grantReadWriteData(updateSwitchesFn);

    const roomsFn = new nodejs.NodejsFunction(this, 'RoomsFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/RoomsFn.ts'),
    });
    props.dbStack.roomsTable.grantReadWriteData(roomsFn);

    const scenesFn = new nodejs.NodejsFunction(this, 'ScenesFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/ScenesFn.ts'),
    });
    props.dbStack.scenesTable.grantReadWriteData(scenesFn);

    const schedulesFn = new nodejs.NodejsFunction(this, 'SchedulesFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/SchedulesFn.ts'),
    });
    props.dbStack.schedulesTable.grantReadWriteData(schedulesFn);

    const notificationsFn = new nodejs.NodejsFunction(this, 'NotificationsFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/NotificationsFn.ts'),
    });
    props.dbStack.notificationsTable.grantReadWriteData(notificationsFn);

    const userFn = new nodejs.NodejsFunction(this, 'UserFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/UserFn.ts'),
    });
    props.dbStack.usersTable.grantReadWriteData(userFn);

    const updateFcmTokenFn = new nodejs.NodejsFunction(this, 'UpdateFcmTokenFn', {
      ...lambdaProps,
      entry: path.join(__dirname, '../src/lambdas/UpdateFcmTokenFn.ts'),
    });
    props.dbStack.usersTable.grantReadWriteData(updateFcmTokenFn);

    // API Routes
    const devicesResource = this.api.root.addResource('devices');
    devicesResource.addMethod('GET', new apigateway.LambdaIntegration(listDevicesFn), defaultMethodOptions);
    
    const claimDeviceResource = devicesResource.addResource('claim');
    claimDeviceResource.addMethod('POST', new apigateway.LambdaIntegration(claimDeviceFn), defaultMethodOptions);

    const deviceIdResource = devicesResource.addResource('{id}');
    deviceIdResource.addMethod('DELETE', new apigateway.LambdaIntegration(removeDeviceFn), defaultMethodOptions);

    const switchesResource = deviceIdResource.addResource('switches');
    switchesResource.addMethod('PUT', new apigateway.LambdaIntegration(updateSwitchesFn), defaultMethodOptions);

    const roomsResource = this.api.root.addResource('rooms');
    const roomsIntegration = new apigateway.LambdaIntegration(roomsFn);
    roomsResource.addMethod('GET', roomsIntegration, defaultMethodOptions);
    roomsResource.addMethod('POST', roomsIntegration, defaultMethodOptions);
    roomsResource.addMethod('PUT', roomsIntegration, defaultMethodOptions);
    roomsResource.addMethod('DELETE', roomsIntegration, defaultMethodOptions);

    const scenesResource = this.api.root.addResource('scenes');
    const scenesIntegration = new apigateway.LambdaIntegration(scenesFn);
    scenesResource.addMethod('GET', scenesIntegration, defaultMethodOptions);
    scenesResource.addMethod('POST', scenesIntegration, defaultMethodOptions);
    scenesResource.addMethod('PUT', scenesIntegration, defaultMethodOptions);
    scenesResource.addMethod('DELETE', scenesIntegration, defaultMethodOptions);

    const schedulesResource = this.api.root.addResource('schedules');
    const schedulesIntegration = new apigateway.LambdaIntegration(schedulesFn);
    schedulesResource.addMethod('GET', schedulesIntegration, defaultMethodOptions);
    schedulesResource.addMethod('POST', schedulesIntegration, defaultMethodOptions);
    schedulesResource.addMethod('PUT', schedulesIntegration, defaultMethodOptions);
    schedulesResource.addMethod('DELETE', schedulesIntegration, defaultMethodOptions);

    const notificationsResource = this.api.root.addResource('notifications');
    notificationsResource.addMethod('GET', new apigateway.LambdaIntegration(notificationsFn), defaultMethodOptions);
    const notificationIdResource = notificationsResource.addResource('{id}');
    const markReadResource = notificationIdResource.addResource('read');
    markReadResource.addMethod('PUT', new apigateway.LambdaIntegration(notificationsFn), defaultMethodOptions);

    const usersResource = this.api.root.addResource('users');
    const meResource = usersResource.addResource('me');
    const userIntegration = new apigateway.LambdaIntegration(userFn);
    meResource.addMethod('GET', userIntegration, defaultMethodOptions);
    meResource.addMethod('PUT', userIntegration, defaultMethodOptions);
    meResource.addMethod('DELETE', userIntegration, defaultMethodOptions);

    const fcmTokenResource = usersResource.addResource('fcm-token');
    fcmTokenResource.addMethod('PUT', new apigateway.LambdaIntegration(updateFcmTokenFn), defaultMethodOptions);
  }
}
