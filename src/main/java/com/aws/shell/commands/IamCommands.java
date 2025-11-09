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

    // ==================== User Management ====================

    /**
     * Create a new IAM user
     * <p>
     * Usage:
     * iam create-user --user-name username
     * iam create-user --user-name $USER_NAME --path /division/
     */
    @ShellMethod(key = "iam create-user", value = "Create a new IAM user")
    public String createUser(String userName,
                             @ShellOption(defaultValue = "/") String path,
                             @ShellOption(defaultValue = "") String tags) {
        try {
            userName = sessionContext.resolveVariables(userName);
            path = sessionContext.resolveVariables(path);
            tags = sessionContext.resolveVariables(tags);

            CreateUserRequest.Builder requestBuilder = CreateUserRequest.builder()
                    .userName(userName)
                    .path(path);

            if (!tags.isEmpty()) {
                List<Tag> tagList = new ArrayList<>();
                for (String tag : tags.split(",")) {
                    String[] parts = tag.trim().split("=");
                    if (parts.length == 2) {
                        tagList.add(Tag.builder()
                                .key(parts[0].trim())
                                .value(parts[1].trim())
                                .build());
                    }
                }
                if (!tagList.isEmpty()) {
                    requestBuilder.tags(tagList);
                }
            }

            CreateUserResponse response = iamClient.createUser(requestBuilder.build());
            User user = response.user();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"User Name", user.userName()});
            pairs.add(new String[]{"User ID", user.userId()});
            pairs.add(new String[]{"ARN", user.arn()});
            pairs.add(new String[]{"Path", user.path()});
            pairs.add(new String[]{"Created", user.createDate().toString()});

            return "User created successfully:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an IAM user
     * <p>
     * Usage:
     * iam delete-user --user-name username
     * iam delete-user --user-name $USER_NAME
     */
    @ShellMethod(key = "iam delete-user", value = "Delete an IAM user")
    public String deleteUser(String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            DeleteUserRequest request = DeleteUserRequest.builder()
                    .userName(userName)
                    .build();

            iamClient.deleteUser(request);

            return "User deleted successfully: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update an IAM user
     * <p>
     * Usage:
     * iam update-user --user-name oldname --new-user-name newname
     * iam update-user --user-name $USER_NAME --new-path /new/path/
     */
    @ShellMethod(key = "iam update-user", value = "Update an IAM user")
    public String updateUser(String userName,
                             @ShellOption(defaultValue = "") String newUserName,
                             @ShellOption(defaultValue = "") String newPath) {
        try {
            userName = sessionContext.resolveVariables(userName);
            newUserName = sessionContext.resolveVariables(newUserName);
            newPath = sessionContext.resolveVariables(newPath);

            UpdateUserRequest.Builder requestBuilder = UpdateUserRequest.builder()
                    .userName(userName);

            if (!newUserName.isEmpty()) {
                requestBuilder.newUserName(newUserName);
            }
            if (!newPath.isEmpty()) {
                requestBuilder.newPath(newPath);
            }

            iamClient.updateUser(requestBuilder.build());

            return "User updated successfully: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create a login profile for a user
     * <p>
     * Usage:
     * iam create-login-profile --user-name username --password mypassword
     */
    @ShellMethod(key = "iam create-login-profile", value = "Create a login profile for a user")
    public String createLoginProfile(String userName,
                                     String password,
                                     @ShellOption(defaultValue = "false") boolean passwordResetRequired) {
        try {
            userName = sessionContext.resolveVariables(userName);
            password = sessionContext.resolveVariables(password);

            CreateLoginProfileRequest request = CreateLoginProfileRequest.builder()
                    .userName(userName)
                    .password(password)
                    .passwordResetRequired(passwordResetRequired)
                    .build();

            CreateLoginProfileResponse response = iamClient.createLoginProfile(request);

            return "Login profile created successfully for user: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete a login profile for a user
     * <p>
     * Usage:
     * iam delete-login-profile --user-name username
     */
    @ShellMethod(key = "iam delete-login-profile", value = "Delete a login profile for a user")
    public String deleteLoginProfile(String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            DeleteLoginProfileRequest request = DeleteLoginProfileRequest.builder()
                    .userName(userName)
                    .build();

            iamClient.deleteLoginProfile(request);

            return "Login profile deleted successfully for user: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Role Management ====================

    /**
     * Create a new IAM role
     * <p>
     * Usage:
     * iam create-role --role-name rolename --assume-role-policy-document '{"Version":"2012-10-17",...}'
     */
    @ShellMethod(key = "iam create-role", value = "Create a new IAM role")
    public String createRole(String roleName,
                             String assumeRolePolicyDocument,
                             @ShellOption(defaultValue = "") String description,
                             @ShellOption(defaultValue = "/") String path,
                             @ShellOption(defaultValue = "3600") int maxSessionDuration) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            assumeRolePolicyDocument = sessionContext.resolveVariables(assumeRolePolicyDocument);
            description = sessionContext.resolveVariables(description);
            path = sessionContext.resolveVariables(path);

            CreateRoleRequest.Builder requestBuilder = CreateRoleRequest.builder()
                    .roleName(roleName)
                    .assumeRolePolicyDocument(assumeRolePolicyDocument)
                    .path(path)
                    .maxSessionDuration(maxSessionDuration);

            if (!description.isEmpty()) {
                requestBuilder.description(description);
            }

            CreateRoleResponse response = iamClient.createRole(requestBuilder.build());
            Role role = response.role();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Role Name", role.roleName()});
            pairs.add(new String[]{"Role ID", role.roleId()});
            pairs.add(new String[]{"ARN", role.arn()});
            pairs.add(new String[]{"Path", role.path()});
            pairs.add(new String[]{"Created", role.createDate().toString()});

            return "Role created successfully:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an IAM role
     * <p>
     * Usage:
     * iam delete-role --role-name rolename
     */
    @ShellMethod(key = "iam delete-role", value = "Delete an IAM role")
    public String deleteRole(String roleName) {
        try {
            roleName = sessionContext.resolveVariables(roleName);

            DeleteRoleRequest request = DeleteRoleRequest.builder()
                    .roleName(roleName)
                    .build();

            iamClient.deleteRole(request);

            return "Role deleted successfully: " + roleName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update an IAM role
     * <p>
     * Usage:
     * iam update-role --role-name rolename --description "New description"
     */
    @ShellMethod(key = "iam update-role", value = "Update an IAM role")
    public String updateRole(String roleName,
                             @ShellOption(defaultValue = "") String description,
                             @ShellOption(defaultValue = "0") int maxSessionDuration) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            description = sessionContext.resolveVariables(description);

            UpdateRoleRequest.Builder requestBuilder = UpdateRoleRequest.builder()
                    .roleName(roleName);

            if (!description.isEmpty()) {
                requestBuilder.description(description);
            }
            if (maxSessionDuration > 0) {
                requestBuilder.maxSessionDuration(maxSessionDuration);
            }

            iamClient.updateRole(requestBuilder.build());

            return "Role updated successfully: " + roleName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Attach a managed policy to a role
     * <p>
     * Usage:
     * iam attach-role-policy --role-name rolename --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
     */
    @ShellMethod(key = "iam attach-role-policy", value = "Attach a managed policy to a role")
    public String attachRolePolicy(String roleName, String policyArn) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            policyArn = sessionContext.resolveVariables(policyArn);

            AttachRolePolicyRequest request = AttachRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyArn(policyArn)
                    .build();

            iamClient.attachRolePolicy(request);

            return "Policy attached successfully to role: " + roleName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Detach a managed policy from a role
     * <p>
     * Usage:
     * iam detach-role-policy --role-name rolename --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
     */
    @ShellMethod(key = "iam detach-role-policy", value = "Detach a managed policy from a role")
    public String detachRolePolicy(String roleName, String policyArn) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            policyArn = sessionContext.resolveVariables(policyArn);

            DetachRolePolicyRequest request = DetachRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyArn(policyArn)
                    .build();

            iamClient.detachRolePolicy(request);

            return "Policy detached successfully from role: " + roleName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List attached role policies
     * <p>
     * Usage:
     * iam list-attached-role-policies --role-name rolename
     */
    @ShellMethod(key = "iam list-attached-role-policies", value = "List attached role policies")
    public String listAttachedRolePolicies(String roleName) {
        try {
            roleName = sessionContext.resolveVariables(roleName);

            ListAttachedRolePoliciesRequest request = ListAttachedRolePoliciesRequest.builder()
                    .roleName(roleName)
                    .build();

            ListAttachedRolePoliciesResponse response = iamClient.listAttachedRolePolicies(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Policy Name", "Policy ARN"});

            for (AttachedPolicy policy : response.attachedPolicies()) {
                rows.add(new String[]{
                        policy.policyName(),
                        policy.policyArn()
                });
            }

            if (rows.size() == 1) {
                return "No policies attached to role: " + roleName;
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Policy Management ====================

    /**
     * Create a new IAM policy
     * <p>
     * Usage:
     * iam create-policy --policy-name policyname --policy-document '{"Version":"2012-10-17",...}'
     */
    @ShellMethod(key = "iam create-policy", value = "Create a new IAM policy")
    public String createPolicy(String policyName,
                               String policyDocument,
                               @ShellOption(defaultValue = "") String description,
                               @ShellOption(defaultValue = "/") String path) {
        try {
            policyName = sessionContext.resolveVariables(policyName);
            policyDocument = sessionContext.resolveVariables(policyDocument);
            description = sessionContext.resolveVariables(description);
            path = sessionContext.resolveVariables(path);

            CreatePolicyRequest.Builder requestBuilder = CreatePolicyRequest.builder()
                    .policyName(policyName)
                    .policyDocument(policyDocument)
                    .path(path);

            if (!description.isEmpty()) {
                requestBuilder.description(description);
            }

            CreatePolicyResponse response = iamClient.createPolicy(requestBuilder.build());
            Policy policy = response.policy();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Policy Name", policy.policyName()});
            pairs.add(new String[]{"Policy ID", policy.policyId()});
            pairs.add(new String[]{"ARN", policy.arn()});
            pairs.add(new String[]{"Path", policy.path()});
            pairs.add(new String[]{"Created", policy.createDate().toString()});

            return "Policy created successfully:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an IAM policy
     * <p>
     * Usage:
     * iam delete-policy --policy-arn arn:aws:iam::123456789012:policy/mypolicy
     */
    @ShellMethod(key = "iam delete-policy", value = "Delete an IAM policy")
    public String deletePolicy(String policyArn) {
        try {
            policyArn = sessionContext.resolveVariables(policyArn);

            DeletePolicyRequest request = DeletePolicyRequest.builder()
                    .policyArn(policyArn)
                    .build();

            iamClient.deletePolicy(request);

            return "Policy deleted successfully: " + policyArn;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get details about an IAM policy
     * <p>
     * Usage:
     * iam get-policy --policy-arn arn:aws:iam::123456789012:policy/mypolicy
     */
    @ShellMethod(key = "iam get-policy", value = "Get details about an IAM policy")
    public String getPolicy(String policyArn) {
        try {
            policyArn = sessionContext.resolveVariables(policyArn);

            GetPolicyRequest request = GetPolicyRequest.builder()
                    .policyArn(policyArn)
                    .build();

            GetPolicyResponse response = iamClient.getPolicy(request);
            Policy policy = response.policy();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Policy Name", policy.policyName()});
            pairs.add(new String[]{"Policy ID", policy.policyId()});
            pairs.add(new String[]{"ARN", policy.arn()});
            pairs.add(new String[]{"Path", policy.path()});
            pairs.add(new String[]{"Default Version", policy.defaultVersionId()});
            pairs.add(new String[]{"Attachment Count", String.valueOf(policy.attachmentCount())});
            pairs.add(new String[]{"Created", policy.createDate().toString()});
            pairs.add(new String[]{"Updated", policy.updateDate().toString()});

            if (policy.description() != null) {
                pairs.add(new String[]{"Description", policy.description()});
            }

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get a specific version of an IAM policy
     * <p>
     * Usage:
     * iam get-policy-version --policy-arn arn:aws:iam::123456789012:policy/mypolicy --version-id v1
     */
    @ShellMethod(key = "iam get-policy-version", value = "Get a specific version of an IAM policy")
    public String getPolicyVersion(String policyArn, String versionId) {
        try {
            policyArn = sessionContext.resolveVariables(policyArn);
            versionId = sessionContext.resolveVariables(versionId);

            GetPolicyVersionRequest request = GetPolicyVersionRequest.builder()
                    .policyArn(policyArn)
                    .versionId(versionId)
                    .build();

            GetPolicyVersionResponse response = iamClient.getPolicyVersion(request);
            PolicyVersion policyVersion = response.policyVersion();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Version ID", policyVersion.versionId()});
            pairs.add(new String[]{"Is Default", String.valueOf(policyVersion.isDefaultVersion())});
            pairs.add(new String[]{"Created", policyVersion.createDate().toString()});
            pairs.add(new String[]{"Document", policyVersion.document()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Attach/Detach User Policies ====================

    /**
     * Attach a managed policy to a user
     * <p>
     * Usage:
     * iam attach-user-policy --user-name username --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
     */
    @ShellMethod(key = "iam attach-user-policy", value = "Attach a managed policy to a user")
    public String attachUserPolicy(String userName, String policyArn) {
        try {
            userName = sessionContext.resolveVariables(userName);
            policyArn = sessionContext.resolveVariables(policyArn);

            AttachUserPolicyRequest request = AttachUserPolicyRequest.builder()
                    .userName(userName)
                    .policyArn(policyArn)
                    .build();

            iamClient.attachUserPolicy(request);

            return "Policy attached successfully to user: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Detach a managed policy from a user
     * <p>
     * Usage:
     * iam detach-user-policy --user-name username --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
     */
    @ShellMethod(key = "iam detach-user-policy", value = "Detach a managed policy from a user")
    public String detachUserPolicy(String userName, String policyArn) {
        try {
            userName = sessionContext.resolveVariables(userName);
            policyArn = sessionContext.resolveVariables(policyArn);

            DetachUserPolicyRequest request = DetachUserPolicyRequest.builder()
                    .userName(userName)
                    .policyArn(policyArn)
                    .build();

            iamClient.detachUserPolicy(request);

            return "Policy detached successfully from user: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Access Key Management ====================

    /**
     * Create an access key for a user
     * <p>
     * Usage:
     * iam create-access-key --user-name username
     */
    @ShellMethod(key = "iam create-access-key", value = "Create an access key for a user")
    public String createAccessKey(@ShellOption(defaultValue = "") String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            CreateAccessKeyRequest.Builder requestBuilder = CreateAccessKeyRequest.builder();

            if (!userName.isEmpty()) {
                requestBuilder.userName(userName);
            }

            CreateAccessKeyResponse response = iamClient.createAccessKey(requestBuilder.build());
            AccessKey accessKey = response.accessKey();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"User Name", accessKey.userName()});
            pairs.add(new String[]{"Access Key ID", accessKey.accessKeyId()});
            pairs.add(new String[]{"Secret Access Key", accessKey.secretAccessKey()});
            pairs.add(new String[]{"Status", accessKey.status().toString()});
            pairs.add(new String[]{"Created", accessKey.createDate().toString()});

            return "Access key created successfully:\n" + OutputFormatter.toKeyValue(pairs) +
                    "\n\nWARNING: Save the Secret Access Key securely. It cannot be retrieved again.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an access key
     * <p>
     * Usage:
     * iam delete-access-key --access-key-id AKIAIOSFODNN7EXAMPLE --user-name username
     */
    @ShellMethod(key = "iam delete-access-key", value = "Delete an access key")
    public String deleteAccessKey(String accessKeyId,
                                  @ShellOption(defaultValue = "") String userName) {
        try {
            accessKeyId = sessionContext.resolveVariables(accessKeyId);
            userName = sessionContext.resolveVariables(userName);

            DeleteAccessKeyRequest.Builder requestBuilder = DeleteAccessKeyRequest.builder()
                    .accessKeyId(accessKeyId);

            if (!userName.isEmpty()) {
                requestBuilder.userName(userName);
            }

            iamClient.deleteAccessKey(requestBuilder.build());

            return "Access key deleted successfully: " + accessKeyId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List access keys for a user
     * <p>
     * Usage:
     * iam list-access-keys --user-name username
     */
    @ShellMethod(key = "iam list-access-keys", value = "List access keys for a user")
    public String listAccessKeys(@ShellOption(defaultValue = "") String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            ListAccessKeysRequest.Builder requestBuilder = ListAccessKeysRequest.builder();

            if (!userName.isEmpty()) {
                requestBuilder.userName(userName);
            }

            ListAccessKeysResponse response = iamClient.listAccessKeys(requestBuilder.build());

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"User Name", "Access Key ID", "Status", "Created"});

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (AccessKeyMetadata key : response.accessKeyMetadata()) {
                rows.add(new String[]{
                        key.userName(),
                        key.accessKeyId(),
                        key.status().toString(),
                        key.createDate().atZone(ZoneId.systemDefault()).format(formatter)
                });
            }

            if (rows.size() == 1) {
                return "No access keys found";
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Update an access key status
     * <p>
     * Usage:
     * iam update-access-key --access-key-id AKIAIOSFODNN7EXAMPLE --status Active
     */
    @ShellMethod(key = "iam update-access-key", value = "Update an access key status")
    public String updateAccessKey(String accessKeyId,
                                  String status,
                                  @ShellOption(defaultValue = "") String userName) {
        try {
            accessKeyId = sessionContext.resolveVariables(accessKeyId);
            status = sessionContext.resolveVariables(status);
            userName = sessionContext.resolveVariables(userName);

            UpdateAccessKeyRequest.Builder requestBuilder = UpdateAccessKeyRequest.builder()
                    .accessKeyId(accessKeyId)
                    .status(StatusType.fromValue(status));

            if (!userName.isEmpty()) {
                requestBuilder.userName(userName);
            }

            iamClient.updateAccessKey(requestBuilder.build());

            return "Access key updated successfully: " + accessKeyId + " -> " + status;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Group Management ====================

    /**
     * List IAM groups
     * <p>
     * Usage:
     * iam list-groups
     */
    @ShellMethod(key = "iam list-groups", value = "List IAM groups")
    public String listGroups() {
        try {
            ListGroupsResponse response = iamClient.listGroups();

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
                return "No groups found";
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create a new IAM group
     * <p>
     * Usage:
     * iam create-group --group-name groupname
     */
    @ShellMethod(key = "iam create-group", value = "Create a new IAM group")
    public String createGroup(String groupName,
                              @ShellOption(defaultValue = "/") String path) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            path = sessionContext.resolveVariables(path);

            CreateGroupRequest request = CreateGroupRequest.builder()
                    .groupName(groupName)
                    .path(path)
                    .build();

            CreateGroupResponse response = iamClient.createGroup(request);
            Group group = response.group();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Group Name", group.groupName()});
            pairs.add(new String[]{"Group ID", group.groupId()});
            pairs.add(new String[]{"ARN", group.arn()});
            pairs.add(new String[]{"Path", group.path()});
            pairs.add(new String[]{"Created", group.createDate().toString()});

            return "Group created successfully:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an IAM group
     * <p>
     * Usage:
     * iam delete-group --group-name groupname
     */
    @ShellMethod(key = "iam delete-group", value = "Delete an IAM group")
    public String deleteGroup(String groupName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);

            DeleteGroupRequest request = DeleteGroupRequest.builder()
                    .groupName(groupName)
                    .build();

            iamClient.deleteGroup(request);

            return "Group deleted successfully: " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get details about an IAM group
     * <p>
     * Usage:
     * iam get-group --group-name groupname
     */
    @ShellMethod(key = "iam get-group", value = "Get details about an IAM group")
    public String getGroup(String groupName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);

            GetGroupRequest request = GetGroupRequest.builder()
                    .groupName(groupName)
                    .build();

            GetGroupResponse response = iamClient.getGroup(request);
            Group group = response.group();

            StringBuilder result = new StringBuilder();

            List<String[]> groupPairs = new ArrayList<>();
            groupPairs.add(new String[]{"Group Name", group.groupName()});
            groupPairs.add(new String[]{"Group ID", group.groupId()});
            groupPairs.add(new String[]{"ARN", group.arn()});
            groupPairs.add(new String[]{"Path", group.path()});
            groupPairs.add(new String[]{"Created", group.createDate().toString()});

            result.append("Group Details:\n");
            result.append(OutputFormatter.toKeyValue(groupPairs));

            if (!response.users().isEmpty()) {
                result.append("\n\nGroup Members:\n");
                List<String[]> userRows = new ArrayList<>();
                userRows.add(new String[]{"User Name", "User ID", "ARN"});

                for (User user : response.users()) {
                    userRows.add(new String[]{
                            user.userName(),
                            user.userId(),
                            user.arn()
                    });
                }

                result.append(OutputFormatter.toTable(userRows));
            } else {
                result.append("\n\nNo users in this group");
            }

            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Add a user to a group
     * <p>
     * Usage:
     * iam add-user-to-group --group-name groupname --user-name username
     */
    @ShellMethod(key = "iam add-user-to-group", value = "Add a user to a group")
    public String addUserToGroup(String groupName, String userName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            userName = sessionContext.resolveVariables(userName);

            AddUserToGroupRequest request = AddUserToGroupRequest.builder()
                    .groupName(groupName)
                    .userName(userName)
                    .build();

            iamClient.addUserToGroup(request);

            return "User added to group successfully: " + userName + " -> " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Remove a user from a group
     * <p>
     * Usage:
     * iam remove-user-from-group --group-name groupname --user-name username
     */
    @ShellMethod(key = "iam remove-user-from-group", value = "Remove a user from a group")
    public String removeUserFromGroup(String groupName, String userName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            userName = sessionContext.resolveVariables(userName);

            RemoveUserFromGroupRequest request = RemoveUserFromGroupRequest.builder()
                    .groupName(groupName)
                    .userName(userName)
                    .build();

            iamClient.removeUserFromGroup(request);

            return "User removed from group successfully: " + userName + " <- " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Attach a managed policy to a group
     * <p>
     * Usage:
     * iam attach-group-policy --group-name groupname --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
     */
    @ShellMethod(key = "iam attach-group-policy", value = "Attach a managed policy to a group")
    public String attachGroupPolicy(String groupName, String policyArn) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            policyArn = sessionContext.resolveVariables(policyArn);

            AttachGroupPolicyRequest request = AttachGroupPolicyRequest.builder()
                    .groupName(groupName)
                    .policyArn(policyArn)
                    .build();

            iamClient.attachGroupPolicy(request);

            return "Policy attached successfully to group: " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Detach a managed policy from a group
     * <p>
     * Usage:
     * iam detach-group-policy --group-name groupname --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess
     */
    @ShellMethod(key = "iam detach-group-policy", value = "Detach a managed policy from a group")
    public String detachGroupPolicy(String groupName, String policyArn) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            policyArn = sessionContext.resolveVariables(policyArn);

            DetachGroupPolicyRequest request = DetachGroupPolicyRequest.builder()
                    .groupName(groupName)
                    .policyArn(policyArn)
                    .build();

            iamClient.detachGroupPolicy(request);

            return "Policy detached successfully from group: " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List attached group policies
     * <p>
     * Usage:
     * iam list-attached-group-policies --group-name groupname
     */
    @ShellMethod(key = "iam list-attached-group-policies", value = "List attached group policies")
    public String listAttachedGroupPolicies(String groupName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);

            ListAttachedGroupPoliciesRequest request = ListAttachedGroupPoliciesRequest.builder()
                    .groupName(groupName)
                    .build();

            ListAttachedGroupPoliciesResponse response = iamClient.listAttachedGroupPolicies(request);

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Policy Name", "Policy ARN"});

            for (AttachedPolicy policy : response.attachedPolicies()) {
                rows.add(new String[]{
                        policy.policyName(),
                        policy.policyArn()
                });
            }

            if (rows.size() == 1) {
                return "No policies attached to group: " + groupName;
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Inline Policy Management ====================

    /**
     * Put an inline policy on a user
     * <p>
     * Usage:
     * iam put-user-policy --user-name username --policy-name policyname --policy-document '{"Version":"2012-10-17",...}'
     */
    @ShellMethod(key = "iam put-user-policy", value = "Put an inline policy on a user")
    public String putUserPolicy(String userName, String policyName, String policyDocument) {
        try {
            userName = sessionContext.resolveVariables(userName);
            policyName = sessionContext.resolveVariables(policyName);
            policyDocument = sessionContext.resolveVariables(policyDocument);

            PutUserPolicyRequest request = PutUserPolicyRequest.builder()
                    .userName(userName)
                    .policyName(policyName)
                    .policyDocument(policyDocument)
                    .build();

            iamClient.putUserPolicy(request);

            return "Inline policy added successfully to user: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get an inline policy from a user
     * <p>
     * Usage:
     * iam get-user-policy --user-name username --policy-name policyname
     */
    @ShellMethod(key = "iam get-user-policy", value = "Get an inline policy from a user")
    public String getUserPolicy(String userName, String policyName) {
        try {
            userName = sessionContext.resolveVariables(userName);
            policyName = sessionContext.resolveVariables(policyName);

            GetUserPolicyRequest request = GetUserPolicyRequest.builder()
                    .userName(userName)
                    .policyName(policyName)
                    .build();

            GetUserPolicyResponse response = iamClient.getUserPolicy(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"User Name", response.userName()});
            pairs.add(new String[]{"Policy Name", response.policyName()});
            pairs.add(new String[]{"Policy Document", response.policyDocument()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an inline policy from a user
     * <p>
     * Usage:
     * iam delete-user-policy --user-name username --policy-name policyname
     */
    @ShellMethod(key = "iam delete-user-policy", value = "Delete an inline policy from a user")
    public String deleteUserPolicy(String userName, String policyName) {
        try {
            userName = sessionContext.resolveVariables(userName);
            policyName = sessionContext.resolveVariables(policyName);

            DeleteUserPolicyRequest request = DeleteUserPolicyRequest.builder()
                    .userName(userName)
                    .policyName(policyName)
                    .build();

            iamClient.deleteUserPolicy(request);

            return "Inline policy deleted successfully from user: " + userName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List inline policies for a user
     * <p>
     * Usage:
     * iam list-user-policies --user-name username
     */
    @ShellMethod(key = "iam list-user-policies", value = "List inline policies for a user")
    public String listUserPolicies(String userName) {
        try {
            userName = sessionContext.resolveVariables(userName);

            ListUserPoliciesRequest request = ListUserPoliciesRequest.builder()
                    .userName(userName)
                    .build();

            ListUserPoliciesResponse response = iamClient.listUserPolicies(request);

            if (response.policyNames().isEmpty()) {
                return "No inline policies found for user: " + userName;
            }

            StringBuilder result = new StringBuilder();
            result.append("Inline policies for user: ").append(userName).append("\n");
            for (String policyName : response.policyNames()) {
                result.append("- ").append(policyName).append("\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Put an inline policy on a role
     * <p>
     * Usage:
     * iam put-role-policy --role-name rolename --policy-name policyname --policy-document '{"Version":"2012-10-17",...}'
     */
    @ShellMethod(key = "iam put-role-policy", value = "Put an inline policy on a role")
    public String putRolePolicy(String roleName, String policyName, String policyDocument) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            policyName = sessionContext.resolveVariables(policyName);
            policyDocument = sessionContext.resolveVariables(policyDocument);

            PutRolePolicyRequest request = PutRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName(policyName)
                    .policyDocument(policyDocument)
                    .build();

            iamClient.putRolePolicy(request);

            return "Inline policy added successfully to role: " + roleName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get an inline policy from a role
     * <p>
     * Usage:
     * iam get-role-policy --role-name rolename --policy-name policyname
     */
    @ShellMethod(key = "iam get-role-policy", value = "Get an inline policy from a role")
    public String getRolePolicy(String roleName, String policyName) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            policyName = sessionContext.resolveVariables(policyName);

            GetRolePolicyRequest request = GetRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName(policyName)
                    .build();

            GetRolePolicyResponse response = iamClient.getRolePolicy(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Role Name", response.roleName()});
            pairs.add(new String[]{"Policy Name", response.policyName()});
            pairs.add(new String[]{"Policy Document", response.policyDocument()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an inline policy from a role
     * <p>
     * Usage:
     * iam delete-role-policy --role-name rolename --policy-name policyname
     */
    @ShellMethod(key = "iam delete-role-policy", value = "Delete an inline policy from a role")
    public String deleteRolePolicy(String roleName, String policyName) {
        try {
            roleName = sessionContext.resolveVariables(roleName);
            policyName = sessionContext.resolveVariables(policyName);

            DeleteRolePolicyRequest request = DeleteRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName(policyName)
                    .build();

            iamClient.deleteRolePolicy(request);

            return "Inline policy deleted successfully from role: " + roleName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List inline policies for a role
     * <p>
     * Usage:
     * iam list-role-policies --role-name rolename
     */
    @ShellMethod(key = "iam list-role-policies", value = "List inline policies for a role")
    public String listRolePolicies(String roleName) {
        try {
            roleName = sessionContext.resolveVariables(roleName);

            ListRolePoliciesRequest request = ListRolePoliciesRequest.builder()
                    .roleName(roleName)
                    .build();

            ListRolePoliciesResponse response = iamClient.listRolePolicies(request);

            if (response.policyNames().isEmpty()) {
                return "No inline policies found for role: " + roleName;
            }

            StringBuilder result = new StringBuilder();
            result.append("Inline policies for role: ").append(roleName).append("\n");
            for (String policyName : response.policyNames()) {
                result.append("- ").append(policyName).append("\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Put an inline policy on a group
     * <p>
     * Usage:
     * iam put-group-policy --group-name groupname --policy-name policyname --policy-document '{"Version":"2012-10-17",...}'
     */
    @ShellMethod(key = "iam put-group-policy", value = "Put an inline policy on a group")
    public String putGroupPolicy(String groupName, String policyName, String policyDocument) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            policyName = sessionContext.resolveVariables(policyName);
            policyDocument = sessionContext.resolveVariables(policyDocument);

            PutGroupPolicyRequest request = PutGroupPolicyRequest.builder()
                    .groupName(groupName)
                    .policyName(policyName)
                    .policyDocument(policyDocument)
                    .build();

            iamClient.putGroupPolicy(request);

            return "Inline policy added successfully to group: " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get an inline policy from a group
     * <p>
     * Usage:
     * iam get-group-policy --group-name groupname --policy-name policyname
     */
    @ShellMethod(key = "iam get-group-policy", value = "Get an inline policy from a group")
    public String getGroupPolicy(String groupName, String policyName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            policyName = sessionContext.resolveVariables(policyName);

            GetGroupPolicyRequest request = GetGroupPolicyRequest.builder()
                    .groupName(groupName)
                    .policyName(policyName)
                    .build();

            GetGroupPolicyResponse response = iamClient.getGroupPolicy(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Group Name", response.groupName()});
            pairs.add(new String[]{"Policy Name", response.policyName()});
            pairs.add(new String[]{"Policy Document", response.policyDocument()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete an inline policy from a group
     * <p>
     * Usage:
     * iam delete-group-policy --group-name groupname --policy-name policyname
     */
    @ShellMethod(key = "iam delete-group-policy", value = "Delete an inline policy from a group")
    public String deleteGroupPolicy(String groupName, String policyName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);
            policyName = sessionContext.resolveVariables(policyName);

            DeleteGroupPolicyRequest request = DeleteGroupPolicyRequest.builder()
                    .groupName(groupName)
                    .policyName(policyName)
                    .build();

            iamClient.deleteGroupPolicy(request);

            return "Inline policy deleted successfully from group: " + groupName;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * List inline policies for a group
     * <p>
     * Usage:
     * iam list-group-policies --group-name groupname
     */
    @ShellMethod(key = "iam list-group-policies", value = "List inline policies for a group")
    public String listGroupPolicies(String groupName) {
        try {
            groupName = sessionContext.resolveVariables(groupName);

            ListGroupPoliciesRequest request = ListGroupPoliciesRequest.builder()
                    .groupName(groupName)
                    .build();

            ListGroupPoliciesResponse response = iamClient.listGroupPolicies(request);

            if (response.policyNames().isEmpty()) {
                return "No inline policies found for group: " + groupName;
            }

            StringBuilder result = new StringBuilder();
            result.append("Inline policies for group: ").append(groupName).append("\n");
            for (String policyName : response.policyNames()) {
                result.append("- ").append(policyName).append("\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
