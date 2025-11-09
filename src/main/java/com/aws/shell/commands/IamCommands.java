package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * IAM Commands
 * <p>
 * Implements AWS IAM CLI-like functionality with variable support
 */
@ShellComponent
public class IamCommands {

    private final IamClient iamClient;
    private final SessionContext sessionContext;

    public IamCommands(IamClient iamClient, SessionContext sessionContext) {
        this.iamClient = iamClient;
        this.sessionContext = sessionContext;
    }

    /**
     * List IAM users
     * <p>
     * Usage:
     * iam list-users
     */
    @ShellMethod(key = "iam list-users", value = "List IAM users")
    public String listUsers() {
        try {
            ListUsersResponse response = iamClient.listUsers();

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"User Name", "User ID", "ARN", "Created"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (User user : response.users()) {
                rows.add(new String[]{
                        user.userName(),
                        user.userId(),
                        user.arn(),
                        user.createDate().atZone(ZoneId.systemDefault()).format(formatter)
                });
            }

            if (rows.size() == 1) {
                return "No users found";
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get IAM user details
     * <p>
     * Usage:
     * iam get-user --user-name username
     * iam get-user --user-name $USER_NAME
     */
    @ShellMethod(key = "iam get-user", value = "Get IAM user details")
    public String getUser(@ShellOption(defaultValue = "") String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            GetUserRequest.Builder requestBuilder = GetUserRequest.builder();

            if (!userName.isEmpty()) {
                requestBuilder.userName(userName);
            }

            GetUserResponse response = iamClient.getUser(requestBuilder.build());
            User user = response.user();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"User Name", user.userName()});
            pairs.add(new String[]{"User ID", user.userId()});
            pairs.add(new String[]{"ARN", user.arn()});
            pairs.add(new String[]{"Path", user.path()});
            pairs.add(new String[]{"Created", user.createDate().toString()});

            if (user.passwordLastUsed() != null) {
                pairs.add(new String[]{"Password Last Used", user.passwordLastUsed().toString()});
            }

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List IAM roles
     * <p>
     * Usage:
     * iam list-roles
     */
    @ShellMethod(key = "iam list-roles", value = "List IAM roles")
    public String listRoles() {
        try {
            ListRolesResponse response = iamClient.listRoles();

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Role Name", "Role ID", "ARN", "Created"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Role role : response.roles()) {
                rows.add(new String[]{
                        role.roleName(),
                        role.roleId(),
                        role.arn(),
                        role.createDate().atZone(ZoneId.systemDefault()).format(formatter)
                });
            }

            if (rows.size() == 1) {
                return "No roles found";
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get IAM role details
     * <p>
     * Usage:
     * iam get-role --role-name rolename
     * iam get-role --role-name $ROLE_NAME
     */
    @ShellMethod(key = "iam get-role", value = "Get IAM role details")
    public String getRole(String roleName) {
        try {
            roleName = sessionContext.resolveVariables(roleName);

            GetRoleRequest request = GetRoleRequest.builder()
                    .roleName(roleName)
                    .build();

            GetRoleResponse response = iamClient.getRole(request);
            Role role = response.role();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Role Name", role.roleName()});
            pairs.add(new String[]{"Role ID", role.roleId()});
            pairs.add(new String[]{"ARN", role.arn()});
            pairs.add(new String[]{"Path", role.path()});
            pairs.add(new String[]{"Created", role.createDate().toString()});
            pairs.add(new String[]{"Max Session Duration", role.maxSessionDuration().toString()});

            if (role.description() != null) {
                pairs.add(new String[]{"Description", role.description()});
            }

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List IAM policies
     * <p>
     * Usage:
     * iam list-policies
     * iam list-policies --scope Local
     */
    @ShellMethod(key = "iam list-policies", value = "List IAM policies")
    public String listPolicies(@ShellOption(defaultValue = "All") String scope) {
        try {
            ListPoliciesRequest.Builder requestBuilder = ListPoliciesRequest.builder();

            if (!"All".equals(scope)) {
                requestBuilder.scope(PolicyScopeType.fromValue(scope));
            }

            ListPoliciesResponse response = iamClient.listPolicies(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Policy Name", "Policy ID", "ARN", "Attachment Count"});

            for (Policy policy : response.policies()) {
                rows.add(new String[]{
                        policy.policyName(),
                        policy.policyId(),
                        policy.arn(),
                        String.valueOf(policy.attachmentCount())
                });
            }

            if (rows.size() == 1) {
                return "No policies found";
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List groups for a user
     * <p>
     * Usage:
     * iam list-groups-for-user --user-name username
     * iam list-groups-for-user --user-name $USER_NAME
     */
    @ShellMethod(key = "iam list-groups-for-user", value = "List groups for a user")
    public String listGroupsForUser(String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            ListGroupsForUserRequest request = ListGroupsForUserRequest.builder()
                    .userName(userName)
                    .build();

            ListGroupsForUserResponse response = iamClient.listGroupsForUser(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Group Name", "Group ID", "ARN", "Created"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Group group : response.groups()) {
                rows.add(new String[]{
                        group.groupName(),
                        group.groupId(),
                        group.arn(),
                        group.createDate().atZone(ZoneId.systemDefault()).format(formatter)
                });
            }

            if (rows.size() == 1) {
                return "No groups found for user: " + userName;
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List attached user policies
     * <p>
     * Usage:
     * iam list-attached-user-policies --user-name username
     * iam list-attached-user-policies --user-name $USER_NAME
     */
    @ShellMethod(key = "iam list-attached-user-policies", value = "List attached user policies")
    public String listAttachedUserPolicies(String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            ListAttachedUserPoliciesRequest request = ListAttachedUserPoliciesRequest.builder()
                    .userName(userName)
                    .build();

            ListAttachedUserPoliciesResponse response = iamClient.listAttachedUserPolicies(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Policy Name", "Policy ARN"});

            for (AttachedPolicy policy : response.attachedPolicies()) {
                rows.add(new String[]{
                        policy.policyName(),
                        policy.policyArn()
                });
            }

            if (rows.size() == 1) {
                return "No policies attached to user: " + userName;
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
