package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * S3 Commands
 * <p>
 * Implements AWS S3 CLI-like functionality with variable support
 */
@ShellComponent
public class S3Commands {

    private final S3Client s3Client;
    private final SessionContext sessionContext;

    public S3Commands(S3Client s3Client, SessionContext sessionContext) {
        this.s3Client = s3Client;
        this.sessionContext = sessionContext;
    }

    /**
     * List S3 buckets or objects in a bucket
     * <p>
     * Usage:
     * s3 ls                    - List all buckets
     * s3 ls s3://bucket-name   - List objects in bucket
     * s3 ls s3://bucket/prefix - List objects with prefix
     * s3 ls s3://$BUCKET       - Use variable
     */
    @ShellMethod(key = "s3 ls", value = "List S3 buckets or objects")
    public String list(@ShellOption(defaultValue = "") String path) {
        try {
            // Resolve variables
            path = sessionContext.resolveVariables(path);

            if (path.isEmpty()) {
                // List all buckets
                return listBuckets();
            } else {
                // Parse s3://bucket/prefix
                S3Path s3Path = parseS3Path(path);
                return listObjects(s3Path.bucket, s3Path.prefix);
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create a new S3 bucket
     * <p>
     * Usage:
     * s3 mb s3://bucket-name
     * s3 mb s3://$BUCKET
     */
    @ShellMethod(key = "s3 mb", value = "Create an S3 bucket")
    public String makeBucket(String bucketUri) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            S3Path s3Path = parseS3Path(bucketUri);

            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            s3Client.createBucket(request);
            return "Bucket created: " + s3Path.bucket;
        } catch (Exception e) {
            return "Error creating bucket: " + e.getMessage();
        }
    }

    /**
     * Remove an S3 bucket
     * <p>
     * Usage:
     * s3 rb s3://bucket-name
     * s3 rb s3://$BUCKET
     */
    @ShellMethod(key = "s3 rb", value = "Remove an S3 bucket")
    public String removeBucket(String bucketUri) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            S3Path s3Path = parseS3Path(bucketUri);

            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            s3Client.deleteBucket(request);
            return "Bucket deleted: " + s3Path.bucket;
        } catch (Exception e) {
            return "Error deleting bucket: " + e.getMessage();
        }
    }

    /**
     * Copy files to/from S3
     * <p>
     * Usage:
     * s3 cp local-file s3://bucket/key     - Upload file
     * s3 cp s3://bucket/key local-file     - Download file
     * s3 cp s3://bucket1/key s3://bucket2/key - Copy between buckets
     * s3 cp $LOCAL_FILE s3://$BUCKET/$KEY  - Use variables
     */
    @ShellMethod(key = "s3 cp", value = "Copy files to/from S3")
    public String copy(String source, String destination) {
        try {
            source = sessionContext.resolveVariables(source);
            destination = sessionContext.resolveVariables(destination);

            boolean sourceIsS3 = source.startsWith("s3://");
            boolean destIsS3 = destination.startsWith("s3://");

            if (!sourceIsS3 && destIsS3) {
                // Upload local file to S3
                return uploadFile(source, destination);
            } else if (sourceIsS3 && !destIsS3) {
                // Download S3 file to local
                return downloadFile(source, destination);
            } else if (sourceIsS3 && destIsS3) {
                // Copy between S3 locations
                return copyS3Object(source, destination);
            } else {
                return "Error: At least one path must be an S3 URI (s3://...)";
            }
        } catch (Exception e) {
            return "Error copying: " + e.getMessage();
        }
    }

    /**
     * Remove S3 objects
     * <p>
     * Usage:
     * s3 rm s3://bucket/key
     * s3 rm s3://$BUCKET/$KEY
     */
    @ShellMethod(key = "s3 rm", value = "Remove S3 objects")
    public String remove(String path) {
        try {
            path = sessionContext.resolveVariables(path);
            S3Path s3Path = parseS3Path(path);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Path.key)
                    .build();

            s3Client.deleteObject(request);
            return "Object deleted: " + path;
        } catch (Exception e) {
            return "Error deleting object: " + e.getMessage();
        }
    }

    /**
     * Check if a bucket exists
     * <p>
     * Usage:
     * s3 head-bucket s3://bucket-name
     * s3 head-bucket s3://$BUCKET
     */
    @ShellMethod(key = "s3 head-bucket", value = "Check if a bucket exists")
    public String headBucket(String bucketUri) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            S3Path s3Path = parseS3Path(bucketUri);

            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            s3Client.headBucket(request);
            return "Bucket exists: " + s3Path.bucket;
        } catch (Exception e) {
            return "Bucket does not exist or is not accessible: " + e.getMessage();
        }
    }

    /**
     * Get bucket location
     * <p>
     * Usage:
     * s3 get-bucket-location s3://bucket-name
     */
    @ShellMethod(key = "s3 get-bucket-location", value = "Get bucket region")
    public String getBucketLocation(String bucketUri) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            S3Path s3Path = parseS3Path(bucketUri);

            GetBucketLocationRequest request = GetBucketLocationRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            GetBucketLocationResponse response = s3Client.getBucketLocation(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Bucket", s3Path.bucket});
            pairs.add(new String[]{"Region", response.locationConstraintAsString()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get object metadata
     * <p>
     * Usage:
     * s3 head-object s3://bucket/key
     */
    @ShellMethod(key = "s3 head-object", value = "Get object metadata")
    public String headObject(String path) {
        try {
            path = sessionContext.resolveVariables(path);
            S3Path s3Path = parseS3Path(path);

            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Path.key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Bucket", s3Path.bucket});
            pairs.add(new String[]{"Key", s3Path.key});
            pairs.add(new String[]{"Size", formatSize(response.contentLength())});
            pairs.add(new String[]{"Content-Type", response.contentType()});
            pairs.add(new String[]{"Last Modified", response.lastModified().toString()});
            pairs.add(new String[]{"ETag", response.eTag()});
            if (response.storageClassAsString() != null) {
                pairs.add(new String[]{"Storage Class", response.storageClassAsString()});
            }

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get bucket versioning status
     * <p>
     * Usage:
     * s3 get-bucket-versioning s3://bucket-name
     */
    @ShellMethod(key = "s3 get-bucket-versioning", value = "Get bucket versioning status")
    public String getBucketVersioning(String bucketUri) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            S3Path s3Path = parseS3Path(bucketUri);

            GetBucketVersioningRequest request = GetBucketVersioningRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            GetBucketVersioningResponse response = s3Client.getBucketVersioning(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Bucket", s3Path.bucket});
            pairs.add(new String[]{"Status", response.statusAsString() != null ? response.statusAsString() : "Disabled"});
            pairs.add(new String[]{"MFA Delete", response.mfaDeleteAsString() != null ? response.mfaDeleteAsString() : "Disabled"});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Enable bucket versioning
     * <p>
     * Usage:
     * s3 put-bucket-versioning s3://bucket-name --status Enabled
     * s3 put-bucket-versioning s3://bucket-name --status Suspended
     */
    @ShellMethod(key = "s3 put-bucket-versioning", value = "Enable or suspend bucket versioning")
    public String putBucketVersioning(String bucketUri, String status) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            status = sessionContext.resolveVariables(status);
            S3Path s3Path = parseS3Path(bucketUri);

            VersioningConfiguration versioningConfig = VersioningConfiguration.builder()
                    .status(status)
                    .build();

            PutBucketVersioningRequest request = PutBucketVersioningRequest.builder()
                    .bucket(s3Path.bucket)
                    .versioningConfiguration(versioningConfig)
                    .build();

            s3Client.putBucketVersioning(request);
            return "Bucket versioning set to: " + status + " for " + s3Path.bucket;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get object tagging
     * <p>
     * Usage:
     * s3 get-object-tagging s3://bucket/key
     */
    @ShellMethod(key = "s3 get-object-tagging", value = "Get object tags")
    public String getObjectTagging(String path) {
        try {
            path = sessionContext.resolveVariables(path);
            S3Path s3Path = parseS3Path(path);

            GetObjectTaggingRequest request = GetObjectTaggingRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Path.key)
                    .build();

            GetObjectTaggingResponse response = s3Client.getObjectTagging(request);

            if (response.tagSet().isEmpty()) {
                return "No tags found for: " + path;
            }

            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Key", "Value"});

            for (Tag tag : response.tagSet()) {
                rows.add(new String[]{tag.key(), tag.value()});
            }

            return OutputFormatter.toTable(rows);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Put object tagging
     * <p>
     * Usage:
     * s3 put-object-tagging s3://bucket/key --tags "Key1=Value1,Key2=Value2"
     */
    @ShellMethod(key = "s3 put-object-tagging", value = "Set object tags")
    public String putObjectTagging(String path, String tags) {
        try {
            path = sessionContext.resolveVariables(path);
            tags = sessionContext.resolveVariables(tags);
            S3Path s3Path = parseS3Path(path);

            // Parse tags "Key1=Value1,Key2=Value2"
            List<Tag> tagList = new ArrayList<>();
            for (String tagPair : tags.split(",")) {
                String[] parts = tagPair.trim().split("=", 2);
                if (parts.length == 2) {
                    tagList.add(Tag.builder().key(parts[0].trim()).value(parts[1].trim()).build());
                }
            }

            Tagging tagging = Tagging.builder().tagSet(tagList).build();

            PutObjectTaggingRequest request = PutObjectTaggingRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Path.key)
                    .tagging(tagging)
                    .build();

            s3Client.putObjectTagging(request);
            return "Tags set for: " + path;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete object tagging
     * <p>
     * Usage:
     * s3 delete-object-tagging s3://bucket/key
     */
    @ShellMethod(key = "s3 delete-object-tagging", value = "Delete object tags")
    public String deleteObjectTagging(String path) {
        try {
            path = sessionContext.resolveVariables(path);
            S3Path s3Path = parseS3Path(path);

            DeleteObjectTaggingRequest request = DeleteObjectTaggingRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Path.key)
                    .build();

            s3Client.deleteObjectTagging(request);
            return "Tags deleted for: " + path;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete multiple objects
     * <p>
     * Usage:
     * s3 delete-objects s3://bucket --keys "key1,key2,key3"
     */
    @ShellMethod(key = "s3 delete-objects", value = "Delete multiple objects")
    public String deleteObjects(String bucketUri, String keys) {
        try {
            bucketUri = sessionContext.resolveVariables(bucketUri);
            keys = sessionContext.resolveVariables(keys);
            S3Path s3Path = parseS3Path(bucketUri);

            // Parse keys
            List<ObjectIdentifier> objectIds = new ArrayList<>();
            for (String key : keys.split(",")) {
                objectIds.add(ObjectIdentifier.builder().key(key.trim()).build());
            }

            Delete delete = Delete.builder().objects(objectIds).build();

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(s3Path.bucket)
                    .delete(delete)
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(request);

            return "Deleted " + response.deleted().size() + " objects from " + s3Path.bucket;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get object URL (without presigning - requires AWS credentials to access)
     * <p>
     * Usage:
     * s3 get-object-url s3://bucket/key
     */
    @ShellMethod(key = "s3 get-object-url", value = "Get object URL")
    public String getObjectUrl(String path) {
        try {
            path = sessionContext.resolveVariables(path);
            S3Path s3Path = parseS3Path(path);

            // Get bucket location
            GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                    .bucket(s3Path.bucket)
                    .build();

            GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);
            String region = locationResponse.locationConstraintAsString();
            if (region == null || region.isEmpty()) {
                region = "us-east-1";
            }

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Path.bucket, region, s3Path.key);

            return "Object URL:\n" + url + "\n\nNote: This URL requires AWS credentials to access. For public access, use bucket policies or ACLs.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Move object (copy and delete)
     * <p>
     * Usage:
     * s3 mv s3://bucket/source s3://bucket/dest
     */
    @ShellMethod(key = "s3 mv", value = "Move S3 object")
    public String moveObject(String source, String destination) {
        try {
            source = sessionContext.resolveVariables(source);
            destination = sessionContext.resolveVariables(destination);

            // Copy the object
            String copyResult = copyS3Object(source, destination);
            if (copyResult.startsWith("Error")) {
                return copyResult;
            }

            // Delete the source
            S3Path sourcePath = parseS3Path(source);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(sourcePath.bucket)
                    .key(sourcePath.key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            return "Moved: " + source + " -> " + destination;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Sync local directory to S3 or vice versa
     * <p>
     * Usage:
     * s3 sync ./local-dir s3://bucket/prefix
     * s3 sync s3://bucket/prefix ./local-dir
     */
    @ShellMethod(key = "s3 sync", value = "Sync files between local and S3")
    public String sync(String source, String destination) {
        try {
            source = sessionContext.resolveVariables(source);
            destination = sessionContext.resolveVariables(destination);

            boolean sourceIsS3 = source.startsWith("s3://");
            boolean destIsS3 = destination.startsWith("s3://");

            if (sourceIsS3 == destIsS3) {
                return "Error: One path must be local and one must be S3";
            }

            if (sourceIsS3) {
                return syncFromS3(source, destination);
            } else {
                return syncToS3(source, destination);
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Helper methods

    private String syncToS3(String localPath, String s3Uri) throws Exception {
        S3Path s3Path = parseS3Path(s3Uri);
        Path localDir = Paths.get(localPath);

        if (!Files.exists(localDir) || !Files.isDirectory(localDir)) {
            return "Error: Local path must be a directory";
        }

        int uploadCount = 0;
        Files.walk(localDir).filter(Files::isRegularFile).forEach(file -> {
            try {
                String relativePath = localDir.relativize(file).toString().replace("\\", "/");
                String s3Key = s3Path.key.isEmpty() ? relativePath : s3Path.key + "/" + relativePath;

                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(s3Path.bucket)
                        .key(s3Key)
                        .build();

                s3Client.putObject(request, file);
            } catch (Exception e) {
                // Continue with other files
            }
        });

        return "Synced directory to S3: " + s3Uri;
    }

    private String syncFromS3(String s3Uri, String localPath) throws Exception {
        S3Path s3Path = parseS3Path(s3Uri);
        Path localDir = Paths.get(localPath);

        Files.createDirectories(localDir);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Path.bucket)
                .prefix(s3Path.prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        for (S3Object s3Object : listResponse.contents()) {
            String relativePath = s3Object.key();
            if (!s3Path.prefix.isEmpty()) {
                relativePath = s3Object.key().substring(s3Path.prefix.length());
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
            }

            Path targetFile = localDir.resolve(relativePath);
            Files.createDirectories(targetFile.getParent());

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Path.bucket)
                    .key(s3Object.key())
                    .build();

            s3Client.getObject(getRequest, targetFile);
        }

        return "Synced from S3 to local: " + localPath;
    }

    private String listBuckets() {
        ListBucketsResponse response = s3Client.listBuckets();

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Name", "Creation Date"});

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Bucket bucket : response.buckets()) {
            rows.add(new String[]{
                    bucket.name(),
                    bucket.creationDate().atZone(ZoneId.systemDefault()).format(formatter)
            });
        }

        return OutputFormatter.toTable(rows);
    }

    private String listObjects(String bucket, String prefix) {
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucket);

        if (prefix != null && !prefix.isEmpty()) {
            requestBuilder.prefix(prefix);
        }

        ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Key", "Size", "Last Modified"});

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (S3Object object : response.contents()) {
            rows.add(new String[]{
                    object.key(),
                    formatSize(object.size()),
                    object.lastModified().atZone(ZoneId.systemDefault()).format(formatter)
            });
        }

        if (rows.size() == 1) {
            return "No objects found";
        }

        return OutputFormatter.toTable(rows);
    }

    private String uploadFile(String localPath, String s3Uri) throws Exception {
        S3Path s3Path = parseS3Path(s3Uri);
        Path filePath = Paths.get(localPath);

        if (!Files.exists(filePath)) {
            return "Error: Local file not found: " + localPath;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Path.bucket)
                .key(s3Path.key)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(filePath));

        return "Uploaded: " + localPath + " -> " + s3Uri;
    }

    private String downloadFile(String s3Uri, String localPath) throws Exception {
        S3Path s3Path = parseS3Path(s3Uri);
        Path filePath = Paths.get(localPath);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Path.bucket)
                .key(s3Path.key)
                .build();

        s3Client.getObject(request, filePath);

        return "Downloaded: " + s3Uri + " -> " + localPath;
    }

    private String copyS3Object(String sourceUri, String destUri) {
        S3Path source = parseS3Path(sourceUri);
        S3Path dest = parseS3Path(destUri);

        String copySource = source.bucket + "/" + source.key;

        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(source.bucket)
                .sourceKey(source.key)
                .destinationBucket(dest.bucket)
                .destinationKey(dest.key)
                .build();

        s3Client.copyObject(request);

        return "Copied: " + sourceUri + " -> " + destUri;
    }

    private S3Path parseS3Path(String path) {
        if (!path.startsWith("s3://")) {
            throw new IllegalArgumentException("S3 path must start with s3://");
        }

        String withoutProtocol = path.substring(5);
        int firstSlash = withoutProtocol.indexOf('/');

        S3Path result = new S3Path();

        if (firstSlash == -1) {
            result.bucket = withoutProtocol;
            result.key = "";
            result.prefix = "";
        } else {
            result.bucket = withoutProtocol.substring(0, firstSlash);
            result.key = withoutProtocol.substring(firstSlash + 1);
            result.prefix = result.key;
        }

        return result;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "iB";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private static class S3Path {
        String bucket;
        String key;
        String prefix;
    }
}
