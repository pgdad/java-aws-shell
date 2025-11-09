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
     */
    @ShellMethod(key = "ec2 describe-security-groups", value = "Describe security groups")
    public String describeSecurityGroups() {
        try {
            DescribeSecurityGroupsResponse response = ec2Client.describeSecurityGroups();

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
}
