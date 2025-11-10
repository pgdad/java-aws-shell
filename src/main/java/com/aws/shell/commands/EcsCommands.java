package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ECS Commands
 * <p>
 * Implements AWS ECS CLI-like functionality with variable support
 */
@ShellComponent
public class EcsCommands {

    private final EcsClient ecsClient;
    private final SessionContext sessionContext;

    public EcsCommands(EcsClient ecsClient, SessionContext sessionContext) {
        this.ecsClient = ecsClient;
        this.sessionContext = sessionContext;
    }

    /**
     * List ECS clusters
     * <p>
     * Usage:
     * ecs list-clusters
     */
    @ShellMethod(key = "ecs list-clusters", value = "List ECS clusters")
    public String listClusters() {
        try {
            ListClustersResponse response = ecsClient.listClusters();

            if (response.clusterArns().isEmpty()) {
                return "No clusters found";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Cluster ARN"});

            for (String clusterArn : response.clusterArns()) {
                rows.add(new String[]{clusterArn});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe ECS clusters
     * <p>
     * Usage:
     * ecs describe-clusters --clusters my-cluster
     * ecs describe-clusters --clusters $CLUSTER_NAME
     */
    @ShellMethod(key = "ecs describe-clusters", value = "Describe ECS clusters")
    public String describeClusters(@ShellOption(defaultValue = "") String clusters) {
        try {
            clusters = sessionContext.resolveVariables(clusters);

            DescribeClustersRequest.Builder requestBuilder = DescribeClustersRequest.builder();

            if (!clusters.isEmpty()) {
                requestBuilder.clusters(Arrays.asList(clusters.split(",")));
            }

            DescribeClustersResponse response = ecsClient.describeClusters(requestBuilder.build());

            if (response.clusters().isEmpty()) {
                return "No clusters found";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Cluster Name", "Status", "Running Tasks", "Pending Tasks", "Active Services", "Registered Instances"});

            for (Cluster cluster : response.clusters()) {
                rows.add(new String[]{
                        cluster.clusterName(),
                        cluster.status(),
                        String.valueOf(cluster.runningTasksCount()),
                        String.valueOf(cluster.pendingTasksCount()),
                        String.valueOf(cluster.activeServicesCount()),
                        String.valueOf(cluster.registeredContainerInstancesCount())
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List ECS services in a cluster
     * <p>
     * Usage:
     * ecs list-services --cluster my-cluster
     * ecs list-services --cluster $CLUSTER_NAME
     */
    @ShellMethod(key = "ecs list-services", value = "List ECS services")
    public String listServices(String cluster) {
        try {
            cluster = sessionContext.resolveVariables(cluster);

            ListServicesRequest request = ListServicesRequest.builder()
                    .cluster(cluster)
                    .build();

            ListServicesResponse response = ecsClient.listServices(request);

            if (response.serviceArns().isEmpty()) {
                return "No services found in cluster: " + cluster;
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Service ARN"});

            for (String serviceArn : response.serviceArns()) {
                rows.add(new String[]{serviceArn});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe ECS services
     * <p>
     * Usage:
     * ecs describe-services --cluster my-cluster --services my-service
     * ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME
     */
    @ShellMethod(key = "ecs describe-services", value = "Describe ECS services")
    public String describeServices(String cluster, String services) {
        try {
            cluster = sessionContext.resolveVariables(cluster);
            services = sessionContext.resolveVariables(services);

            DescribeServicesRequest request = DescribeServicesRequest.builder()
                    .cluster(cluster)
                    .services(Arrays.asList(services.split(",")))
                    .build();

            DescribeServicesResponse response = ecsClient.describeServices(request);

            if (response.services().isEmpty()) {
                return "No services found";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Service Name", "Status", "Desired Count", "Running Count", "Pending Count", "Task Definition"});

            for (Service service : response.services()) {
                String taskDef = service.taskDefinition();
                // Extract just the family:revision from the full ARN
                if (taskDef.contains("/")) {
                    taskDef = taskDef.substring(taskDef.lastIndexOf("/") + 1);
                }

                rows.add(new String[]{
                        service.serviceName(),
                        service.status(),
                        String.valueOf(service.desiredCount()),
                        String.valueOf(service.runningCount()),
                        String.valueOf(service.pendingCount()),
                        taskDef
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List ECS tasks in a cluster
     * <p>
     * Usage:
     * ecs list-tasks --cluster my-cluster
     * ecs list-tasks --cluster my-cluster --service-name my-service
     * ecs list-tasks --cluster $CLUSTER_NAME --service-name $SERVICE_NAME
     */
    @ShellMethod(key = "ecs list-tasks", value = "List ECS tasks")
    public String listTasks(String cluster, @ShellOption(defaultValue = "") String serviceName) {
        try {
            cluster = sessionContext.resolveVariables(cluster);
            serviceName = sessionContext.resolveVariables(serviceName);

            ListTasksRequest.Builder requestBuilder = ListTasksRequest.builder()
                    .cluster(cluster);

            if (!serviceName.isEmpty()) {
                requestBuilder.serviceName(serviceName);
            }

            ListTasksResponse response = ecsClient.listTasks(requestBuilder.build());

            if (response.taskArns().isEmpty()) {
                return "No tasks found in cluster: " + cluster;
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Task ARN"});

            for (String taskArn : response.taskArns()) {
                rows.add(new String[]{taskArn});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe ECS tasks
     * <p>
     * Usage:
     * ecs describe-tasks --cluster my-cluster --tasks task-id
     * ecs describe-tasks --cluster $CLUSTER_NAME --tasks $TASK_ID
     */
    @ShellMethod(key = "ecs describe-tasks", value = "Describe ECS tasks")
    public String describeTasks(String cluster, String tasks) {
        try {
            cluster = sessionContext.resolveVariables(cluster);
            tasks = sessionContext.resolveVariables(tasks);

            DescribeTasksRequest request = DescribeTasksRequest.builder()
                    .cluster(cluster)
                    .tasks(Arrays.asList(tasks.split(",")))
                    .build();

            DescribeTasksResponse response = ecsClient.describeTasks(request);

            if (response.tasks().isEmpty()) {
                return "No tasks found";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Task ID", "Status", "Desired Status", "Task Definition", "Container Instance"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Task task : response.tasks()) {
                String taskId = task.taskArn();
                if (taskId.contains("/")) {
                    taskId = taskId.substring(taskId.lastIndexOf("/") + 1);
                }

                String taskDef = task.taskDefinitionArn();
                if (taskDef.contains("/")) {
                    taskDef = taskDef.substring(taskDef.lastIndexOf("/") + 1);
                }

                String containerInstance = task.containerInstanceArn() != null ? task.containerInstanceArn() : "N/A";
                if (containerInstance.contains("/")) {
                    containerInstance = containerInstance.substring(containerInstance.lastIndexOf("/") + 1);
                }

                rows.add(new String[]{
                        taskId,
                        task.lastStatus(),
                        task.desiredStatus(),
                        taskDef,
                        containerInstance
                });
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List ECS task definitions
     * <p>
     * Usage:
     * ecs list-task-definitions
     * ecs list-task-definitions --family-prefix my-task
     */
    @ShellMethod(key = "ecs list-task-definitions", value = "List ECS task definitions")
    public String listTaskDefinitions(@ShellOption(defaultValue = "") String familyPrefix) {
        try {
            familyPrefix = sessionContext.resolveVariables(familyPrefix);

            ListTaskDefinitionsRequest.Builder requestBuilder = ListTaskDefinitionsRequest.builder();

            if (!familyPrefix.isEmpty()) {
                requestBuilder.familyPrefix(familyPrefix);
            }

            ListTaskDefinitionsResponse response = ecsClient.listTaskDefinitions(requestBuilder.build());

            if (response.taskDefinitionArns().isEmpty()) {
                return "No task definitions found";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Task Definition ARN"});

            for (String taskDefArn : response.taskDefinitionArns()) {
                rows.add(new String[]{taskDefArn});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Describe ECS task definition
     * <p>
     * Usage:
     * ecs describe-task-definition --task-definition my-task:1
     * ecs describe-task-definition --task-definition $TASK_DEF
     */
    @ShellMethod(key = "ecs describe-task-definition", value = "Describe ECS task definition")
    public String describeTaskDefinition(String taskDefinition) {
        try {
            taskDefinition = sessionContext.resolveVariables(taskDefinition);

            DescribeTaskDefinitionRequest request = DescribeTaskDefinitionRequest.builder()
                    .taskDefinition(taskDefinition)
                    .build();

            DescribeTaskDefinitionResponse response = ecsClient.describeTaskDefinition(request);
            TaskDefinition taskDef = response.taskDefinition();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Family", taskDef.family()});
            pairs.add(new String[]{"Revision", String.valueOf(taskDef.revision())});
            pairs.add(new String[]{"Status", taskDef.statusAsString()});
            pairs.add(new String[]{"Network Mode", taskDef.networkModeAsString()});
            pairs.add(new String[]{"CPU", taskDef.cpu() != null ? taskDef.cpu() : "N/A"});
            pairs.add(new String[]{"Memory", taskDef.memory() != null ? taskDef.memory() : "N/A"});
            pairs.add(new String[]{"Requires Compatibilities", taskDef.requiresCompatibilities().toString()});
            pairs.add(new String[]{"Task Role ARN", taskDef.taskRoleArn() != null ? taskDef.taskRoleArn() : "N/A"});
            pairs.add(new String[]{"Execution Role ARN", taskDef.executionRoleArn() != null ? taskDef.executionRoleArn() : "N/A"});

            StringBuilder result = new StringBuilder(OutputFormatter.toKeyValue(pairs));
            result.append("\n\nContainers:\n");

            List<String[]> containerRows = new ArrayList<>();
            containerRows.add(new String[]{"Name", "Image", "CPU", "Memory", "Essential"});

            for (ContainerDefinition container : taskDef.containerDefinitions()) {
                containerRows.add(new String[]{
                        container.name(),
                        container.image(),
                        container.cpu() != null ? String.valueOf(container.cpu()) : "0",
                        container.memory() != null ? String.valueOf(container.memory()) : "N/A",
                        String.valueOf(container.essential())
                });
            }

            result.append(OutputFormatter.toTable(containerRows));

            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Stop ECS task
     * <p>
     * Usage:
     * ecs stop-task --cluster my-cluster --task task-id
     * ecs stop-task --cluster $CLUSTER_NAME --task $TASK_ID --reason "Manual stop"
     */
    @ShellMethod(key = "ecs stop-task", value = "Stop ECS task")
    public String stopTask(String cluster, String task, @ShellOption(defaultValue = "") String reason) {
        try {
            cluster = sessionContext.resolveVariables(cluster);
            task = sessionContext.resolveVariables(task);
            reason = sessionContext.resolveVariables(reason);

            StopTaskRequest.Builder requestBuilder = StopTaskRequest.builder()
                    .cluster(cluster)
                    .task(task);

            if (!reason.isEmpty()) {
                requestBuilder.reason(reason);
            }

            StopTaskResponse response = ecsClient.stopTask(requestBuilder.build());
            Task stoppedTask = response.task();

            String taskId = stoppedTask.taskArn();
            if (taskId.contains("/")) {
                taskId = taskId.substring(taskId.lastIndexOf("/") + 1);
            }

            return "Task stopped: " + taskId + " (Status: " + stoppedTask.lastStatus() + ")";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Run ECS task
     * <p>
     * Usage:
     * ecs run-task --cluster my-cluster --task-definition my-task:1
     * ecs run-task --cluster $CLUSTER_NAME --task-definition $TASK_DEF --count 2
     */
    @ShellMethod(key = "ecs run-task", value = "Run ECS task")
    public String runTask(String cluster, String taskDefinition, @ShellOption(defaultValue = "1") int count) {
        try {
            cluster = sessionContext.resolveVariables(cluster);
            taskDefinition = sessionContext.resolveVariables(taskDefinition);

            RunTaskRequest request = RunTaskRequest.builder()
                    .cluster(cluster)
                    .taskDefinition(taskDefinition)
                    .count(count)
                    .build();

            RunTaskResponse response = ecsClient.runTask(request);

            if (response.tasks().isEmpty()) {
                return "No tasks were started";
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Task ID", "Status", "Desired Status"});

            for (Task task : response.tasks()) {
                String taskId = task.taskArn();
                if (taskId.contains("/")) {
                    taskId = taskId.substring(taskId.lastIndexOf("/") + 1);
                }

                rows.add(new String[]{
                        taskId,
                        task.lastStatus(),
                        task.desiredStatus()
                });
            }

            return "Started " + response.tasks().size() + " task(s):\n" + OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update ECS service
     * <p>
     * Usage:
     * ecs update-service --cluster my-cluster --service my-service --desired-count 3
     * ecs update-service --cluster $CLUSTER_NAME --service $SERVICE_NAME --task-definition my-task:2
     */
    @ShellMethod(key = "ecs update-service", value = "Update ECS service")
    public String updateService(String cluster, String service,
                                @ShellOption(defaultValue = "-1") int desiredCount,
                                @ShellOption(defaultValue = "") String taskDefinition) {
        try {
            cluster = sessionContext.resolveVariables(cluster);
            service = sessionContext.resolveVariables(service);
            taskDefinition = sessionContext.resolveVariables(taskDefinition);

            UpdateServiceRequest.Builder requestBuilder = UpdateServiceRequest.builder()
                    .cluster(cluster)
                    .service(service);

            if (desiredCount >= 0) {
                requestBuilder.desiredCount(desiredCount);
            }

            if (!taskDefinition.isEmpty()) {
                requestBuilder.taskDefinition(taskDefinition);
            }

            UpdateServiceResponse response = ecsClient.updateService(requestBuilder.build());
            Service updatedService = response.service();

            return "Service updated: " + updatedService.serviceName() +
                   " (Desired: " + updatedService.desiredCount() +
                   ", Running: " + updatedService.runningCount() + ")";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
