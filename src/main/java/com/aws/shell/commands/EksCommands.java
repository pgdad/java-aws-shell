package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EKS Commands
 * <p>
 * Implements AWS EKS CLI-like functionality with variable support
 */
@ShellComponent
public class EksCommands {

    private final EksClient eksClient;
    private final SessionContext sessionContext;

    public EksCommands(EksClient eksClient, SessionContext sessionContext) {
        this.eksClient = eksClient;
        this.sessionContext = sessionContext;
    }

    /**
     * List EKS clusters
     * <p>
     * Usage:
     * eks list-clusters
     */
    @ShellMethod(key = "eks list-clusters", value = "List EKS clusters")
    public String listClusters() {
        try {
            ListClustersResponse response = eksClient.listClusters();

            if (response.clusters().isEmpty()) {
                return "No clusters found";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Cluster Name"});

            for (String clusterName : response.clusters()) {
                rows.add(new String[]{clusterName});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe EKS cluster
     * <p>
     * Usage:
     * eks describe-cluster --name my-cluster
     * eks describe-cluster --name $CLUSTER_NAME
     */
    @ShellMethod(key = "eks describe-cluster", value = "Describe EKS cluster")
    public String describeCluster(String name) {
        try {
            name = sessionContext.resolveVariables(name);

            DescribeClusterRequest request = DescribeClusterRequest.builder()
                    .name(name)
                    .build();

            DescribeClusterResponse response = eksClient.describeCluster(request);
            Cluster cluster = response.cluster();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Name", cluster.name()});
            pairs.add(new String[]{"ARN", cluster.arn()});
            pairs.add(new String[]{"Status", cluster.statusAsString()});
            pairs.add(new String[]{"Version", cluster.version()});
            pairs.add(new String[]{"Endpoint", cluster.endpoint() != null ? cluster.endpoint() : "N/A"});
            pairs.add(new String[]{"Role ARN", cluster.roleArn()});
            pairs.add(new String[]{"Platform Version", cluster.platformVersion()});

            if (cluster.createdAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pairs.add(new String[]{"Created At", cluster.createdAt().atZone(ZoneId.systemDefault()).format(formatter)});
            }

            StringBuilder result = new StringBuilder(OutputFormatter.toKeyValue(pairs));

            // VPC Config
            if (cluster.resourcesVpcConfig() != null) {
                result.append("\n\nVPC Configuration:\n");
                VpcConfigResponse vpcConfig = cluster.resourcesVpcConfig();
                List<String[]> vpcPairs = new ArrayList<>();
                vpcPairs.add(new String[]{"VPC ID", vpcConfig.vpcId() != null ? vpcConfig.vpcId() : "N/A"});
                vpcPairs.add(new String[]{"Subnet IDs", String.join(", ", vpcConfig.subnetIds())});
                vpcPairs.add(new String[]{"Security Group IDs", String.join(", ", vpcConfig.securityGroupIds())});
                vpcPairs.add(new String[]{"Endpoint Public Access", String.valueOf(vpcConfig.endpointPublicAccess())});
                vpcPairs.add(new String[]{"Endpoint Private Access", String.valueOf(vpcConfig.endpointPrivateAccess())});
                result.append(OutputFormatter.toKeyValue(vpcPairs));
            }

            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List EKS node groups
     * <p>
     * Usage:
     * eks list-nodegroups --cluster-name my-cluster
     * eks list-nodegroups --cluster-name $CLUSTER_NAME
     */
    @ShellMethod(key = "eks list-nodegroups", value = "List EKS node groups")
    public String listNodegroups(String clusterName) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);

            ListNodegroupsRequest request = ListNodegroupsRequest.builder()
                    .clusterName(clusterName)
                    .build();

            ListNodegroupsResponse response = eksClient.listNodegroups(request);

            if (response.nodegroups().isEmpty()) {
                return "No node groups found in cluster: " + clusterName;
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Node Group Name"});

            for (String nodegroupName : response.nodegroups()) {
                rows.add(new String[]{nodegroupName});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe EKS node group
     * <p>
     * Usage:
     * eks describe-nodegroup --cluster-name my-cluster --nodegroup-name my-nodegroup
     * eks describe-nodegroup --cluster-name $CLUSTER_NAME --nodegroup-name $NODEGROUP_NAME
     */
    @ShellMethod(key = "eks describe-nodegroup", value = "Describe EKS node group")
    public String describeNodegroup(String clusterName, String nodegroupName) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);
            nodegroupName = sessionContext.resolveVariables(nodegroupName);

            DescribeNodegroupRequest request = DescribeNodegroupRequest.builder()
                    .clusterName(clusterName)
                    .nodegroupName(nodegroupName)
                    .build();

            DescribeNodegroupResponse response = eksClient.describeNodegroup(request);
            Nodegroup nodegroup = response.nodegroup();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Nodegroup Name", nodegroup.nodegroupName()});
            pairs.add(new String[]{"ARN", nodegroup.nodegroupArn()});
            pairs.add(new String[]{"Status", nodegroup.statusAsString()});
            pairs.add(new String[]{"Capacity Type", nodegroup.capacityTypeAsString()});
            pairs.add(new String[]{"Node Role", nodegroup.nodeRole()});
            pairs.add(new String[]{"Version", nodegroup.version() != null ? nodegroup.version() : "N/A"});
            pairs.add(new String[]{"Release Version", nodegroup.releaseVersion() != null ? nodegroup.releaseVersion() : "N/A"});

            if (nodegroup.createdAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pairs.add(new String[]{"Created At", nodegroup.createdAt().atZone(ZoneId.systemDefault()).format(formatter)});
            }

            StringBuilder result = new StringBuilder(OutputFormatter.toKeyValue(pairs));

            // Scaling Config
            if (nodegroup.scalingConfig() != null) {
                result.append("\n\nScaling Configuration:\n");
                NodegroupScalingConfig scaling = nodegroup.scalingConfig();
                List<String[]> scalingPairs = new ArrayList<>();
                scalingPairs.add(new String[]{"Min Size", String.valueOf(scaling.minSize())});
                scalingPairs.add(new String[]{"Max Size", String.valueOf(scaling.maxSize())});
                scalingPairs.add(new String[]{"Desired Size", String.valueOf(scaling.desiredSize())});
                result.append(OutputFormatter.toKeyValue(scalingPairs));
            }

            // Instance Types
            if (nodegroup.instanceTypes() != null && !nodegroup.instanceTypes().isEmpty()) {
                result.append("\n\nInstance Types: ").append(String.join(", ", nodegroup.instanceTypes()));
            }

            // Subnets
            if (nodegroup.subnets() != null && !nodegroup.subnets().isEmpty()) {
                result.append("\n\nSubnets: ").append(String.join(", ", nodegroup.subnets()));
            }

            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update EKS node group configuration
     * <p>
     * Usage:
     * eks update-nodegroup-config --cluster-name my-cluster --nodegroup-name my-nodegroup --min-size 1 --max-size 5 --desired-size 3
     */
    @ShellMethod(key = "eks update-nodegroup-config", value = "Update EKS node group configuration")
    public String updateNodegroupConfig(String clusterName, String nodegroupName,
                                        @ShellOption(defaultValue = "-1") int minSize,
                                        @ShellOption(defaultValue = "-1") int maxSize,
                                        @ShellOption(defaultValue = "-1") int desiredSize) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);
            nodegroupName = sessionContext.resolveVariables(nodegroupName);

            NodegroupScalingConfig.Builder scalingBuilder = NodegroupScalingConfig.builder();
            boolean hasScalingConfig = false;

            if (minSize >= 0) {
                scalingBuilder.minSize(minSize);
                hasScalingConfig = true;
            }
            if (maxSize >= 0) {
                scalingBuilder.maxSize(maxSize);
                hasScalingConfig = true;
            }
            if (desiredSize >= 0) {
                scalingBuilder.desiredSize(desiredSize);
                hasScalingConfig = true;
            }

            if (!hasScalingConfig) {
                return "Error: At least one scaling parameter (min-size, max-size, or desired-size) must be specified";
            }

            UpdateNodegroupConfigRequest request = UpdateNodegroupConfigRequest.builder()
                    .clusterName(clusterName)
                    .nodegroupName(nodegroupName)
                    .scalingConfig(scalingBuilder.build())
                    .build();

            UpdateNodegroupConfigResponse response = eksClient.updateNodegroupConfig(request);
            Update update = response.update();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Update ID", update.id()});
            pairs.add(new String[]{"Status", update.statusAsString()});
            pairs.add(new String[]{"Type", update.typeAsString()});

            return "Node group configuration update initiated:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update EKS node group version
     * <p>
     * Usage:
     * eks update-nodegroup-version --cluster-name my-cluster --nodegroup-name my-nodegroup
     * eks update-nodegroup-version --cluster-name my-cluster --nodegroup-name my-nodegroup --version 1.28
     */
    @ShellMethod(key = "eks update-nodegroup-version", value = "Update EKS node group version")
    public String updateNodegroupVersion(String clusterName, String nodegroupName,
                                         @ShellOption(defaultValue = "") String version) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);
            nodegroupName = sessionContext.resolveVariables(nodegroupName);
            version = sessionContext.resolveVariables(version);

            UpdateNodegroupVersionRequest.Builder requestBuilder = UpdateNodegroupVersionRequest.builder()
                    .clusterName(clusterName)
                    .nodegroupName(nodegroupName);

            if (!version.isEmpty()) {
                requestBuilder.version(version);
            }

            UpdateNodegroupVersionResponse response = eksClient.updateNodegroupVersion(requestBuilder.build());
            Update update = response.update();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Update ID", update.id()});
            pairs.add(new String[]{"Status", update.statusAsString()});
            pairs.add(new String[]{"Type", update.typeAsString()});

            return "Node group version update initiated:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List EKS Fargate profiles
     * <p>
     * Usage:
     * eks list-fargate-profiles --cluster-name my-cluster
     * eks list-fargate-profiles --cluster-name $CLUSTER_NAME
     */
    @ShellMethod(key = "eks list-fargate-profiles", value = "List EKS Fargate profiles")
    public String listFargateProfiles(String clusterName) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);

            ListFargateProfilesRequest request = ListFargateProfilesRequest.builder()
                    .clusterName(clusterName)
                    .build();

            ListFargateProfilesResponse response = eksClient.listFargateProfiles(request);

            if (response.fargateProfileNames().isEmpty()) {
                return "No Fargate profiles found in cluster: " + clusterName;
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Fargate Profile Name"});

            for (String profileName : response.fargateProfileNames()) {
                rows.add(new String[]{profileName});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe EKS Fargate profile
     * <p>
     * Usage:
     * eks describe-fargate-profile --cluster-name my-cluster --fargate-profile-name my-profile
     * eks describe-fargate-profile --cluster-name $CLUSTER_NAME --fargate-profile-name $PROFILE_NAME
     */
    @ShellMethod(key = "eks describe-fargate-profile", value = "Describe EKS Fargate profile")
    public String describeFargateProfile(String clusterName, String fargateProfileName) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);
            fargateProfileName = sessionContext.resolveVariables(fargateProfileName);

            DescribeFargateProfileRequest request = DescribeFargateProfileRequest.builder()
                    .clusterName(clusterName)
                    .fargateProfileName(fargateProfileName)
                    .build();

            DescribeFargateProfileResponse response = eksClient.describeFargateProfile(request);
            FargateProfile profile = response.fargateProfile();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Profile Name", profile.fargateProfileName()});
            pairs.add(new String[]{"ARN", profile.fargateProfileArn()});
            pairs.add(new String[]{"Status", profile.statusAsString()});
            pairs.add(new String[]{"Pod Execution Role ARN", profile.podExecutionRoleArn()});

            if (profile.createdAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pairs.add(new String[]{"Created At", profile.createdAt().atZone(ZoneId.systemDefault()).format(formatter)});
            }

            StringBuilder result = new StringBuilder(OutputFormatter.toKeyValue(pairs));

            // Subnets
            if (profile.subnets() != null && !profile.subnets().isEmpty()) {
                result.append("\n\nSubnets: ").append(String.join(", ", profile.subnets()));
            }

            // Selectors
            if (profile.selectors() != null && !profile.selectors().isEmpty()) {
                result.append("\n\nSelectors:\n");
                List<String[]> selectorRows = new ArrayList<>();
                selectorRows.add(new String[]{"Namespace", "Labels"});

                for (FargateProfileSelector selector : profile.selectors()) {
                    String labels = selector.labels() != null && !selector.labels().isEmpty() ?
                            selector.labels().toString() : "N/A";
                    selectorRows.add(new String[]{
                            selector.namespace() != null ? selector.namespace() : "N/A",
                            labels
                    });
                }
                result.append(OutputFormatter.toTable(selectorRows));
            }

            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List EKS addons
     * <p>
     * Usage:
     * eks list-addons --cluster-name my-cluster
     * eks list-addons --cluster-name $CLUSTER_NAME
     */
    @ShellMethod(key = "eks list-addons", value = "List EKS addons")
    public String listAddons(String clusterName) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);

            ListAddonsRequest request = ListAddonsRequest.builder()
                    .clusterName(clusterName)
                    .build();

            ListAddonsResponse response = eksClient.listAddons(request);

            if (response.addons().isEmpty()) {
                return "No addons found in cluster: " + clusterName;
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Addon Name"});

            for (String addonName : response.addons()) {
                rows.add(new String[]{addonName});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe EKS addon
     * <p>
     * Usage:
     * eks describe-addon --cluster-name my-cluster --addon-name vpc-cni
     * eks describe-addon --cluster-name $CLUSTER_NAME --addon-name $ADDON_NAME
     */
    @ShellMethod(key = "eks describe-addon", value = "Describe EKS addon")
    public String describeAddon(String clusterName, String addonName) {
        try {
            clusterName = sessionContext.resolveVariables(clusterName);
            addonName = sessionContext.resolveVariables(addonName);

            DescribeAddonRequest request = DescribeAddonRequest.builder()
                    .clusterName(clusterName)
                    .addonName(addonName)
                    .build();

            DescribeAddonResponse response = eksClient.describeAddon(request);
            Addon addon = response.addon();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Addon Name", addon.addonName()});
            pairs.add(new String[]{"ARN", addon.addonArn()});
            pairs.add(new String[]{"Status", addon.statusAsString()});
            pairs.add(new String[]{"Version", addon.addonVersion() != null ? addon.addonVersion() : "N/A"});
            pairs.add(new String[]{"Service Account Role ARN", addon.serviceAccountRoleArn() != null ? addon.serviceAccountRoleArn() : "N/A"});

            if (addon.createdAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pairs.add(new String[]{"Created At", addon.createdAt().atZone(ZoneId.systemDefault()).format(formatter)});
            }

            if (addon.modifiedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pairs.add(new String[]{"Modified At", addon.modifiedAt().atZone(ZoneId.systemDefault()).format(formatter)});
            }

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update EKS cluster version
     * <p>
     * Usage:
     * eks update-cluster-version --name my-cluster --version 1.28
     * eks update-cluster-version --name $CLUSTER_NAME --version $VERSION
     */
    @ShellMethod(key = "eks update-cluster-version", value = "Update EKS cluster version")
    public String updateClusterVersion(String name, String version) {
        try {
            name = sessionContext.resolveVariables(name);
            version = sessionContext.resolveVariables(version);

            UpdateClusterVersionRequest request = UpdateClusterVersionRequest.builder()
                    .name(name)
                    .version(version)
                    .build();

            UpdateClusterVersionResponse response = eksClient.updateClusterVersion(request);
            Update update = response.update();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Update ID", update.id()});
            pairs.add(new String[]{"Status", update.statusAsString()});
            pairs.add(new String[]{"Type", update.typeAsString()});

            return "Cluster version update initiated:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe update status
     * <p>
     * Usage:
     * eks describe-update --name my-cluster --update-id update-id
     * eks describe-update --name $CLUSTER_NAME --update-id $UPDATE_ID
     */
    @ShellMethod(key = "eks describe-update", value = "Describe EKS update status")
    public String describeUpdate(String name, String updateId,
                                 @ShellOption(defaultValue = "") String nodegroupName) {
        try {
            name = sessionContext.resolveVariables(name);
            updateId = sessionContext.resolveVariables(updateId);
            nodegroupName = sessionContext.resolveVariables(nodegroupName);

            DescribeUpdateRequest.Builder requestBuilder = DescribeUpdateRequest.builder()
                    .name(name)
                    .updateId(updateId);

            if (!nodegroupName.isEmpty()) {
                requestBuilder.nodegroupName(nodegroupName);
            }

            DescribeUpdateResponse response = eksClient.describeUpdate(requestBuilder.build());
            Update update = response.update();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Update ID", update.id()});
            pairs.add(new String[]{"Status", update.statusAsString()});
            pairs.add(new String[]{"Type", update.typeAsString()});

            if (update.createdAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pairs.add(new String[]{"Created At", update.createdAt().atZone(ZoneId.systemDefault()).format(formatter)});
            }

            if (update.errors() != null && !update.errors().isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (ErrorDetail error : update.errors()) {
                    errors.append(error.errorCode()).append(": ").append(error.errorMessage()).append("\n");
                }
                pairs.add(new String[]{"Errors", errors.toString()});
            }

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
