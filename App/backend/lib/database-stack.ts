import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';

export class DatabaseStack extends cdk.Stack {
  public readonly usersTable: dynamodb.Table;
  public readonly devicesTable: dynamodb.Table;
  public readonly switchesTable: dynamodb.Table;
  public readonly roomsTable: dynamodb.Table;
  public readonly scenesTable: dynamodb.Table;
  public readonly schedulesTable: dynamodb.Table;
  public readonly notificationsTable: dynamodb.Table;
  public readonly deviceAccessTable: dynamodb.Table;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    this.usersTable = new dynamodb.Table(this, 'SmartHomeUsers', {
      tableName: 'smarthome-users',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY, // For dev, we destroy to clean up easily
    });

    this.devicesTable = new dynamodb.Table(this, 'SmartHomeDevices', {
      tableName: 'smarthome-devices',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'deviceId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.switchesTable = new dynamodb.Table(this, 'SmartHomeSwitches', {
      tableName: 'smarthome-switches',
      partitionKey: { name: 'deviceId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'switchIndex', type: dynamodb.AttributeType.NUMBER },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.switchesTable.addGlobalSecondaryIndex({
      indexName: 'roomId-index',
      partitionKey: { name: 'roomId', type: dynamodb.AttributeType.STRING },
    });

    this.roomsTable = new dynamodb.Table(this, 'SmartHomeRooms', {
      tableName: 'smarthome-rooms',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'roomId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.scenesTable = new dynamodb.Table(this, 'SmartHomeScenes', {
      tableName: 'smarthome-scenes',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'sceneId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.schedulesTable = new dynamodb.Table(this, 'SmartHomeSchedules', {
      tableName: 'smarthome-schedules',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'scheduleId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.schedulesTable.addGlobalSecondaryIndex({
      indexName: 'deviceId-index',
      partitionKey: { name: 'deviceId', type: dynamodb.AttributeType.STRING },
    });

    this.notificationsTable = new dynamodb.Table(this, 'SmartHomeNotifications', {
      tableName: 'smarthome-notifications',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'timestamp#id', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.deviceAccessTable = new dynamodb.Table(this, 'SmartHomeDeviceAccess', {
      tableName: 'smarthome-device-access',
      partitionKey: { name: 'deviceId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    this.deviceAccessTable.addGlobalSecondaryIndex({
      indexName: 'userId-deviceId-index',
      partitionKey: { name: 'userId', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'deviceId', type: dynamodb.AttributeType.STRING },
    });
  }
}
