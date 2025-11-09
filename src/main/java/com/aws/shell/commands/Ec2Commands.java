package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EC2 Commands
 * <p>
 * Implements AWS EC2 CLI-like functionality with variable support
 */
@ShellComponent
public class Ec2Commands {

    private final Ec2Client ec2Client;
    private final SessionContext sessionContext;

    public Ec2Commands(Ec2Client ec2Client, SessionContext sessionContext) {
        this.ec2Client = ec2Client;
        this.sessionContext = sessionContext;
    }

    /**
     * Describe EC2 instances
     * <p>
     * Usage:
     * ec2 describe-instances
     * ec2 describe-instances --instance-ids i-1234567890abcdef0
     * ec2 describe-instances --instance-ids $INSTANCE_ID
     */
    @ShellMethod(key = "ec2 describe-instances", value = "Describe EC2 instances")
    public String describeInstances(
            @ShellOption(defaultValue = "") String instanceIds) {
        try {
            instanceIds = sessionContext.resolveVariables(instanceIds);

            DescribeInstancesRequest.Builder requestBuilder = DescribeInstancesRequest.builder();

            if (!instanceIds.isEmpty()) {
                List<String> idList = Arrays.asList(instanceIds.split(","));
                requestBuilder.instanceIds(idList);
            }

            DescribeInstancesResponse response = ec2Client.describeInstances(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Instance ID", "Name", "Type", "State", "Public IP", "Private IP", "Launch Time"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String name = instance.tags().stream()
                            .filter(tag -> "Name".equals(tag.key()))
                            .map(Tag::value)
                            .findFirst()
                            .orElse("-");

                    rows.add(new String[]{
                            instance.instanceId(),
                            name,
                            instance.instanceTypeAsString(),
                            instance.state().nameAsString(),
                            instance.publicIpAddress() != null ? instance.publicIpAddress() : "-",
                            instance.privateIpAddress() != null ? instance.privateIpAddress() : "-",
                            instance.launchTime().atZone(ZoneId.systemDefault()).format(formatter)
                    });
                }
            }

            if (rows.size() == 1) {
                return "No instances found";
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Start EC2 instances
     * <p>
     * Usage:
     * ec2 start-instances --instance-ids i-1234567890abcdef0,i-0987654321fedcba0
     * ec2 start-instances --instance-ids $INSTANCE_ID
     */
    @ShellMethod(key = "ec2 start-instances", value = "Start EC2 instances")
    public String startInstances(String instanceIds) {
        try {
            instanceIds = sessionContext.resolveVariables(instanceIds);
            List<String> idList = Arrays.asList(instanceIds.split(","));

            StartInstancesRequest request = StartInstancesRequest.builder()
                    .instanceIds(idList)
                    .build();

            StartInstancesResponse response = ec2Client.startInstances(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Instance ID", "Previous State", "Current State"});

            for (InstanceStateChange change : response.startingInstances()) {
                rows.add(new String[]{
                        change.instanceId(),
                        change.previousState().nameAsString(),
                        change.currentState().nameAsString()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Stop EC2 instances
     * <p>
     * Usage:
     * ec2 stop-instances --instance-ids i-1234567890abcdef0,i-0987654321fedcba0
     * ec2 stop-instances --instance-ids $INSTANCE_ID
     */
    @ShellMethod(key = "ec2 stop-instances", value = "Stop EC2 instances")
    public String stopInstances(String instanceIds) {
        try {
            instanceIds = sessionContext.resolveVariables(instanceIds);
            List<String> idList = Arrays.asList(instanceIds.split(","));

            StopInstancesRequest request = StopInstancesRequest.builder()
                    .instanceIds(idList)
                    .build();

            StopInstancesResponse response = ec2Client.stopInstances(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Instance ID", "Previous State", "Current State"});

            for (InstanceStateChange change : response.stoppingInstances()) {
                rows.add(new String[]{
                        change.instanceId(),
                        change.previousState().nameAsString(),
                        change.currentState().nameAsString()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Terminate EC2 instances
     * <p>
     * Usage:
     * ec2 terminate-instances --instance-ids i-1234567890abcdef0
     * ec2 terminate-instances --instance-ids $INSTANCE_ID
     */
    @ShellMethod(key = "ec2 terminate-instances", value = "Terminate EC2 instances")
    public String terminateInstances(String instanceIds) {
        try {
            instanceIds = sessionContext.resolveVariables(instanceIds);
            List<String> idList = Arrays.asList(instanceIds.split(","));

            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                    .instanceIds(idList)
                    .build();

            TerminateInstancesResponse response = ec2Client.terminateInstances(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Instance ID", "Previous State", "Current State"});

            for (InstanceStateChange change : response.terminatingInstances()) {
                rows.add(new String[]{
                        change.instanceId(),
                        change.previousState().nameAsString(),
                        change.currentState().nameAsString()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe VPCs
     * <p>
     * Usage:
     * ec2 describe-vpcs
     */
    @ShellMethod(key = "ec2 describe-vpcs", value = "Describe VPCs")
    public String describeVpcs() {
        try {
            DescribeVpcsResponse response = ec2Client.describeVpcs();

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"VPC ID", "Name", "CIDR Block", "State", "Default"});

            for (Vpc vpc : response.vpcs()) {
                String name = vpc.tags().stream()
                        .filter(tag -> "Name".equals(tag.key()))
                        .map(Tag::value)
                        .findFirst()
                        .orElse("-");

                rows.add(new String[]{
                        vpc.vpcId(),
                        name,
                        vpc.cidrBlock(),
                        vpc.stateAsString(),
                        vpc.isDefault() ? "Yes" : "No"
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe subnets
     * <p>
     * Usage:
     * ec2 describe-subnets
     * ec2 describe-subnets --vpc-id vpc-12345678
     * ec2 describe-subnets --vpc-id $VPC_ID
     */
    @ShellMethod(key = "ec2 describe-subnets", value = "Describe subnets")
    public String describeSubnets(@ShellOption(defaultValue = "") String vpcId) {
        try {
            vpcId = sessionContext.resolveVariables(vpcId);

            DescribeSubnetsRequest.Builder requestBuilder = DescribeSubnetsRequest.builder();

            if (!vpcId.isEmpty()) {
                requestBuilder.filters(Filter.builder()
                        .name("vpc-id")
                        .values(vpcId)
                        .build());
            }

            DescribeSubnetsResponse response = ec2Client.describeSubnets(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Subnet ID", "Name", "VPC ID", "CIDR Block", "AZ", "Available IPs"});

            for (Subnet subnet : response.subnets()) {
                String name = subnet.tags().stream()
                        .filter(tag -> "Name".equals(tag.key()))
                        .map(Tag::value)
                        .findFirst()
                        .orElse("-");

                rows.add(new String[]{
                        subnet.subnetId(),
                        name,
                        subnet.vpcId(),
                        subnet.cidrBlock(),
                        subnet.availabilityZone(),
                        String.valueOf(subnet.availableIpAddressCount())
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe security groups
     * <p>
     * Usage:
     * ec2 describe-security-groups
     * ec2 describe-security-groups --group-ids sg-12345678
     */
    @ShellMethod(key = "ec2 describe-security-groups", value = "Describe security groups")
    public String describeSecurityGroups(@ShellOption(defaultValue = "") String groupIds) {
        try {
            groupIds = sessionContext.resolveVariables(groupIds);

            DescribeSecurityGroupsRequest.Builder requestBuilder = DescribeSecurityGroupsRequest.builder();

            if (!groupIds.isEmpty()) {
                requestBuilder.groupIds(Arrays.asList(groupIds.split(",")));
            }

            DescribeSecurityGroupsResponse response = ec2Client.describeSecurityGroups(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Group ID", "Group Name", "VPC ID", "Description"});

            for (SecurityGroup sg : response.securityGroups()) {
                rows.add(new String[]{
                        sg.groupId(),
                        sg.groupName(),
                        sg.vpcId() != null ? sg.vpcId() : "-",
                        sg.description()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Run/create new EC2 instances
     * <p>
     * Usage:
     * ec2 run-instances --image-id ami-12345678 --instance-type t2.micro --count 1
     */
    @ShellMethod(key = "ec2 run-instances", value = "Launch EC2 instances")
    public String runInstances(String imageId,
                               @ShellOption(defaultValue = "t2.micro") String instanceType,
                               @ShellOption(defaultValue = "1") int count,
                               @ShellOption(defaultValue = "") String keyName,
                               @ShellOption(defaultValue = "") String securityGroupIds,
                               @ShellOption(defaultValue = "") String subnetId) {
        try {
            imageId = sessionContext.resolveVariables(imageId);
            instanceType = sessionContext.resolveVariables(instanceType);
            keyName = sessionContext.resolveVariables(keyName);
            securityGroupIds = sessionContext.resolveVariables(securityGroupIds);
            subnetId = sessionContext.resolveVariables(subnetId);

            RunInstancesRequest.Builder requestBuilder = RunInstancesRequest.builder()
                    .imageId(imageId)
                    .instanceType(instanceType)
                    .minCount(count)
                    .maxCount(count);

            if (!keyName.isEmpty()) {
                requestBuilder.keyName(keyName);
            }
            if (!securityGroupIds.isEmpty()) {
                requestBuilder.securityGroupIds(Arrays.asList(securityGroupIds.split(",")));
            }
            if (!subnetId.isEmpty()) {
                requestBuilder.subnetId(subnetId);
            }

            RunInstancesResponse response = ec2Client.runInstances(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Instance ID", "Type", "State", "AMI ID"});

            for (Instance instance : response.instances()) {
                rows.add(new String[]{
                        instance.instanceId(),
                        instance.instanceTypeAsString(),
                        instance.state().nameAsString(),
                        instance.imageId()
                });
            }

            return "Launched " + response.instances().size() + " instance(s):\n" + OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Reboot EC2 instances
     * <p>
     * Usage:
     * ec2 reboot-instances --instance-ids i-1234567890abcdef0
     */
    @ShellMethod(key = "ec2 reboot-instances", value = "Reboot EC2 instances")
    public String rebootInstances(String instanceIds) {
        try {
            instanceIds = sessionContext.resolveVariables(instanceIds);
            List<String> idList = Arrays.asList(instanceIds.split(","));

            RebootInstancesRequest request = RebootInstancesRequest.builder()
                    .instanceIds(idList)
                    .build();

            ec2Client.rebootInstances(request);
            return "Rebooting instances: " + String.join(", ", idList);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe EBS volumes
     * <p>
     * Usage:
     * ec2 describe-volumes
     * ec2 describe-volumes --volume-ids vol-12345678
     */
    @ShellMethod(key = "ec2 describe-volumes", value = "Describe EBS volumes")
    public String describeVolumes(@ShellOption(defaultValue = "") String volumeIds) {
        try {
            volumeIds = sessionContext.resolveVariables(volumeIds);

            DescribeVolumesRequest.Builder requestBuilder = DescribeVolumesRequest.builder();

            if (!volumeIds.isEmpty()) {
                requestBuilder.volumeIds(Arrays.asList(volumeIds.split(",")));
            }

            DescribeVolumesResponse response = ec2Client.describeVolumes(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Volume ID", "Size (GB)", "Type", "State", "AZ", "Encrypted"});

            for (Volume volume : response.volumes()) {
                String attachmentInfo = "-";
                if (!volume.attachments().isEmpty()) {
                    VolumeAttachment attachment = volume.attachments().get(0);
                    attachmentInfo = attachment.instanceId() + ":" + attachment.device();
                }

                rows.add(new String[]{
                        volume.volumeId(),
                        volume.size().toString(),
                        volume.volumeTypeAsString(),
                        volume.stateAsString(),
                        volume.availabilityZone(),
                        volume.encrypted() ? "Yes" : "No"
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create EBS volume
     * <p>
     * Usage:
     * ec2 create-volume --availability-zone us-east-2a --size 10
     */
    @ShellMethod(key = "ec2 create-volume", value = "Create EBS volume")
    public String createVolume(String availabilityZone, int size,
                              @ShellOption(defaultValue = "gp3") String volumeType) {
        try {
            availabilityZone = sessionContext.resolveVariables(availabilityZone);
            volumeType = sessionContext.resolveVariables(volumeType);

            CreateVolumeRequest request = CreateVolumeRequest.builder()
                    .availabilityZone(availabilityZone)
                    .size(size)
                    .volumeType(volumeType)
                    .build();

            CreateVolumeResponse response = ec2Client.createVolume(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Volume ID", response.volumeId()});
            pairs.add(new String[]{"Size", response.size() + " GB"});
            pairs.add(new String[]{"Type", response.volumeTypeAsString()});
            pairs.add(new String[]{"State", response.stateAsString()});
            pairs.add(new String[]{"AZ", response.availabilityZone()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete EBS volume
     * <p>
     * Usage:
     * ec2 delete-volume --volume-id vol-12345678
     */
    @ShellMethod(key = "ec2 delete-volume", value = "Delete EBS volume")
    public String deleteVolume(String volumeId) {
        try {
            volumeId = sessionContext.resolveVariables(volumeId);

            DeleteVolumeRequest request = DeleteVolumeRequest.builder()
                    .volumeId(volumeId)
                    .build();

            ec2Client.deleteVolume(request);
            return "Volume deleted: " + volumeId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Attach EBS volume to instance
     * <p>
     * Usage:
     * ec2 attach-volume --volume-id vol-12345678 --instance-id i-1234567890abcdef0 --device /dev/sdf
     */
    @ShellMethod(key = "ec2 attach-volume", value = "Attach EBS volume to instance")
    public String attachVolume(String volumeId, String instanceId, String device) {
        try {
            volumeId = sessionContext.resolveVariables(volumeId);
            instanceId = sessionContext.resolveVariables(instanceId);
            device = sessionContext.resolveVariables(device);

            AttachVolumeRequest request = AttachVolumeRequest.builder()
                    .volumeId(volumeId)
                    .instanceId(instanceId)
                    .device(device)
                    .build();

            AttachVolumeResponse response = ec2Client.attachVolume(request);
            return "Attaching volume " + volumeId + " to instance " + instanceId + " as " + device +
                   " (State: " + response.stateAsString() + ")";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Detach EBS volume from instance
     * <p>
     * Usage:
     * ec2 detach-volume --volume-id vol-12345678
     */
    @ShellMethod(key = "ec2 detach-volume", value = "Detach EBS volume")
    public String detachVolume(String volumeId) {
        try {
            volumeId = sessionContext.resolveVariables(volumeId);

            DetachVolumeRequest request = DetachVolumeRequest.builder()
                    .volumeId(volumeId)
                    .build();

            DetachVolumeResponse response = ec2Client.detachVolume(request);
            return "Detaching volume " + volumeId + " (State: " + response.stateAsString() + ")";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe snapshots
     * <p>
     * Usage:
     * ec2 describe-snapshots --owner-ids self
     * ec2 describe-snapshots --snapshot-ids snap-12345678
     */
    @ShellMethod(key = "ec2 describe-snapshots", value = "Describe EBS snapshots")
    public String describeSnapshots(@ShellOption(defaultValue = "self") String ownerIds,
                                    @ShellOption(defaultValue = "") String snapshotIds) {
        try {
            ownerIds = sessionContext.resolveVariables(ownerIds);
            snapshotIds = sessionContext.resolveVariables(snapshotIds);

            DescribeSnapshotsRequest.Builder requestBuilder = DescribeSnapshotsRequest.builder();

            if (!ownerIds.isEmpty()) {
                requestBuilder.ownerIds(Arrays.asList(ownerIds.split(",")));
            }
            if (!snapshotIds.isEmpty()) {
                requestBuilder.snapshotIds(Arrays.asList(snapshotIds.split(",")));
            }

            DescribeSnapshotsResponse response = ec2Client.describeSnapshots(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Snapshot ID", "Volume ID", "Size (GB)", "State", "Progress", "Start Time"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Snapshot snapshot : response.snapshots()) {
                rows.add(new String[]{
                        snapshot.snapshotId(),
                        snapshot.volumeId(),
                        snapshot.volumeSize().toString(),
                        snapshot.stateAsString(),
                        snapshot.progress(),
                        snapshot.startTime().atZone(ZoneId.systemDefault()).format(formatter)
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create snapshot
     * <p>
     * Usage:
     * ec2 create-snapshot --volume-id vol-12345678 --description "My backup"
     */
    @ShellMethod(key = "ec2 create-snapshot", value = "Create EBS snapshot")
    public String createSnapshot(String volumeId,
                                 @ShellOption(defaultValue = "") String description) {
        try {
            volumeId = sessionContext.resolveVariables(volumeId);
            description = sessionContext.resolveVariables(description);

            CreateSnapshotRequest.Builder requestBuilder = CreateSnapshotRequest.builder()
                    .volumeId(volumeId);

            if (!description.isEmpty()) {
                requestBuilder.description(description);
            }

            CreateSnapshotResponse response = ec2Client.createSnapshot(requestBuilder.build());

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Snapshot ID", response.snapshotId()});
            pairs.add(new String[]{"Volume ID", response.volumeId()});
            pairs.add(new String[]{"State", response.stateAsString()});
            pairs.add(new String[]{"Progress", response.progress()});
            pairs.add(new String[]{"Start Time", response.startTime().toString()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete snapshot
     * <p>
     * Usage:
     * ec2 delete-snapshot --snapshot-id snap-12345678
     */
    @ShellMethod(key = "ec2 delete-snapshot", value = "Delete EBS snapshot")
    public String deleteSnapshot(String snapshotId) {
        try {
            snapshotId = sessionContext.resolveVariables(snapshotId);

            DeleteSnapshotRequest request = DeleteSnapshotRequest.builder()
                    .snapshotId(snapshotId)
                    .build();

            ec2Client.deleteSnapshot(request);
            return "Snapshot deleted: " + snapshotId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe AMIs
     * <p>
     * Usage:
     * ec2 describe-images --owners self
     * ec2 describe-images --image-ids ami-12345678
     */
    @ShellMethod(key = "ec2 describe-images", value = "Describe AMIs")
    public String describeImages(@ShellOption(defaultValue = "self") String owners,
                                 @ShellOption(defaultValue = "") String imageIds) {
        try {
            owners = sessionContext.resolveVariables(owners);
            imageIds = sessionContext.resolveVariables(imageIds);

            DescribeImagesRequest.Builder requestBuilder = DescribeImagesRequest.builder();

            if (!owners.isEmpty()) {
                requestBuilder.owners(Arrays.asList(owners.split(",")));
            }
            if (!imageIds.isEmpty()) {
                requestBuilder.imageIds(Arrays.asList(imageIds.split(",")));
            }

            DescribeImagesResponse response = ec2Client.describeImages(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"AMI ID", "Name", "State", "Architecture", "Root Device"});

            for (Image image : response.images()) {
                rows.add(new String[]{
                        image.imageId(),
                        image.name() != null ? image.name() : "-",
                        image.stateAsString(),
                        image.architectureAsString(),
                        image.rootDeviceTypeAsString()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create AMI from instance
     * <p>
     * Usage:
     * ec2 create-image --instance-id i-1234567890abcdef0 --name "My AMI"
     */
    @ShellMethod(key = "ec2 create-image", value = "Create AMI from instance")
    public String createImage(String instanceId, String name,
                             @ShellOption(defaultValue = "") String description) {
        try {
            instanceId = sessionContext.resolveVariables(instanceId);
            name = sessionContext.resolveVariables(name);
            description = sessionContext.resolveVariables(description);

            CreateImageRequest.Builder requestBuilder = CreateImageRequest.builder()
                    .instanceId(instanceId)
                    .name(name);

            if (!description.isEmpty()) {
                requestBuilder.description(description);
            }

            CreateImageResponse response = ec2Client.createImage(requestBuilder.build());
            return "AMI created: " + response.imageId();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe key pairs
     * <p>
     * Usage:
     * ec2 describe-key-pairs
     */
    @ShellMethod(key = "ec2 describe-key-pairs", value = "Describe EC2 key pairs")
    public String describeKeyPairs() {
        try {
            DescribeKeyPairsResponse response = ec2Client.describeKeyPairs();

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Key Name", "Key Pair ID", "Fingerprint"});

            for (KeyPairInfo keyPair : response.keyPairs()) {
                rows.add(new String[]{
                        keyPair.keyName(),
                        keyPair.keyPairId(),
                        keyPair.keyFingerprint()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create key pair
     * <p>
     * Usage:
     * ec2 create-key-pair --key-name my-key
     */
    @ShellMethod(key = "ec2 create-key-pair", value = "Create EC2 key pair")
    public String createKeyPair(String keyName) {
        try {
            keyName = sessionContext.resolveVariables(keyName);

            CreateKeyPairRequest request = CreateKeyPairRequest.builder()
                    .keyName(keyName)
                    .build();

            CreateKeyPairResponse response = ec2Client.createKeyPair(request);

            return "Key pair created: " + response.keyName() + "\n\n" +
                   "IMPORTANT: Save this private key material (only shown once):\n\n" +
                   response.keyMaterial();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete key pair
     * <p>
     * Usage:
     * ec2 delete-key-pair --key-name my-key
     */
    @ShellMethod(key = "ec2 delete-key-pair", value = "Delete EC2 key pair")
    public String deleteKeyPair(String keyName) {
        try {
            keyName = sessionContext.resolveVariables(keyName);

            DeleteKeyPairRequest request = DeleteKeyPairRequest.builder()
                    .keyName(keyName)
                    .build();

            ec2Client.deleteKeyPair(request);
            return "Key pair deleted: " + keyName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create security group
     * <p>
     * Usage:
     * ec2 create-security-group --group-name my-sg --description "My SG" --vpc-id vpc-12345678
     */
    @ShellMethod(key = "ec2 create-security-group", value = "Create security group")
    public String createSecurityGroup(String groupName, String description, String vpcId) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            description = sessionContext.resolveVariables(description);
            vpcId = sessionContext.resolveVariables(vpcId);

            CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                    .groupName(groupName)
                    .description(description)
                    .vpcId(vpcId)
                    .build();

            CreateSecurityGroupResponse response = ec2Client.createSecurityGroup(request);
            return "Security group created: " + response.groupId();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete security group
     * <p>
     * Usage:
     * ec2 delete-security-group --group-id sg-12345678
     */
    @ShellMethod(key = "ec2 delete-security-group", value = "Delete security group")
    public String deleteSecurityGroup(String groupId) {
        try {
            groupId = sessionContext.resolveVariables(groupId);

            DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder()
                    .groupId(groupId)
                    .build();

            ec2Client.deleteSecurityGroup(request);
            return "Security group deleted: " + groupId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Authorize security group ingress
     * <p>
     * Usage:
     * ec2 authorize-security-group-ingress --group-id sg-12345678 --protocol tcp --port 22 --cidr 0.0.0.0/0
     */
    @ShellMethod(key = "ec2 authorize-security-group-ingress", value = "Add ingress rule to security group")
    public String authorizeSecurityGroupIngress(String groupId, String protocol, int port,
                                               @ShellOption(defaultValue = "0.0.0.0/0") String cidr) {
        try {
            groupId = sessionContext.resolveVariables(groupId);
            protocol = sessionContext.resolveVariables(protocol);
            cidr = sessionContext.resolveVariables(cidr);

            IpPermission permission = IpPermission.builder()
                    .ipProtocol(protocol)
                    .fromPort(port)
                    .toPort(port)
                    .ipRanges(IpRange.builder().cidrIp(cidr).build())
                    .build();

            AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(groupId)
                    .ipPermissions(permission)
                    .build();

            ec2Client.authorizeSecurityGroupIngress(request);
            return "Ingress rule added to " + groupId + ": " + protocol + " port " + port + " from " + cidr;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe Elastic IPs
     * <p>
     * Usage:
     * ec2 describe-addresses
     */
    @ShellMethod(key = "ec2 describe-addresses", value = "Describe Elastic IP addresses")
    public String describeAddresses() {
        try {
            DescribeAddressesResponse response = ec2Client.describeAddresses();

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Allocation ID", "Public IP", "Private IP", "Instance ID", "Domain"});

            for (Address address : response.addresses()) {
                rows.add(new String[]{
                        address.allocationId() != null ? address.allocationId() : "-",
                        address.publicIp(),
                        address.privateIpAddress() != null ? address.privateIpAddress() : "-",
                        address.instanceId() != null ? address.instanceId() : "-",
                        address.domainAsString()
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Allocate Elastic IP
     * <p>
     * Usage:
     * ec2 allocate-address
     */
    @ShellMethod(key = "ec2 allocate-address", value = "Allocate Elastic IP address")
    public String allocateAddress() {
        try {
            AllocateAddressRequest request = AllocateAddressRequest.builder()
                    .domain(DomainType.VPC)
                    .build();

            AllocateAddressResponse response = ec2Client.allocateAddress(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Allocation ID", response.allocationId()});
            pairs.add(new String[]{"Public IP", response.publicIp()});
            pairs.add(new String[]{"Domain", response.domainAsString()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Release Elastic IP
     * <p>
     * Usage:
     * ec2 release-address --allocation-id eipalloc-12345678
     */
    @ShellMethod(key = "ec2 release-address", value = "Release Elastic IP address")
    public String releaseAddress(String allocationId) {
        try {
            allocationId = sessionContext.resolveVariables(allocationId);

            ReleaseAddressRequest request = ReleaseAddressRequest.builder()
                    .allocationId(allocationId)
                    .build();

            ec2Client.releaseAddress(request);
            return "Elastic IP released: " + allocationId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Associate Elastic IP with instance
     * <p>
     * Usage:
     * ec2 associate-address --instance-id i-1234567890abcdef0 --allocation-id eipalloc-12345678
     */
    @ShellMethod(key = "ec2 associate-address", value = "Associate Elastic IP with instance")
    public String associateAddress(String instanceId, String allocationId) {
        try {
            instanceId = sessionContext.resolveVariables(instanceId);
            allocationId = sessionContext.resolveVariables(allocationId);

            AssociateAddressRequest request = AssociateAddressRequest.builder()
                    .instanceId(instanceId)
                    .allocationId(allocationId)
                    .build();

            AssociateAddressResponse response = ec2Client.associateAddress(request);
            return "Elastic IP associated. Association ID: " + response.associationId();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create tags
     * <p>
     * Usage:
     * ec2 create-tags --resources i-1234567890abcdef0 --tags "Name=MyInstance,Environment=prod"
     */
    @ShellMethod(key = "ec2 create-tags", value = "Create or update EC2 tags")
    public String createTags(String resources, String tags) {
        try {
            resources = sessionContext.resolveVariables(resources);
            tags = sessionContext.resolveVariables(tags);

            List<Tag> tagList = new ArrayList<>();
            for (String tagPair : tags.split(",")) {
                String[] parts = tagPair.trim().split("=", 2);
                if (parts.length == 2) {
                    tagList.add(Tag.builder().key(parts[0].trim()).value(parts[1].trim()).build());
                }
            }

            CreateTagsRequest request = CreateTagsRequest.builder()
                    .resources(Arrays.asList(resources.split(",")))
                    .tags(tagList)
                    .build();

            ec2Client.createTags(request);
            return "Tags created for: " + resources;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
